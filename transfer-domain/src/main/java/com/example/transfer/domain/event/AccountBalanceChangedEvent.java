package com.example.transfer.domain.event;

import java.time.LocalDateTime;

/**
 * 계좌 잔액 변경 시 발행되는 도메인 이벤트
 */
public record AccountBalanceChangedEvent(
	Long accountId,
	ChangeType changeType,
	Long previousBalance,
	Long changeAmount,
	Long currentBalance,
	LocalDateTime occurredAt
) {
	public enum ChangeType {
		DEPOSIT,      // 입금
		WITHDRAWAL,   // 출금
		TRANSFER_OUT, // 이체 출금
		TRANSFER_IN   // 이체 입금
	}
}
