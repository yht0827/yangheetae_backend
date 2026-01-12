package com.example.transfer.infra.repository;

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
}
