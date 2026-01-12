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
@Table(name = "idempotency_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IdempotencyStatus status;

	@Column(columnDefinition = "BINARY(16)")
	private UUID transferId;

	@Column
	private Long amountWon;

	@Column
	private Long feeWon;

	@Column
	private LocalDateTime occurredAt;

	public static IdempotencyRecord pending(String key) {
		IdempotencyRecord record = new IdempotencyRecord();
		record.idempotencyKey = key;
		record.status = IdempotencyStatus.IN_PROGRESS;
		return record;
	}

	public void complete(UUID transferId, Long amountWon, Long feeWon, LocalDateTime occurredAt) {
		this.transferId = transferId;
		this.amountWon = amountWon;
		this.feeWon = feeWon;
		this.occurredAt = occurredAt;
		this.status = IdempotencyStatus.COMPLETED;
	}

	public boolean isCompleted() {
		return this.status == IdempotencyStatus.COMPLETED;
	}
}
