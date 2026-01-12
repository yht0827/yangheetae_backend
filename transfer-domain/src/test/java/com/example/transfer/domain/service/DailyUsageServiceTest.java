package com.example.transfer.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.repository.DailyUsageRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("일일 이용내역 서비스")
class DailyUsageServiceTest {

	@Mock
	private DailyUsageRepository dailyUsageRepository;

	private Clock clock;

	private DailyUsageService dailyUsageService;

	@BeforeEach
	void setUp() {
		LocalDate today = LocalDate.now();
		clock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
		dailyUsageService = new DailyUsageService(dailyUsageRepository, clock);
	}

	@Test
	@DisplayName("당일 이용내역이 있으면 그대로 반환한다")
	void getOrCreateToday_returnsExistingUsageWhenPresent() {
		// given
		Long accountId = 1L;
		LocalDate today = LocalDate.now(clock);
		DailyUsage existing = DailyUsage.create(accountId, today);
		existing.addTransfer(10_000L);
		when(dailyUsageRepository.findByAccountIdAndUsageDateWithLock(accountId, today))
			.thenReturn(Optional.of(existing));

		// when
		DailyUsage result = dailyUsageService.getOrCreateToday(accountId);

		// then
		assertThat(result).isSameAs(existing);
		verify(dailyUsageRepository, never()).save(any(DailyUsage.class));
	}

	@Test
	@DisplayName("당일 이용내역이 없으면 새로 생성한다")
	void getOrCreateToday_createsNewUsageWhenAbsent() {
		// given
		Long accountId = 2L;
		LocalDate today = LocalDate.now(clock);
		when(dailyUsageRepository.findByAccountIdAndUsageDateWithLock(accountId, today))
			.thenReturn(Optional.empty());
		when(dailyUsageRepository.save(any(DailyUsage.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// when
		DailyUsage result = dailyUsageService.getOrCreateToday(accountId);

		// then
		assertThat(result.getAccountId()).isEqualTo(accountId);
		assertThat(result.getUsageDate()).isEqualTo(today);
		assertThat(result.getTransferTotalWon()).isZero();
		assertThat(result.getWithdrawalTotalWon()).isZero();
		verify(dailyUsageRepository).save(result);
	}
}
