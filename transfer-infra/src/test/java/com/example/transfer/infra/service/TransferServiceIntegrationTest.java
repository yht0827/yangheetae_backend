package com.example.transfer.infra.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.entity.TransactionType;
import com.example.transfer.domain.exception.AccountNotFoundException;
import com.example.transfer.domain.repository.DailyUsageRepository;
import com.example.transfer.domain.repository.TransactionEntryRepository;
import com.example.transfer.domain.service.AccountCommandService;
import com.example.transfer.domain.service.AccountQueryService;
import com.example.transfer.domain.service.TransferService;
import com.example.transfer.infra.config.IntegrationTestSupport;

class TransferServiceIntegrationTest extends IntegrationTestSupport {

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
	private Clock clock;

	@Test
	@DisplayName("이체는 잔액과 거래내역을 원자적으로 반영한다")
	void transfer_updatesBalancesAndEntriesAtomically() {
		Account sender = accountCommandService.createAccount("보내는 사람");
		Account receiver = accountCommandService.createAccount("받는 사람");

		accountCommandService.deposit(sender.getId(), 500_000L);
		accountCommandService.deposit(receiver.getId(), 100_000L);

		long originalTotal = accountQueryService.getAccount(sender.getId()).getBalanceWon()
			+ accountQueryService.getAccount(receiver.getId()).getBalanceWon();

		TransferResult result = transferService.transfer(sender.getId(), receiver.getId(), 200_000L);

		Account updatedSender = accountQueryService.getAccount(sender.getId());
		Account updatedReceiver = accountQueryService.getAccount(receiver.getId());

		assertThat(updatedSender.getBalanceWon() + updatedReceiver.getBalanceWon() + result.fee())
			.isEqualTo(originalTotal);

		List<AccountTransactionEntry> senderEntries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(sender.getId());
		List<AccountTransactionEntry> receiverEntries = transactionEntryRepository
			.findByAccountIdOrderByOccurredAtDesc(receiver.getId());

		List<AccountTransactionEntry> senderTransferEntries = senderEntries.stream()
			.filter(entry -> result.transferId().equals(entry.getRelatedTransferId()))
			.toList();
		assertThat(senderTransferEntries).hasSize(2);
		assertThat(senderTransferEntries)
			.extracting(AccountTransactionEntry::getType)
			.containsExactlyInAnyOrder(TransactionType.TRANSFER_OUT, TransactionType.FEE);

		List<AccountTransactionEntry> receiverTransferEntries = receiverEntries.stream()
			.filter(entry -> result.transferId().equals(entry.getRelatedTransferId()))
			.toList();
		assertThat(receiverTransferEntries).hasSize(1);
		assertThat(receiverTransferEntries.get(0).getType()).isEqualTo(TransactionType.TRANSFER_IN);

		DailyUsage usage = dailyUsageRepository
			.findByAccountIdAndUsageDate(sender.getId(), LocalDate.now(clock))
			.orElseThrow();
		assertThat(usage.getTransferTotalWon()).isEqualTo(200_000L);
	}

	@Test
	@DisplayName("삭제된 계좌는 거래가 차단되지만 거래내역은 조회된다")
	void softDelete_blocksCommandsButKeepsHistory() {
		Account account = accountCommandService.createAccount("삭제 대상");
		accountCommandService.deposit(account.getId(), 50_000L);

		accountCommandService.deleteAccount(account.getId());

		List<AccountTransactionEntry> history = accountQueryService.getTransactions(account.getId());
		assertThat(history).isNotEmpty();

		assertThatThrownBy(() -> accountCommandService.deposit(account.getId(), 10_000L))
			.isInstanceOf(AccountNotFoundException.class);
	}
}
