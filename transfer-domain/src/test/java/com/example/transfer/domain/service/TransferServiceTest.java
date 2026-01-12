package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.event.TransferCompletedEvent;
import com.example.transfer.domain.exception.DailyLimitExceededException;
import com.example.transfer.domain.exception.InsufficientBalanceException;
import com.example.transfer.domain.exception.SameAccountTransferException;
import com.example.transfer.domain.policy.BalancePolicy;
import com.example.transfer.domain.policy.FeePolicy;
import com.example.transfer.domain.policy.TransferLimitPolicy;

@ExtendWith(MockitoExtension.class)
@DisplayName("이체 서비스")
class TransferServiceTest {

	@Mock
	private AccountQueryService accountQueryService;

	@Mock
	private DailyUsageService dailyUsageService;

	@Mock
	private AccountTransactionEntryService accountTransactionEntryService;

	@Mock
	private TransferLimitPolicy transferLimitPolicy;

	@Mock
	private BalancePolicy balancePolicy;

	@Mock
	private FeePolicy feePolicy;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private TransferService transferService;

	@BeforeEach
	void setUpTransactionMocks() {
		when(accountTransactionEntryService.saveTransferOut(anyLong(), anyLong(), anyLong(), any(UUID.class)))
			.thenAnswer(invocation -> AccountTransactionEntry.createTransferOut(
				invocation.getArgument(0),
				invocation.getArgument(1),
				invocation.getArgument(2),
				invocation.getArgument(3)
			));
	}

	@Test
	@DisplayName("이체 시 잔액 이동과 거래 내역이 저장된다")
	void transfer_movesMoneyAndPersistsEntries() {
		// given
		Long fromId = 20L;
		Long toId = 10L;
		Long amount = 100_000L;
		Long fee = 1_000L;
		Account from = stubLockedAccount(fromId, "Sender", 200_000L);
		Account to = stubLockedAccount(toId, "Receiver", 0L);
		Long startingBalance = from.getBalanceWon();
		DailyUsage usage = stubDailyUsage(fromId, 10_000L);
		Long usedToday = usage.getTransferTotalWon();
		stubFee(amount, fee);

		// when
		TransferResult result = transferService.transfer(fromId, toId, amount);

		// then
		assertThat(result.transferId()).isNotNull();
		assertThat(result.amount()).isEqualTo(amount);
		assertThat(result.fee()).isEqualTo(fee);
		assertThat(result.occurredAt()).isNotNull();
		assertThat(from.getBalanceWon()).isEqualTo(200_000L - amount - fee);
		assertThat(to.getBalanceWon()).isEqualTo(amount);
		assertThat(usage.getTransferTotalWon()).isEqualTo(usedToday + amount);
		verify(transferLimitPolicy).validate(usedToday, amount);
		verify(balancePolicy).validate(startingBalance, amount + fee);
		ArgumentCaptor<UUID> transferIdCaptor = ArgumentCaptor.forClass(UUID.class);
		verify(accountTransactionEntryService)
			.saveTransferOut(anyLong(), anyLong(), anyLong(), transferIdCaptor.capture());
		UUID usedTransferId = transferIdCaptor.getValue();
		verify(accountTransactionEntryService).saveFee(fromId, fee, usedTransferId);
		verify(accountTransactionEntryService).saveTransferIn(toId, amount, fromId, usedTransferId);
	}

	@Test
	@DisplayName("이체 시 당일 이용내역을 조회한다")
	void transfer_requestsDailyUsageFromService() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		Long amount = 50_000L;
		Account from = stubLockedAccount(fromId, "From", 100_000L);
		Account to = stubLockedAccount(toId, "To", 0L);
		DailyUsage usage = stubDailyUsage(fromId, 0L);
		stubFee(amount, 500L);

		// when
		transferService.transfer(fromId, toId, amount);

		// then
		verify(dailyUsageService).getOrCreateToday(fromId);
	}

	@Test
	@DisplayName("같은 계좌로 이체하면 예외가 발생한다")
	void transfer_sameAccount_throwsException() {
		// when & then
		assertThatThrownBy(() -> transferService.transfer(1L, 1L, 10_000L))
			.isInstanceOf(SameAccountTransferException.class);
	}

	@Test
	@DisplayName("이체 완료 시 이벤트를 발행한다")
	void transfer_publishesTransferCompletedEvent() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		Long amount = 100_000L;
		Long fee = 1_000L;
		Account from = stubLockedAccount(fromId, "Sender", 200_000L);
		Account to = stubLockedAccount(toId, "Receiver", 0L);
		DailyUsage usage = stubDailyUsage(fromId, 0L);
		stubFee(amount, fee);

		// when
		transferService.transfer(fromId, toId, amount);

		// then
		ArgumentCaptor<TransferCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TransferCompletedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		TransferCompletedEvent event = eventCaptor.getValue();
		assertThat(event.fromAccountId()).isEqualTo(fromId);
		assertThat(event.toAccountId()).isEqualTo(toId);
		assertThat(event.amount()).isEqualTo(amount);
		assertThat(event.fee()).isEqualTo(fee);
		assertThat(event.transferId()).isNotNull();
		assertThat(event.occurredAt()).isNotNull();
	}

	@Test
	@DisplayName("잔액이 부족하면 이체가 중단된다")
	void transfer_insufficientBalance_throwsException() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		Long amount = 100_000L;
		Long fee = 1_000L;
		Account from = stubLockedAccount(fromId, "Sender", 50_000L);
		Account to = stubLockedAccount(toId, "Receiver", 0L);
		stubDailyUsage(fromId, 0L);
		stubFee(amount, fee);
		doThrow(new InsufficientBalanceException("잔액이 부족합니다"))
			.when(balancePolicy).validate(from.getBalanceWon(), amount + fee);

		// when & then
		assertThatThrownBy(() -> transferService.transfer(fromId, toId, amount))
			.isInstanceOf(InsufficientBalanceException.class)
			.hasMessageContaining("잔액이 부족합니다");
	}

	@Test
	@DisplayName("일일 이체 한도를 초과하면 예외가 발생한다")
	void transfer_exceedsDailyLimit_throwsException() {
		// given
		Long fromId = 1L;
		Long toId = 2L;
		Long amount = 300_000L;
		Account from = stubLockedAccount(fromId, "Sender", 5_000_000L);
		Account to = stubLockedAccount(toId, "Receiver", 0L);
		DailyUsage usage = stubDailyUsage(fromId, 2_800_000L); // 이미 280만원 이체
		doThrow(new DailyLimitExceededException("일일 이체 한도를 초과했습니다"))
			.when(transferLimitPolicy).validate(usage.getTransferTotalWon(), amount);

		// when & then
		assertThatThrownBy(() -> transferService.transfer(fromId, toId, amount))
			.isInstanceOf(DailyLimitExceededException.class)
			.hasMessageContaining("일일 이체 한도를 초과했습니다");
	}

	private Account stubLockedAccount(Long id, String ownerName, long initialBalance) {
		Account account = Account.create(ownerName);
		if (initialBalance > 0) {
			account.increaseBalance(initialBalance);
		}
		when(accountQueryService.findActiveAccountWithLock(eq(id))).thenReturn(account);
		return account;
	}

	private DailyUsage stubDailyUsage(Long accountId, long transferredAmount) {
		DailyUsage usage = DailyUsage.create(accountId, LocalDate.now());
		if (transferredAmount > 0) {
			usage.addTransfer(transferredAmount);
		}
		when(dailyUsageService.getOrCreateToday(accountId)).thenReturn(usage);
		return usage;
	}

	private void stubFee(Long amount, Long fee) {
		when(feePolicy.calculate(amount)).thenReturn(fee);
	}
}
