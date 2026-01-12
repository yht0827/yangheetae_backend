package com.example.transfer.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.example.transfer.domain.entity.DailyUsage;

public interface DailyUsageRepository {

	DailyUsage save(DailyUsage dailyUsage);

	Optional<DailyUsage> findByAccountIdAndUsageDate(Long accountId, LocalDate usageDate);

	Optional<DailyUsage> findByAccountIdAndUsageDateWithLock(Long accountId, LocalDate usageDate);
}
