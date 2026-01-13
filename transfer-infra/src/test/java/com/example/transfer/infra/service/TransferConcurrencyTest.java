package com.example.transfer.infra.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.example.transfer.domain.service.TransferService;
import com.example.transfer.domain.policy.FeePolicy;
import com.example.transfer.infra.config.IntegrationTestSupport;

class TransferConcurrencyTest extends IntegrationTestSupport {

	@Autowired
	private AccountCommandService accountCommandService;

	@Autowired
	private AccountQueryService accountQueryService;

	@Autowired
	private TransferService transferService;

	@Autowired
	private TransactionEntryRepository transactionEntryRepository;

	@Autowired
	private DailyUsageRepository dailyUsageRepository;

	@Autowired
	private FeePolicy feePolicy;

	@Autowired
	private Clock clock;

	@Test
	@DisplayName("동시 이체에서도 잔액/수수료/거래내역이 중복 없이 일치한다")
	void concurrentTransfers_preserveConsistency() throws InterruptedException {
		Account sender = accountCommandService.createAccount("동시이체-보내는이");
		Account receiver = accountCommandService.createAccount("동시이체-받는이");

		accountCommandService.deposit(sender.getId(), 5_000_000L);

		int workerCount = 8;
		long transferAmount = 500_000L;
		long feePerTransfer = feePolicy.calculate(transferAmount);
		long transferDailyLimit = 3_000_000L;
		int expectedSuccesses = (int)(transferDailyLimit / transferAmount);

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
					transferService.transfer(sender.getId(), receiver.getId(), transferAmount);
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

		Account senderAfter = accountQueryService.getAccount(sender.getId());
		Account receiverAfter = accountQueryService.getAccount(receiver.getId());
		DailyUsage usage = dailyUsageRepository
			.findByAccountIdAndUsageDate(sender.getId(), LocalDate.now(clock))
			.orElseThrow();

		assertThat(successCount.get()).isEqualTo(expectedSuccesses);
		assertThat(rejectedCount.get()).isEqualTo(workerCount - expectedSuccesses);

		long totalFee = feePerTransfer * successCount.get();
		assertThat(senderAfter.getBalanceWon())
			.isEqualTo(5_000_000L - successCount.get() * (transferAmount + feePerTransfer));
		assertThat(receiverAfter.getBalanceWon()).isEqualTo(successCount.get() * transferAmount);
		assertThat(usage.getTransferTotalWon()).isEqualTo(successCount.get() * transferAmount);

		List<AccountTransactionEntry> senderEntries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(sender.getId());
		List<AccountTransactionEntry> receiverEntries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(receiver.getId());

		long transferOutCount = senderEntries.stream()
			.filter(entry -> entry.getType() == TransactionType.TRANSFER_OUT)
			.count();
		long feeCount = senderEntries.stream()
			.filter(entry -> entry.getType() == TransactionType.FEE)
			.count();
		long transferInCount = receiverEntries.stream()
			.filter(entry -> entry.getType() == TransactionType.TRANSFER_IN)
			.count();
		long receiverFeeCount = receiverEntries.stream()
			.filter(entry -> entry.getType() == TransactionType.FEE)
			.count();

		assertThat(transferOutCount).isEqualTo(successCount.get());
		assertThat(feeCount).isEqualTo(successCount.get());
		assertThat(transferInCount).isEqualTo(successCount.get());
		assertThat(receiverFeeCount).isZero();

		assertThat(senderAfter.getBalanceWon() + receiverAfter.getBalanceWon() + totalFee)
			.isEqualTo(5_000_000L);
	}

	@Test
	@DisplayName("A에서 B로 100명이 동시에 1만원씩 송금하면 잔액과 거래 내역이 정확히 반영된다")
	void hundredConcurrentTransfers_balanceAndEntriesAreConsistent() throws InterruptedException {
		// given
		// 1만원 송금 시 수수료 = floor(10,000 * 0.01) = 100원
		// 100건 송금 시 총 차감 = (10,000 + 100) × 100 = 1,010,000원
		long transferAmount = 10_000L;
		long feePerTransfer = feePolicy.calculate(transferAmount);
		int workerCount = 100;
		long totalDeduction = (transferAmount + feePerTransfer) * workerCount;

		Account sender = accountCommandService.createAccount("100명송금-보내는이");
		Account receiver = accountCommandService.createAccount("100명송금-받는이");
		accountCommandService.deposit(sender.getId(), totalDeduction); // 정확히 필요한 금액
		accountCommandService.deposit(receiver.getId(), 1_000_000L);   // 초기 잔액 100만원

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failedCount = new AtomicInteger();

		// when - 100명이 동시에 1만원씩 송금
		for (int i = 0; i < workerCount; i++) {
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					transferService.transfer(sender.getId(), receiver.getId(), transferAmount);
					successCount.incrementAndGet();
				} catch (DailyLimitExceededException | InsufficientBalanceException e) {
					failedCount.incrementAndGet();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();
		done.await(30, TimeUnit.SECONDS);
		executor.shutdownNow();

		// then
		Account senderAfter = accountQueryService.getAccount(sender.getId());
		Account receiverAfter = accountQueryService.getAccount(receiver.getId());

		// 일일 이체 한도 300만원, 100건 × 1만원 = 100만원 < 한도 내이므로 모두 성공해야 함
		assertThat(successCount.get()).isEqualTo(workerCount);
		assertThat(failedCount.get()).isZero();

		// A는 0원 (정확히 필요한 금액만 입금했으므로)
		assertThat(senderAfter.getBalanceWon()).isZero();

		// B는 200만원 (초기 100만원 + 받은 100만원)
		assertThat(receiverAfter.getBalanceWon()).isEqualTo(1_000_000L + transferAmount * workerCount);

		// 거래 내역 검증 - 중복 생성 없음
		List<AccountTransactionEntry> senderEntries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(sender.getId());
		List<AccountTransactionEntry> receiverEntries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(receiver.getId());

		long transferOutCount = senderEntries.stream()
			.filter(e -> e.getType() == TransactionType.TRANSFER_OUT)
			.count();
		long feeCount = senderEntries.stream()
			.filter(e -> e.getType() == TransactionType.FEE)
			.count();
		long transferInCount = receiverEntries.stream()
			.filter(e -> e.getType() == TransactionType.TRANSFER_IN)
			.count();

		// 거래 내역 수 = 송금 성공 횟수
		assertThat(transferOutCount).isEqualTo(workerCount);
		assertThat(feeCount).isEqualTo(workerCount);
		assertThat(transferInCount).isEqualTo(workerCount);

		// 총 금액 보존 확인 (A + B + 수수료 = 초기 총액)
		long totalFee = feePerTransfer * workerCount;
		assertThat(senderAfter.getBalanceWon() + receiverAfter.getBalanceWon() + totalFee)
			.isEqualTo(totalDeduction + 1_000_000L);
	}
}
