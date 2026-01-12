package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.transfer.domain.service.TransferResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResponse {

	private UUID transferId;
	private Long amountWon;
	private Long feeWon;
	private LocalDateTime occurredAt;

	public static TransferResponse from(TransferResult result) {
		return TransferResponse.builder()
			.transferId(result.getTransferId())
			.amountWon(result.getAmount())
			.feeWon(result.getFee())
			.occurredAt(result.getOccurredAt())
			.build();
	}
}
