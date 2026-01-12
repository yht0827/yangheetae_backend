package com.example.transfer.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResult(
	UUID transferId,
	Long amount,
	Long fee,
	LocalDateTime occurredAt
) {
}
