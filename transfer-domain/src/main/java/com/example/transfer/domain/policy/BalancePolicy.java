package com.example.transfer.domain.policy;

import org.springframework.stereotype.Component;

import com.example.transfer.domain.exception.InsufficientBalanceException;

@Component
public class BalancePolicy {

	public void validate(Long currentBalance, Long requestAmount) {
		if (currentBalance < requestAmount) {
			throw new InsufficientBalanceException("잔액이 부족합니다");
		}
	}
}
