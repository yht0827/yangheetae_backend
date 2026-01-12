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
public class AccountTransactionEntry extends BaseTimeEntity {

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

	private Long counterpartyAccountId; // 이체 상대 계좌

	@Column(columnDefinition = "BINARY(16)")
	private UUID relatedTransferId; // 이체 묶음 ID

	@Column(nullable = false)
	private LocalDateTime occurredAt;

	public static AccountTransactionEntry createDeposit(Long accountId, Long amount) {
		return create(accountId, TransactionType.DEPOSIT, amount, null, null);
	}

	public static AccountTransactionEntry createWithdrawal(Long accountId, Long amount) {
		return create(accountId, TransactionType.WITHDRAWAL, amount, null, null);
	}

	public static AccountTransactionEntry createTransferOut(
		Long accountId, Long amount, Long counterpartyAccountId, UUID transferId
	) {
		return create(accountId, TransactionType.TRANSFER_OUT, amount, counterpartyAccountId, transferId);
	}

	public static AccountTransactionEntry createTransferIn(
		Long accountId, Long amount, Long counterpartyAccountId, UUID transferId
	) {
		return create(accountId, TransactionType.TRANSFER_IN, amount, counterpartyAccountId, transferId);
	}

	public static AccountTransactionEntry createFee(Long accountId, Long amount, UUID transferId) {
		return create(accountId, TransactionType.FEE, amount, null, transferId);
	}

	private static AccountTransactionEntry create(
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
