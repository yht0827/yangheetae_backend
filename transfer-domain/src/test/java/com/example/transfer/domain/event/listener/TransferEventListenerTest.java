package com.example.transfer.domain.event.listener;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.transfer.domain.event.AccountBalanceChangedEvent;
import com.example.transfer.domain.event.AccountBalanceChangedEvent.ChangeType;
import com.example.transfer.domain.event.TransferCompletedEvent;

@DisplayName("이벤트 리스너")
class TransferEventListenerTest {

	private final TransferEventListener listener = new TransferEventListener();

	@Test
	@DisplayName("이체 완료 이벤트를 처리해도 예외가 발생하지 않는다")
	void handleTransferCompleted_doesNotThrow() {
		TransferCompletedEvent event = new TransferCompletedEvent(
			UUID.randomUUID(),
			1L,
			2L,
			100_000L,
			1_000L,
			LocalDateTime.now()
		);

		assertThatNoException()
			.isThrownBy(() -> listener.handleTransferCompleted(event));
	}

	@Test
	@DisplayName("입금 잔액 변경 이벤트를 처리해도 예외가 발생하지 않는다")
	void handleBalanceChanged_deposit_doesNotThrow() {
		AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(
			1L,
			ChangeType.DEPOSIT,
			0L,
			50_000L,
			50_000L,
			LocalDateTime.now()
		);

		assertThatNoException()
			.isThrownBy(() -> listener.handleBalanceChanged(event));
	}

	@Test
	@DisplayName("출금 잔액 변경 이벤트를 처리해도 예외가 발생하지 않는다")
	void handleBalanceChanged_withdrawal_doesNotThrow() {
		AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(
			1L,
			ChangeType.WITHDRAWAL,
			100_000L,
			30_000L,
			70_000L,
			LocalDateTime.now()
		);

		assertThatNoException()
			.isThrownBy(() -> listener.handleBalanceChanged(event));
	}

	@Test
	@DisplayName("송금 출금 이벤트를 처리해도 예외가 발생하지 않는다")
	void handleBalanceChanged_transferOut_doesNotThrow() {
		AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(
			1L,
			ChangeType.TRANSFER_OUT,
			200_000L,
			100_000L,
			100_000L,
			LocalDateTime.now()
		);

		assertThatNoException()
			.isThrownBy(() -> listener.handleBalanceChanged(event));
	}

	@Test
	@DisplayName("송금 입금 이벤트를 처리해도 예외가 발생하지 않는다")
	void handleBalanceChanged_transferIn_doesNotThrow() {
		AccountBalanceChangedEvent event = new AccountBalanceChangedEvent(
			2L,
			ChangeType.TRANSFER_IN,
			50_000L,
			100_000L,
			150_000L,
			LocalDateTime.now()
		);

		assertThatNoException()
			.isThrownBy(() -> listener.handleBalanceChanged(event));
	}
}
