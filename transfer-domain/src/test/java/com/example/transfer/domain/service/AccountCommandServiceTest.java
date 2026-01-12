package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.event.AccountBalanceChangedEvent;
import com.example.transfer.domain.event.AccountBalanceChangedEvent.ChangeType;
import com.example.transfer.domain.exception.DailyLimitExceededException;
import com.example.transfer.domain.exception.InsufficientBalanceException;
import com.example.transfer.domain.policy.BalancePolicy;
import com.example.transfer.domain.policy.WithdrawalLimitPolicy;
import com.example.transfer.domain.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("계좌 명령 서비스")
class AccountCommandServiceTest {

	@Mock
	private AccountQueryService accountQueryService;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private DailyUsageService dailyUsageService;

	@Mock
	private AccountTransactionEntryService accountTransactionEntryService;

	@Mock
	private BalancePolicy balancePolicy;

	@Mock
	private WithdrawalLimitPolicy withdrawalLimitPolicy;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private Clock clock;

	@InjectMocks
	private AccountCommandService accountCommandService;

	private static final Instant FIXED_INSTANT = Instant.parse("2025-01-12T10:00:00Z");
	private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");

	@BeforeEach
	void setUpClock() {
		when(clock.instant()).thenReturn(FIXED_INSTANT);
		when(clock.getZone()).thenReturn(ASIA_SEOUL);
	}

	@Test
	@DisplayName("계좌 생성 시 새 계좌를 저장한다")
	void createAccount_savesNewAccount() {
		// given
		Account created = Account.create("김개발");
		when(accountRepository.save(any(Account.class))).thenReturn(created);

		// when
		Account result = accountCommandService.createAccount("김개발");

		// then
		assertThat(result).isEqualTo(created);
		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository).save(captor.capture());
		assertThat(captor.getValue().getOwnerName()).isEqualTo("김개발");
	}

	@Test
	@DisplayName("입금하면 잔액이 증가하고 입금 내역이 저장된다")
	void deposit_increasesBalanceAndRecordsEntry() {
		// given
		Account account = stubLockedAccount(1L, "안개발", 0L);

		// when
		accountCommandService.deposit(1L, 50_000L);

		// then
		assertThat(account.getBalanceWon()).isEqualTo(50_000L);
		verify(accountTransactionEntryService).saveDeposit(1L, 50_000L);
	}

	@Test
	@DisplayName("출금 시 정책 검증 후 거래 내역을 기록한다")
	void withdraw_usesPoliciesAndRecordsEntry() {
		// given
		Account account = stubLockedAccount(1L, "전개발", 100_000L);
		Long startingBalance = account.getBalanceWon();
		DailyUsage usage = stubDailyUsage(1L);

		// when
		accountCommandService.withdraw(1L, 30_000L);

		// then
		verify(withdrawalLimitPolicy).validate(0L, 30_000L);
		verify(balancePolicy).validate(startingBalance, 30_000L);
		assertThat(account.getBalanceWon()).isEqualTo(70_000L);
		assertThat(usage.getWithdrawalTotalWon()).isEqualTo(30_000L);
		verify(accountTransactionEntryService).saveWithdrawal(1L, 30_000L);
	}

	@Test
	@DisplayName("입금 시 잔액 변경 이벤트를 발행한다")
	void deposit_publishesAccountBalanceChangedEvent() {
		// given
		Account account = stubLockedAccount(1L, "안개발", 0L);

		// when
		accountCommandService.deposit(1L, 50_000L);

		// then
		ArgumentCaptor<AccountBalanceChangedEvent> eventCaptor = ArgumentCaptor.forClass(
			AccountBalanceChangedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		AccountBalanceChangedEvent event = eventCaptor.getValue();
		assertThat(event.accountId()).isEqualTo(1L);
		assertThat(event.changeType()).isEqualTo(ChangeType.DEPOSIT);
		assertThat(event.previousBalance()).isEqualTo(0L);
		assertThat(event.changeAmount()).isEqualTo(50_000L);
		assertThat(event.currentBalance()).isEqualTo(50_000L);
		assertThat(event.occurredAt()).isNotNull();
	}

	@Test
	@DisplayName("출금 시 잔액 변경 이벤트를 발행한다")
	void withdraw_publishesAccountBalanceChangedEvent() {
		// given
		Account account = stubLockedAccount(1L, "이개발", 100_000L);
		DailyUsage usage = stubDailyUsage(1L);

		// when
		accountCommandService.withdraw(1L, 30_000L);

		// then
		ArgumentCaptor<AccountBalanceChangedEvent> eventCaptor = ArgumentCaptor.forClass(
			AccountBalanceChangedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		AccountBalanceChangedEvent event = eventCaptor.getValue();
		assertThat(event.accountId()).isEqualTo(1L);
		assertThat(event.changeType()).isEqualTo(ChangeType.WITHDRAWAL);
		assertThat(event.previousBalance()).isEqualTo(100_000L);
		assertThat(event.changeAmount()).isEqualTo(30_000L);
		assertThat(event.currentBalance()).isEqualTo(70_000L);
		assertThat(event.occurredAt()).isNotNull();
	}

	@Test
	@DisplayName("잔액이 부족하면 출금이 실패한다")
	void withdraw_insufficientBalance_throwsException() {
		// given
		Account account = stubLockedAccount(1L, "박개발", 10_000L);
		stubDailyUsage(1L);
		doThrow(new InsufficientBalanceException("잔액이 부족합니다"))
			.when(balancePolicy).validate(10_000L, 15_000L);

		// when & then
		assertThatThrownBy(() -> accountCommandService.withdraw(1L, 15_000L))
			.isInstanceOf(InsufficientBalanceException.class)
			.hasMessageContaining("잔액이 부족합니다");
	}

	@Test
	@DisplayName("일일 출금 한도를 초과하면 예외가 발생한다")
	void withdraw_exceedsDailyLimit_throwsException() {
		// given
		Account account = stubLockedAccount(1L, "최개발", 1_000_000L);
		DailyUsage usage = stubDailyUsage(1L);
		usage.addWithdrawal(900_000L);
		doThrow(new DailyLimitExceededException("일일 출금 한도를 초과했습니다"))
			.when(withdrawalLimitPolicy).validate(900_000L, 200_000L);

		// when & then
		assertThatThrownBy(() -> accountCommandService.withdraw(1L, 200_000L))
			.isInstanceOf(DailyLimitExceededException.class)
			.hasMessageContaining("일일 출금 한도를 초과했습니다");
	}

	private Account stubLockedAccount(Long id, String ownerName, long initialBalance) {
		Account account = Account.create(ownerName);
		if (initialBalance > 0) {
			account.increaseBalance(initialBalance);
		}
		when(accountQueryService.findActiveAccountWithLock(eq(id))).thenReturn(account);
		return account;
	}

	private DailyUsage stubDailyUsage(Long accountId) {
		DailyUsage usage = DailyUsage.create(accountId, LocalDate.now());
		when(dailyUsageService.getOrCreateToday(accountId)).thenReturn(usage);
		return usage;
	}
}
