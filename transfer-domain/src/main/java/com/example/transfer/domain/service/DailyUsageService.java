package com.example.transfer.domain.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.repository.DailyUsageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyUsageService {

	private final DailyUsageRepository dailyUsageRepository;
	private final Clock clock;

	public DailyUsage getOrCreateToday(Long accountId) {
		LocalDate today = LocalDate.now(clock);
		return dailyUsageRepository.findByAccountIdAndUsageDateWithLock(accountId, today)
			.orElseGet(() -> dailyUsageRepository.save(DailyUsage.create(accountId, today)));
	}
}
