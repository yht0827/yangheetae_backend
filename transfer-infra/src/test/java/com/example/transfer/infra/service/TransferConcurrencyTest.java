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
}
