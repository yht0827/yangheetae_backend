package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.entity.TransactionType;
import com.example.transfer.domain.repository.TransactionEntryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("계좌 거래내역 서비스")
class AccountTransactionEntryServiceTest {

	@Mock
	private TransactionEntryRepository transactionEntryRepository;

	@InjectMocks
	private AccountTransactionEntryService accountTransactionEntryService;

	@BeforeEach
	void setUpMocks() {
		when(transactionEntryRepository.save(any(AccountTransactionEntry.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("입금 거래내역을 저장한다")
	void saveDeposit_persistsDepositEntry() {
		// when
		AccountTransactionEntry entry = accountTransactionEntryService.saveDeposit(1L, 50_000L);

		// then
		assertThat(entry.getType()).isEqualTo(TransactionType.DEPOSIT);
		assertThat(entry.getAccountId()).isEqualTo(1L);
		assertThat(entry.getAmountWon()).isEqualTo(50_000L);
		verify(transactionEntryRepository).save(entry);
	}

	@Test
	@DisplayName("출금 거래내역을 저장한다")
	void saveWithdrawal_persistsWithdrawalEntry() {
		// when
		AccountTransactionEntry entry = accountTransactionEntryService.saveWithdrawal(2L, 30_000L);

		// then
		assertThat(entry.getType()).isEqualTo(TransactionType.WITHDRAWAL);
		assertThat(entry.getAccountId()).isEqualTo(2L);
		assertThat(entry.getAmountWon()).isEqualTo(30_000L);
		verify(transactionEntryRepository).save(entry);
	}

	@Test
	@DisplayName("송금 출금 거래내역을 저장한다")
	void saveTransferOut_persistsTransferOutEntry() {
		// given
		Long fromId = 3L;
		Long toId = 4L;
		Long amount = 70_000L;
		UUID transferId = UUID.randomUUID();

		// when
		AccountTransactionEntry entry = accountTransactionEntryService.saveTransferOut(fromId, amount, toId,
			transferId);

		// then
		assertThat(entry.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
		assertThat(entry.getAccountId()).isEqualTo(fromId);
		assertThat(entry.getCounterpartyAccountId()).isEqualTo(toId);
		assertThat(entry.getRelatedTransferId()).isEqualTo(transferId);
		assertThat(entry.getAmountWon()).isEqualTo(amount);
		verify(transactionEntryRepository).save(entry);
	}

	@Test
	@DisplayName("송금 입금 거래내역을 저장한다")
	void saveTransferIn_persistsTransferInEntry() {
		// given
		Long fromId = 5L;
		Long toId = 6L;
		Long amount = 90_000L;
		UUID transferId = UUID.randomUUID();

		// when
		AccountTransactionEntry entry = accountTransactionEntryService.saveTransferIn(toId, amount, fromId, transferId);

		// then
		assertThat(entry.getType()).isEqualTo(TransactionType.TRANSFER_IN);
		assertThat(entry.getAccountId()).isEqualTo(toId);
		assertThat(entry.getCounterpartyAccountId()).isEqualTo(fromId);
		assertThat(entry.getRelatedTransferId()).isEqualTo(transferId);
		assertThat(entry.getAmountWon()).isEqualTo(amount);
		verify(transactionEntryRepository).save(entry);
	}

	@Test
	@DisplayName("수수료 거래내역을 저장한다")
	void saveFee_persistsFeeEntry() {
		// given
		Long accountId = 7L;
		Long feeAmount = 1_000L;
		UUID transferId = UUID.randomUUID();

		// when
		AccountTransactionEntry entry = accountTransactionEntryService.saveFee(accountId, feeAmount, transferId);

		// then
		assertThat(entry.getType()).isEqualTo(TransactionType.FEE);
		assertThat(entry.getAccountId()).isEqualTo(accountId);
		assertThat(entry.getCounterpartyAccountId()).isNull();
		assertThat(entry.getRelatedTransferId()).isEqualTo(transferId);
		assertThat(entry.getAmountWon()).isEqualTo(feeAmount);
		verify(transactionEntryRepository).save(entry);
	}
}
