package com.example.transfer.infra.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.exception.DailyLimitExceededException;
import com.example.transfer.domain.exception.InsufficientBalanceException;
import com.example.transfer.domain.repository.DailyUsageRepository;
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
}
