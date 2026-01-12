package com.example.transfer.domain.repository;

import java.util.Optional;

import com.example.transfer.domain.entity.IdempotencyRecord;

public interface IdempotencyRecordRepository {

	IdempotencyRecord save(IdempotencyRecord record);

	Optional<IdempotencyRecord> findByIdempotencyKey(String key);

	void delete(IdempotencyRecord record);
}
