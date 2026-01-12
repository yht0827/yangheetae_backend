package com.example.transfer.domain.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.event.TransferCompletedEvent;
import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.entity.AccountTransactionEntry;
import com.example.transfer.domain.entity.DailyUsage;
import com.example.transfer.domain.exception.SameAccountTransferException;
import com.example.transfer.domain.policy.BalancePolicy;
import com.example.transfer.domain.policy.FeePolicy;
import com.example.transfer.domain.policy.TransferLimitPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

	private final AccountQueryService accountQueryService;
	private final DailyUsageService dailyUsageService;
	private final AccountTransactionEntryService accountTransactionEntryService;
	private final TransferLimitPolicy transferLimitPolicy;
	private final BalancePolicy balancePolicy;
	private final FeePolicy feePolicy;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public TransferResult transfer(Long fromId, Long toId, Long amount) {

		// 동일 계좌 체크
		validateDifferentAccounts(fromId, toId);

		// 데드락 방지: ID 오름차순 락
		Long firstId = Math.min(fromId, toId);
		Long secondId = Math.max(fromId, toId);

		Account first = accountQueryService.findActiveAccountWithLock(firstId);
		Account second = accountQueryService.findActiveAccountWithLock(secondId);

		Account from = (fromId.equals(firstId)) ? first : second;
		Account to = (fromId.equals(firstId)) ? second : first;
		UUID transferId = UUID.randomUUID();

		// 수수료 계산: floor(amount * 0.01)
		Long fee = feePolicy.calculate(amount);
		Long totalDeduction = amount + fee;

		// 한도 체크 (일 이체 300만원)
		DailyUsage usage = dailyUsageService.getOrCreateToday(fromId);
		transferLimitPolicy.validate(usage.getTransferTotalWon(), amount);

		// 잔액 체크 (amount + fee)
		balancePolicy.validate(from.getBalanceWon(), totalDeduction);

		// 잔액 변경
		from.decreaseBalance(totalDeduction);
		to.increaseBalance(amount);
		usage.addTransfer(amount);

		// 거래 내역 3건 저장 (동일 transferId로 묶음)
		AccountTransactionEntry transferOutEntry = accountTransactionEntryService.saveTransferOut(
			fromId, amount, toId, transferId
		);
		accountTransactionEntryService.saveFee(fromId, fee, transferId);
		accountTransactionEntryService.saveTransferIn(toId, amount, fromId, transferId);

		LocalDateTime occurredAt = transferOutEntry.getOccurredAt();

		// 이체 완료 이벤트 발행
		eventPublisher.publishEvent(new TransferCompletedEvent(
			transferId, fromId, toId, amount, fee, occurredAt
		));

		return new TransferResult(transferId, amount, fee, occurredAt);
	}

	private void validateDifferentAccounts(Long fromId, Long toId) {
		if (fromId.equals(toId)) {
			throw new SameAccountTransferException("출금 및 입금 계좌가 동일합니다");
		}
	}
}
