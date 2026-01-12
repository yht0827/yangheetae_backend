package com.example.transfer.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이체 완료 시 발행되는 도메인 이벤트
 */
public record TransferCompletedEvent(
	UUID transferId,
	Long fromAccountId,
	Long toAccountId,
	Long amount,
	Long fee,
	LocalDateTime occurredAt
) {
}