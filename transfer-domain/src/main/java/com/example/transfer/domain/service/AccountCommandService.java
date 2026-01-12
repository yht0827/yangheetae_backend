package com.example.transfer.domain.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.policy.BalancePolicy;
import com.example.transfer.domain.policy.WithdrawalLimitPolicy;
import com.example.transfer.domain.repository.AccountRepository;
import com.example.transfer.domain.repository.DailyUsageRepository;
import com.example.transfer.domain.repository.TransactionEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountCommandService {

	private final AccountQueryService accountQueryService;
	private final AccountRepository accountRepository;
	private final DailyUsageRepository dailyUsageRepository;
	private final TransactionEntryRepository transactionEntryRepository;
	private final BalancePolicy balancePolicy;
	private final WithdrawalLimitPolicy withdrawalLimitPolicy;

	public Account createAccount(String ownerName) {
		Account account = Account.create(ownerName);

		return accountRepository.save(account);
	}

	public void deleteAccount(Long accountId) {
		Account account = accountQueryService.findActiveAccount(accountId);

		// 계좌 삭제 (soft-delete)
		account.delete();
	}

	public Account deposit(Long accountId, Long amount) {
		Account account = accountQueryService.findActiveAccountWithLock(accountId);

		account.increaseBalance(amount);

		// 입금
		AccountTransactionEntry deposit = AccountTransactionEntry.createDeposit(accountId, amount);
		transactionEntryRepository.save(deposit);

		return account;
	}

	public Account withdraw(Long accountId, Long amount) {
		Account account = accountQueryService.findActiveAccountWithLock(accountId);
		DailyUsage usage = getOrCreateDailyUsage(accountId, LocalDate.now());

		// 한도 체크
		withdrawalLimitPolicy.validate(usage.getWithdrawalTotalWon(), amount);

		// 잔액 체크
		balancePolicy.validate(account.getBalanceWon(), amount);

		// 차감
		account.decreaseBalance(amount);
		usage.addWithdrawal(amount);

		// 출금
		AccountTransactionEntry withdrawal = AccountTransactionEntry.createWithdrawal(accountId, amount);
		transactionEntryRepository.save(withdrawal);

		return account;
	}

	private DailyUsage getOrCreateDailyUsage(Long accountId, LocalDate date) {
		return dailyUsageRepository.findByAccountIdAndUsageDateWithLock(accountId, date)
			.orElseGet(() -> dailyUsageRepository.save(DailyUsage.create(accountId, date)));
	}
}
