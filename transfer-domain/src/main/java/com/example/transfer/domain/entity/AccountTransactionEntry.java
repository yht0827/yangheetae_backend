package com.example.transfer.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_transaction_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountTransactionEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long accountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransactionType type;

	@Column(nullable = false)
	private Long amountWon;

	private Long counterpartyAccountId;

	@Column(columnDefinition = "BINARY(16)")
	private UUID relatedTransferId;

	@Column(nullable = false)
	private LocalDateTime occurredAt;

	public static AccountTransactionEntry create(
		Long accountId,
		TransactionType type,
		Long amountWon,
		Long counterpartyAccountId,
		UUID relatedTransferId
	) {
		AccountTransactionEntry entry = new AccountTransactionEntry();
		entry.accountId = accountId;
		entry.type = type;
		entry.amountWon = amountWon;
		entry.counterpartyAccountId = counterpartyAccountId;
		entry.relatedTransferId = relatedTransferId;
		entry.occurredAt = LocalDateTime.now();
		return entry;
	}
}
