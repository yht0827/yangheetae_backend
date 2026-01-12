package com.example.transfer.infra.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.entity.DailyUsageId;
import com.example.transfer.domain.repository.DailyUsageRepository;

import jakarta.persistence.LockModeType;

public interface DailyUsageJpaRepository extends JpaRepository<DailyUsage, DailyUsageId>, DailyUsageRepository {

	@Override
	Optional<DailyUsage> findByAccountIdAndUsageDate(Long accountId, LocalDate usageDate);

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM DailyUsage d WHERE d.accountId = :accountId AND d.usageDate = :usageDate")
	Optional<DailyUsage> findByAccountIdAndUsageDateWithLock(
		@Param("accountId") Long accountId,
		@Param("usageDate") LocalDate usageDate
	);
}
