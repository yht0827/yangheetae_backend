package com.example.transfer.infra.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.entity.TransactionType;
import com.example.transfer.domain.exception.DailyLimitExceededException;
import com.example.transfer.domain.exception.InsufficientBalanceException;
import com.example.transfer.domain.repository.DailyUsageRepository;
import com.example.transfer.domain.repository.TransactionEntryRepository;
import com.example.transfer.domain.service.AccountCommandService;
import com.example.transfer.domain.service.AccountQueryService;
import com.example.transfer.infra.config.IntegrationTestSupport;

class AccountCommandConcurrencyTest extends IntegrationTestSupport {

	@Autowired
	private AccountCommandService accountCommandService;

	@Autowired
	private AccountQueryService accountQueryService;

	@Autowired
	private DailyUsageRepository dailyUsageRepository;

	@Autowired
	private TransactionEntryRepository transactionEntryRepository;

	@Autowired
	private Clock clock;

	@Test
	@DisplayName("동시 출금 시 일일 한도와 잔액이 정확히 반영된다")
	void concurrentWithdrawals_respectLimitAndBalance() throws InterruptedException {
		Account account = accountCommandService.createAccount("동시출금");
		accountCommandService.deposit(account.getId(), 2_000_000L);

		int workerCount = 12;
		long withdrawAmount = 100_000L;
		long expectedDailyLimit = 1_000_000L; // WithdrawalLimitPolicy 기준
		int expectedSuccesses = (int)(expectedDailyLimit / withdrawAmount);

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger rejectedCount = new AtomicInteger();

		for (int i = 0; i < workerCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					accountCommandService.withdraw(account.getId(), withdrawAmount);
					successCount.incrementAndGet();
				} catch (DailyLimitExceededException | InsufficientBalanceException e) {
					rejectedCount.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		Account updated = accountQueryService.getAccount(account.getId());
		DailyUsage usage = dailyUsageRepository
			.findByAccountIdAndUsageDate(account.getId(), LocalDate.now(clock))
			.orElseThrow();

		assertThat(successCount.get()).isEqualTo(expectedSuccesses);
		assertThat(rejectedCount.get()).isEqualTo(workerCount - expectedSuccesses);
		assertThat(updated.getBalanceWon()).isEqualTo(2_000_000L - withdrawAmount * successCount.get());
		assertThat(usage.getWithdrawalTotalWon()).isEqualTo(successCount.get() * withdrawAmount);
	}

	@Test
	@DisplayName("동시 입금 요청 시 잔액과 거래 내역이 정확히 반영되고 중복 생성되지 않는다")
	void concurrentDeposits_balanceAndEntriesAreConsistent() throws InterruptedException {
		// given
		Account account = accountCommandService.createAccount("동시입금테스트");
		long depositAmount = 100_000L;
		int workerCount = 10;

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);
		AtomicInteger successCount = new AtomicInteger();

		// when
		for (int i = 0; i < workerCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					accountCommandService.deposit(account.getId(), depositAmount);
					successCount.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		// then
		Account updated = accountQueryService.getAccount(account.getId());
		List<AccountTransactionEntry> entries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(account.getId());
		long depositEntryCount = entries.stream()
			.filter(e -> e.getType() == TransactionType.DEPOSIT)
			.count();

		// 모든 입금이 성공해야 함
		assertThat(successCount.get()).isEqualTo(workerCount);

		// 잔액 = 입금 횟수 × 입금액
		assertThat(updated.getBalanceWon()).isEqualTo(depositAmount * workerCount);

		// 거래 내역 수 = 입금 횟수 (중복 생성 없음)
		assertThat(depositEntryCount).isEqualTo(workerCount);
	}

	@Test
	@DisplayName("동시 출금 요청 시 잔액과 거래 내역이 정확히 반영되고 중복 생성되지 않는다")
	void concurrentWithdrawals_balanceAndEntriesAreConsistent() throws InterruptedException {
		// given
		Account account = accountCommandService.createAccount("동시출금테스트");
		accountCommandService.deposit(account.getId(), 10_000_000L); // 충분한 잔액
		long withdrawAmount = 50_000L; // 일일 한도(100만원) 내에서 처리되도록
		int workerCount = 10; // 50,000 × 10 = 500,000 (한도 내)

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);
		AtomicInteger successCount = new AtomicInteger();

		// when
		for (int i = 0; i < workerCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					accountCommandService.withdraw(account.getId(), withdrawAmount);
					successCount.incrementAndGet();
				} catch (DailyLimitExceededException | InsufficientBalanceException e) {
					// 한도 초과나 잔액 부족은 정상 시나리오
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		// then
		Account updated = accountQueryService.getAccount(account.getId());
		DailyUsage usage = dailyUsageRepository
			.findByAccountIdAndUsageDate(account.getId(), LocalDate.now(clock))
			.orElseThrow();
		List<AccountTransactionEntry> entries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(account.getId());

		long withdrawalEntryCount = entries.stream()
			.filter(e -> e.getType() == TransactionType.WITHDRAWAL)
			.count();

		// 모든 출금이 성공해야 함 (한도 내)
		assertThat(successCount.get()).isEqualTo(workerCount);

		// 잔액 = 초기 잔액 - (출금 횟수 × 출금액)
		assertThat(updated.getBalanceWon()).isEqualTo(10_000_000L - withdrawAmount * successCount.get());

		// 일일 출금 합계 = 출금 횟수 × 출금액
		assertThat(usage.getWithdrawalTotalWon()).isEqualTo(withdrawAmount * successCount.get());

		// 거래 내역 수 = 출금 횟수 (중복 생성 없음) + 1 (초기 입금)
		assertThat(withdrawalEntryCount).isEqualTo(successCount.get());
	}

	@Test
	@DisplayName("동시 입출금 혼합 요청 시 잔액 무결성이 유지된다")
	void concurrentDepositAndWithdraw_balanceIntegrityMaintained() throws InterruptedException {
		// given
		Account account = accountCommandService.createAccount("입출금혼합");
		accountCommandService.deposit(account.getId(), 5_000_000L);
		long amount = 100_000L;
		int depositCount = 5;
		int withdrawCount = 5;
		int workerCount = depositCount + withdrawCount;

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);
		AtomicInteger depositSuccessCount = new AtomicInteger();
		AtomicInteger withdrawSuccessCount = new AtomicInteger();

		// when - 입금 스레드
		for (int i = 0; i < depositCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					accountCommandService.deposit(account.getId(), amount);
					depositSuccessCount.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		// 출금 스레드
		for (int i = 0; i < withdrawCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					accountCommandService.withdraw(account.getId(), amount);
					withdrawSuccessCount.incrementAndGet();
				} catch (DailyLimitExceededException | InsufficientBalanceException e) {
					// 정상 시나리오
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		// then
		Account updated = accountQueryService.getAccount(account.getId());
		List<AccountTransactionEntry> entries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(account.getId());

		long depositEntryCount = entries.stream()
			.filter(e -> e.getType() == TransactionType.DEPOSIT)
			.count();
		long withdrawalEntryCount = entries.stream()
			.filter(e -> e.getType() == TransactionType.WITHDRAWAL)
			.count();

		// 잔액 = 초기 잔액 + (입금 성공 횟수 × 금액) - (출금 성공 횟수 × 금액)
		long expectedBalance = 5_000_000L
			+ (depositSuccessCount.get() * amount)
			- (withdrawSuccessCount.get() * amount);
		assertThat(updated.getBalanceWon()).isEqualTo(expectedBalance);

		// 거래 내역 수 일치 (입금 내역 = 초기 입금 1건 + 동시 입금 성공 건수)
		assertThat(depositEntryCount).isEqualTo(1 + depositSuccessCount.get());
		assertThat(withdrawalEntryCount).isEqualTo(withdrawSuccessCount.get());
	}
}
