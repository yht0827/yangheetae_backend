package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transfer.domain.repository.TransactionEntryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("통계 서비스")
class StatisticsServiceTest {

	@Mock
	private TransactionEntryRepository transactionEntryRepository;

	@InjectMocks
	private StatisticsService statisticsService;

	@Test
	@DisplayName("기간별 수수료 합계를 조회한다")
	void getFeeTotal_delegatesToRepository() {
		// given
		LocalDateTime from = LocalDateTime.now().minusDays(7);
		LocalDateTime to = LocalDateTime.now();
		when(transactionEntryRepository.sumFees(1L, from, to)).thenReturn(1_200L);

		// when
		Long total = statisticsService.getFeeTotal(1L, from, to);

		// then
		assertThat(total).isEqualTo(1_200L);
		verify(transactionEntryRepository).sumFees(1L, from, to);
	}
}
