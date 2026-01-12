package com.example.transfer.infra.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.repository.TransactionEntryRepository;

public interface TransactionEntryJpaRepository
	extends JpaRepository<AccountTransactionEntry, Long>, TransactionEntryRepository {

	@Override
	@Query("SELECT e FROM AccountTransactionEntry e WHERE e.accountId = :accountId ORDER BY e.occurredAt DESC")
	List<AccountTransactionEntry> findByAccountIdOrderByOccurredAtDesc(@Param("accountId") Long accountId);

	@Override
	@Query("SELECT COALESCE(SUM(e.amountWon), 0) FROM AccountTransactionEntry e " +
		"WHERE e.type = com.example.transfer.domain.entity.TransactionType.FEE " +
		"AND e.occurredAt BETWEEN :from AND :to " +
		"AND (:accountId IS NULL OR e.accountId = :accountId)")
	Long sumFees(
		@Param("accountId") Long accountId,
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to
	);
}
