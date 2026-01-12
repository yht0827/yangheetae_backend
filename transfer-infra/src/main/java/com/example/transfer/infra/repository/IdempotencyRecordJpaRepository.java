package com.example.transfer.infra.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.transfer.domain.entity.IdempotencyRecord;
import com.example.transfer.domain.repository.IdempotencyRecordRepository;

public interface IdempotencyRecordJpaRepository extends JpaRepository<IdempotencyRecord, Long>, IdempotencyRecordRepository {

	@Override
	Optional<IdempotencyRecord> findByIdempotencyKey(String key);
}
