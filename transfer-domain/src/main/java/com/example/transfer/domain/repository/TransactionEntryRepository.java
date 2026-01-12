package com.example.transfer.domain.repository;

import java.util.List;

import com.example.transfer.domain.entity.AccountTransactionEntry;

public interface TransactionEntryRepository {

	AccountTransactionEntry save(AccountTransactionEntry entry);

	List<AccountTransactionEntry> findByAccountIdOrderByOccurredAtDesc(Long accountId);

	Long sumFees(Long accountId, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
