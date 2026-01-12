package com.example.transfer.domain.event.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.transfer.domain.event.AccountBalanceChangedEvent;
import com.example.transfer.domain.event.TransferCompletedEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TransferEventListener {

	/**
	 * 이체 완료 이벤트 처리
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleTransferCompleted(TransferCompletedEvent event) {
		log.info("[이체 완료] transferId={}, from={}, to={}, amount={}, fee={}",
			event.transferId(), event.fromAccountId(), event.toAccountId(),
			event.amount(), event.fee());

	}

	/**
	 * 계좌 잔액 변경 이벤트 처리
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBalanceChanged(AccountBalanceChangedEvent event) {
		log.info("[잔액 변경] accountId={}, type={}, {} -> {} (변경: {})",
			event.accountId(), event.changeType(),
			event.previousBalance(), event.currentBalance(), event.changeAmount());
	}
}
