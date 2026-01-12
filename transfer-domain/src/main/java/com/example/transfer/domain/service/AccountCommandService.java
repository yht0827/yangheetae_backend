package com.example.transfer.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.event.AccountBalanceChangedEvent;
import com.example.transfer.domain.event.AccountBalanceChangedEvent.ChangeType;
import com.example.transfer.domain.policy.BalancePolicy;
import com.example.transfer.domain.policy.WithdrawalLimitPolicy;
import com.example.transfer.domain.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountCommandService {

	private final AccountQueryService accountQueryService;
	private final AccountRepository accountRepository;
	private final DailyUsageService dailyUsageService;
	private final AccountTransactionEntryService accountTransactionEntryService;
	private final BalancePolicy balancePolicy;
	private final WithdrawalLimitPolicy withdrawalLimitPolicy;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

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

		Long previousBalance = account.getBalanceWon();
		account.increaseBalance(amount);
		accountTransactionEntryService.saveDeposit(accountId, amount);

		// 잔액 변경 이벤트 발행
		eventPublisher.publishEvent(new AccountBalanceChangedEvent(
			accountId, ChangeType.DEPOSIT, previousBalance, amount,
			account.getBalanceWon(), LocalDateTime.now(clock)
		));

		return account;
	}

	public Account withdraw(Long accountId, Long amount) {
		Account account = accountQueryService.findActiveAccountWithLock(accountId);
		DailyUsage usage = dailyUsageService.getOrCreateToday(accountId);

		// 한도 체크
		withdrawalLimitPolicy.validate(usage.getWithdrawalTotalWon(), amount);

		// 잔액 체크
		balancePolicy.validate(account.getBalanceWon(), amount);

		Long previousBalance = account.getBalanceWon();

		// 차감
		account.decreaseBalance(amount);
		usage.addWithdrawal(amount);

		// 거래 내역 저장
		accountTransactionEntryService.saveWithdrawal(accountId, amount);

		// 잔액 변경 이벤트 발행
		eventPublisher.publishEvent(new AccountBalanceChangedEvent(
			accountId, ChangeType.WITHDRAWAL, previousBalance, amount,
			account.getBalanceWon(), LocalDateTime.now(clock)
		));

		return account;
	}
}
