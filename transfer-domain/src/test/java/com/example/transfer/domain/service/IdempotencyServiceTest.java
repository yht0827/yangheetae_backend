package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.entity.IdempotencyRecord;
import com.example.transfer.domain.exception.IdempotentRequestInProgressException;
import com.example.transfer.domain.repository.IdempotencyRecordRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("멱등성 서비스")
class IdempotencyServiceTest {

	@Mock
	private IdempotencyRecordRepository idempotencyRecordRepository;

	@InjectMocks
	private IdempotencyService idempotencyService;

	@Test
	@DisplayName("멱등 요청 실행 시 기록을 생성하고 결과를 반환한다")
	void execute_createsRecordAndReturnsResult() {
		// given
		String key = "transfer-1";
		IdempotencyRecord pending = IdempotencyRecord.pending(key);
		when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
		when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenReturn(pending);

		// when
		TransferResult result = idempotencyService.execute(key, () -> new TransferResult(
			UUID.randomUUID(), 10_000L, 100L, LocalDateTime.now()
		));

		// then
		assertThat(result.amount()).isEqualTo(10_000L);
		verify(idempotencyRecordRepository).save(pending);
	}

	@Test
	@DisplayName("완료된 기록이 있으면 캐시된 결과를 반환한다")
	void execute_returnsCachedResultWhenCompleted() {
		// given
		String key = "dup";
		IdempotencyRecord record = IdempotencyRecord.pending(key);
		UUID transferId = UUID.randomUUID();
		LocalDateTime occurredAt = LocalDateTime.now();
		record.complete(transferId, 5_000L, 50L, occurredAt);
		when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

		// when
		TransferResult result = idempotencyService.execute(key, () -> {
			throw new IllegalStateException();
		});

		// then
		assertThat(result.transferId()).isEqualTo(transferId);
		verify(idempotencyRecordRepository, never()).save(any());
	}

	@Test
	@DisplayName("진행 중인 요청이면 예외를 던진다")
	void execute_throwsWhenInProgress() {
		// given
		String key = "dup";
		IdempotencyRecord record = IdempotencyRecord.pending(key);
		when(idempotencyRecordRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(record));

		// when & then
		assertThatThrownBy(() -> idempotencyService.execute(key, () -> null))
			.isInstanceOf(IdempotentRequestInProgressException.class);
	}
}
