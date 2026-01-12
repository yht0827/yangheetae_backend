package com.example.transfer.domain.service;

import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.entity.IdempotencyRecord;
import com.example.transfer.domain.exception.IdempotentRequestInProgressException;
import com.example.transfer.domain.repository.IdempotencyRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class IdempotencyService {

	private final IdempotencyRecordRepository idempotencyRecordRepository;

	public TransferResult execute(String key, Supplier<TransferResult> action) {
		validateKey(key);
		return idempotencyRecordRepository.findByIdempotencyKey(key)
			.map(this::toResultIfCompleted)
			.orElseGet(() -> processNewKey(key, action));
	}

	private TransferResult processNewKey(String key, Supplier<TransferResult> action) {
		IdempotencyRecord record = IdempotencyRecord.pending(key);
		try {
			record = idempotencyRecordRepository.save(record);
		} catch (DataIntegrityViolationException e) {
			return idempotencyRecordRepository.findByIdempotencyKey(key)
				.map(this::toResultIfCompleted)
				.orElseThrow(() -> new IdempotentRequestInProgressException("동일한 요청이 처리 중입니다"));
		}

		try {
			TransferResult result = action.get();
			record.complete(result.transferId(), result.amount(), result.fee(), result.occurredAt());
			idempotencyRecordRepository.save(record);
			return result;
		} catch (RuntimeException e) {
			idempotencyRecordRepository.delete(record);
			throw e;
		}
	}

	private TransferResult toResultIfCompleted(IdempotencyRecord record) {
		if (!record.isCompleted()) {
			throw new IdempotentRequestInProgressException("동일한 요청이 처리 중입니다");
		}
		return new TransferResult(
			record.getTransferId(),
			record.getAmountWon(),
			record.getFeeWon(),
			record.getOccurredAt()
		);
	}

	private void validateKey(String key) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Idempotency-Key 헤더는 필수입니다");
		}
	}
}
