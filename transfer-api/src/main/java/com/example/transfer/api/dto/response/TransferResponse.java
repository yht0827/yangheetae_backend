package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.transfer.domain.dto.TransferResult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "TransferResponse", description = "이체 결과")
public class TransferResponse {

	@Schema(description = "이체 식별자", example = "a3f4d8e4-9c0d-4e4f-8f7a-1234567890ab")
	private UUID transferId;

	@Schema(description = "실제 이체 금액(원)", example = "50000")
	private Long amountWon;

	@Schema(description = "수수료(원)", example = "500")
	private Long feeWon;

	@Schema(description = "거래 발생 시각", example = "2024-02-01T10:15:30")
	private LocalDateTime occurredAt;

	public static TransferResponse from(TransferResult result) {
		return TransferResponse.builder()
			.transferId(result.transferId())
			.amountWon(result.amount())
			.feeWon(result.fee())
			.occurredAt(result.occurredAt())
			.build();
	}
}
