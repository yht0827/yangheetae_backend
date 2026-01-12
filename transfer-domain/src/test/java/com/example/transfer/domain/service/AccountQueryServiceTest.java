package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.exception.AccountNotFoundException;
import com.example.transfer.domain.repository.AccountRepository;
import com.example.transfer.domain.repository.TransactionEntryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("계좌 조회 서비스")
class AccountQueryServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionEntryRepository transactionEntryRepository;

	@InjectMocks
	private AccountQueryService accountQueryService;

	@Test
	@DisplayName("계좌 ID로 활성 계좌를 조회한다")
	void getAccount_returnsActiveAccount() {
		// given
		Account account = Account.create("김개발");
		when(accountRepository.findActiveById(1L)).thenReturn(Optional.of(account));

		// when
		Account result = accountQueryService.getAccount(1L);

		// then
		assertThat(result).isEqualTo(account);
		assertThat(result.getOwnerName()).isEqualTo("김개발");
	}

	@Test
	@DisplayName("계좌 조회 시 없으면 예외를 던진다")
	void getAccount_throwsWhenNotFound() {
		// given
		when(accountRepository.findActiveById(1L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> accountQueryService.getAccount(1L))
			.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@DisplayName("거래내역 조회 시 모든 기록을 반환한다")
	void getTransactions_returnsTransactionList() {
		// given
		Account account = Account.create("이개발");
		List<AccountTransactionEntry> entries = List.of(
			AccountTransactionEntry.createDeposit(1L, 50_000L),
			AccountTransactionEntry.createWithdrawal(1L, 20_000L)
		);
		when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
		when(transactionEntryRepository.findByAccountIdOrderByOccurredAtDesc(1L)).thenReturn(entries);

		// when
		List<AccountTransactionEntry> result = accountQueryService.getTransactions(1L);

		// then
		assertThat(result).hasSize(2);
		verify(transactionEntryRepository).findByAccountIdOrderByOccurredAtDesc(1L);
	}

	@Test
	@DisplayName("삭제된 계좌도 거래내역 조회를 허용한다")
	void getTransactions_allowsDeletedAccount() {
		// given
		Account account = Account.create("박개발");
		account.delete();
		when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
		when(transactionEntryRepository.findByAccountIdOrderByOccurredAtDesc(1L)).thenReturn(List.of());

		// when
		List<AccountTransactionEntry> result = accountQueryService.getTransactions(1L);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("거래내역 조회 시 계좌가 없으면 예외가 발생한다")
	void getTransactions_throwsWhenAccountNotFound() {
		// given
		when(accountRepository.findById(1L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> accountQueryService.getTransactions(1L))
			.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@DisplayName("활성 계좌 조회 시 계좌를 반환한다")
	void findActiveAccount_returnsActiveAccount() {
		// given
		Account account = Account.create("강개발");
		when(accountRepository.findActiveById(1L)).thenReturn(Optional.of(account));

		// when
		Account result = accountQueryService.findActiveAccount(1L);

		// then
		assertThat(result).isEqualTo(account);
	}

	@Test
	@DisplayName("활성 계좌가 없으면 예외가 발생한다")
	void findActiveAccount_throwsWhenNotFound() {
		// given
		when(accountRepository.findActiveById(1L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> accountQueryService.findActiveAccount(1L))
			.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@DisplayName("락을 걸고 활성 계좌를 조회한다")
	void findActiveAccountWithLock_returnsActiveAccount() {
		// given
		Account account = Account.create("김개발");
		when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));

		// when
		Account result = accountQueryService.findActiveAccountWithLock(1L);

		// then
		assertThat(result).isEqualTo(account);
		assertThat(result.isActive()).isTrue();
	}

	@Test
	@DisplayName("락 조회 시 계좌가 없으면 예외가 발생한다")
	void findActiveAccountWithLock_throwsWhenNotFound() {
		// given
		when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> accountQueryService.findActiveAccountWithLock(1L))
			.isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	@DisplayName("락 조회 시 삭제된 계좌면 예외가 발생한다")
	void findActiveAccountWithLock_throwsWhenDeleted() {
		// given
		Account account = Account.create("황개발");
		account.delete();
		when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));

		// when & then
		assertThatThrownBy(() -> accountQueryService.findActiveAccountWithLock(1L))
			.isInstanceOf(AccountNotFoundException.class);
	}
}
