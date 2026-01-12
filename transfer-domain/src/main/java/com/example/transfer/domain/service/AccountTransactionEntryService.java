package com.example.transfer.domain.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.repository.TransactionEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountTransactionEntryService {

	private final TransactionEntryRepository transactionEntryRepository;

	public AccountTransactionEntry saveDeposit(Long accountId, Long amount) {
		return save(AccountTransactionEntry.createDeposit(accountId, amount));
	}

	public AccountTransactionEntry saveWithdrawal(Long accountId, Long amount) {
		return save(AccountTransactionEntry.createWithdrawal(accountId, amount));
	}

	public AccountTransactionEntry saveTransferOut(Long accountId, Long amount, Long counterpartyAccountId,
		UUID transferId) {
		return save(AccountTransactionEntry.createTransferOut(accountId, amount, counterpartyAccountId, transferId));
	}

	public AccountTransactionEntry saveTransferIn(Long accountId, Long amount, Long counterpartyAccountId,
		UUID transferId) {
		return save(AccountTransactionEntry.createTransferIn(accountId, amount, counterpartyAccountId, transferId));
	}

	public AccountTransactionEntry saveFee(Long accountId, Long amount, UUID transferId) {
		return save(AccountTransactionEntry.createFee(accountId, amount, transferId));
	}

	private AccountTransactionEntry save(AccountTransactionEntry entry) {
		return transactionEntryRepository.save(entry);
	}
}
