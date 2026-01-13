package com.example.transfer.infra.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.exception.InsufficientBalanceException;
import com.example.transfer.domain.service.AccountCommandService;
import com.example.transfer.domain.service.AccountQueryService;
import com.example.transfer.domain.service.TransferService;
import com.example.transfer.infra.config.IntegrationTestSupport;

@DisplayName("데드락 테스트")
class TransferDeadlockTest extends IntegrationTestSupport {

	@Autowired
	private AccountCommandService accountCommandService;

	@Autowired
	private AccountQueryService accountQueryService;

	@Autowired
	private TransferService transferService;

	@Test
	@DisplayName("A->B와 B->A 동시 이체 시 데드락 없이 처리된다")
	void bidirectionalTransfer_noDeadlock() throws InterruptedException {
		// given
		Account accountA = accountCommandService.createAccount("데드락A");
		Account accountB = accountCommandService.createAccount("데드락B");
		accountCommandService.deposit(accountA.getId(), 1_000_000L);
		accountCommandService.deposit(accountB.getId(), 1_000_000L);

		long transferAmount = 100_000L;

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);

		AtomicInteger successCount = new AtomicInteger();
		AtomicReference<Exception> exceptionRef = new AtomicReference<>();

		// when - A->B 이체
		executor.submit(() -> {
			ready.countDown();
			try {
				start.await();
				transferService.transfer(accountA.getId(), accountB.getId(), transferAmount);
				successCount.incrementAndGet();
			} catch (InsufficientBalanceException e) {
				// 잔액 부족은 정상 시나리오
			} catch (Exception e) {
				exceptionRef.compareAndSet(null, e);
			} finally {
				done.countDown();
			}
		});

		// B->A 이체
		executor.submit(() -> {
			ready.countDown();
			try {
				start.await();
				transferService.transfer(accountB.getId(), accountA.getId(), transferAmount);
				successCount.incrementAndGet();
			} catch (InsufficientBalanceException e) {
				// 잔액 부족은 정상 시나리오
			} catch (Exception e) {
				exceptionRef.compareAndSet(null, e);
			} finally {
				done.countDown();
			}
		});

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();

		// then - 10초 내에 완료되어야 함 (데드락 발생 시 타임아웃)
		boolean completed = done.await(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		assertThat(completed)
			.as("데드락으로 인한 타임아웃이 발생하지 않아야 한다")
			.isTrue();
		assertThat(exceptionRef.get())
			.as("예상치 못한 예외가 발생하지 않아야 한다")
			.isNull();

		// 잔액 합계 검증 (수수료 차감 후에도 보존)
		Account afterA = accountQueryService.getAccount(accountA.getId());
		Account afterB = accountQueryService.getAccount(accountB.getId());

		// 총 금액 = 초기 잔액 - (성공 횟수 * 수수료)
		long expectedFee = (long)Math.floor(transferAmount * 0.01);
		long totalFee = successCount.get() * expectedFee;
		assertThat(afterA.getBalanceWon() + afterB.getBalanceWon())
			.isEqualTo(2_000_000L - totalFee);
	}

	@RepeatedTest(5)
	@DisplayName("여러 계좌 간 교차 이체 시 데드락 없이 처리된다")
	void multipleAccountsCrossTransfer_noDeadlock() throws InterruptedException {
		// given - 4개 계좌 준비
		Account[] accounts = new Account[4];
		for (int i = 0; i < 4; i++) {
			accounts[i] = accountCommandService.createAccount("교차이체" + i);
			accountCommandService.deposit(accounts[i].getId(), 1_000_000L);
		}

		long transferAmount = 50_000L;
		int transferCount = 8;

		ExecutorService executor = Executors.newFixedThreadPool(transferCount);
		CountDownLatch ready = new CountDownLatch(transferCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(transferCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicReference<Exception> exceptionRef = new AtomicReference<>();

		// when - 교차 이체 (0->1, 1->0, 0->2, 2->0, 1->2, 2->1, 1->3, 3->1)
		int[][] transfers = {
			{0, 1}, {1, 0}, {0, 2}, {2, 0},
			{1, 2}, {2, 1}, {1, 3}, {3, 1}
		};

		for (int[] transfer : transfers) {
			final int from = transfer[0];
			final int to = transfer[1];
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					transferService.transfer(accounts[from].getId(), accounts[to].getId(), transferAmount);
					successCount.incrementAndGet();
				} catch (InsufficientBalanceException e) {
					// 잔액 부족은 정상 시나리오
				} catch (Exception e) {
					exceptionRef.compareAndSet(null, e);
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();

		// then
		boolean completed = done.await(15, TimeUnit.SECONDS);
		executor.shutdownNow();

		assertThat(completed)
			.as("데드락으로 인한 타임아웃이 발생하지 않아야 한다")
			.isTrue();
		assertThat(exceptionRef.get())
			.as("예상치 못한 예외가 발생하지 않아야 한다")
			.isNull();

		// 전체 잔액 합계 검증
		long totalBalance = 0;
		for (int i = 0; i < 4; i++) {
			Account account = accountQueryService.getAccount(accounts[i].getId());
			totalBalance += account.getBalanceWon();
		}

		long expectedFee = (long)Math.floor(transferAmount * 0.01);
		long totalFee = successCount.get() * expectedFee;
		assertThat(totalBalance).isEqualTo(4_000_000L - totalFee);
	}

	@Test
	@DisplayName("대량 동시 양방향 이체 시 데드락 없이 처리된다")
	void massiveBidirectionalTransfers_noDeadlock() throws InterruptedException {
		// given
		Account accountA = accountCommandService.createAccount("대량A");
		Account accountB = accountCommandService.createAccount("대량B");
		accountCommandService.deposit(accountA.getId(), 10_000_000L);
		accountCommandService.deposit(accountB.getId(), 10_000_000L);

		long transferAmount = 10_000L;
		int workerCount = 20;

		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		CountDownLatch ready = new CountDownLatch(workerCount);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(workerCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicReference<Exception> exceptionRef = new AtomicReference<>();

		// when - 절반은 A->B, 절반은 B->A
		for (int i = 0; i < workerCount; i++) {
			final boolean aToB = (i % 2 == 0);
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					if (aToB) {
						transferService.transfer(accountA.getId(), accountB.getId(), transferAmount);
					} else {
						transferService.transfer(accountB.getId(), accountA.getId(), transferAmount);
					}
					successCount.incrementAndGet();
				} catch (InsufficientBalanceException e) {
					// 정상
				} catch (Exception e) {
					exceptionRef.compareAndSet(null, e);
				} finally {
					done.countDown();
				}
			});
		}

		ready.await(5, TimeUnit.SECONDS);
		start.countDown();

		// then
		boolean completed = done.await(30, TimeUnit.SECONDS);
		executor.shutdownNow();

		assertThat(completed)
			.as("데드락으로 인한 타임아웃이 발생하지 않아야 한다")
			.isTrue();
		assertThat(exceptionRef.get())
			.as("예상치 못한 예외가 발생하지 않아야 한다")
			.isNull();

		// 잔액 무결성 검증
		Account afterA = accountQueryService.getAccount(accountA.getId());
		Account afterB = accountQueryService.getAccount(accountB.getId());

		long expectedFee = (long)Math.floor(transferAmount * 0.01);
		long totalFee = successCount.get() * expectedFee;
		assertThat(afterA.getBalanceWon() + afterB.getBalanceWon())
			.isEqualTo(20_000_000L - totalFee);
	}
}
