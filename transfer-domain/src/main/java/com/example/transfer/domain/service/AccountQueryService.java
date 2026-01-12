package com.example.transfer.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.exception.AccountNotFoundException;
import com.example.transfer.domain.repository.AccountRepository;
import com.example.transfer.domain.repository.TransactionEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryService {

	private final AccountRepository accountRepository;
	private final TransactionEntryRepository transactionEntryRepository;

	public Account getAccount(Long accountId) {
		return findActiveAccount(accountId);
	}

	public List<AccountTransactionEntry> getTransactions(Long accountId) {
		// 삭제된 계좌도 거래내역 조회 가능
		accountRepository.findById(accountId)
			.orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다"));

		return transactionEntryRepository.findByAccountIdOrderByOccurredAtDesc(accountId);
	}

	public Account findActiveAccount(Long accountId) {
		return accountRepository.findActiveById(accountId)
			.orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다"));
	}

	public Account findActiveAccountWithLock(Long accountId) {
		return accountRepository.findByIdWithLock(accountId)
			.orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다"));
	}
}
