package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.transfer.domain.entity.AccountTransactionEntry;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "TransactionResponse", description = "거래 내역")
public class TransactionResponse {

	@Schema(description = "거래 ID", example = "5001")
	private Long id;

	@Schema(description = "계좌 ID", example = "1001")
	private Long accountId;

	@Schema(description = "거래 유형", example = "TRANSFER_OUT")
	private String type;

	@Schema(description = "거래 금액(원)", example = "50000")
	private Long amountWon;

	@Schema(description = "상대방 계좌 ID", example = "2001")
	private Long counterpartyAccountId;

	@Schema(description = "연관 이체 식별자", example = "a3f4d8e4-9c0d-4e4f-8f7a-1234567890ab")
	private UUID relatedTransferId;

	@Schema(description = "거래 발생 시각", example = "2024-02-01T10:15:30")
	private LocalDateTime occurredAt;

	public static TransactionResponse from(AccountTransactionEntry entry) {
		return TransactionResponse.builder()
			.id(entry.getId())
			.accountId(entry.getAccountId())
			.type(entry.getType().name())
			.amountWon(entry.getAmountWon())
			.counterpartyAccountId(entry.getCounterpartyAccountId())
			.relatedTransferId(entry.getRelatedTransferId())
			.occurredAt(entry.getOccurredAt())
			.build();
	}
}
