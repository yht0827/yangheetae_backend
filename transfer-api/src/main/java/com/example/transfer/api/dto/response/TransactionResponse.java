package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.transfer.domain.entity.AccountTransactionEntry;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponse {

	private Long id;
	private Long accountId;
	private String type;
	private Long amountWon;
	private Long counterpartyAccountId;
	private UUID relatedTransferId;
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
