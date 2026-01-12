package com.example.transfer.domain.policy;

import org.springframework.stereotype.Component;

import com.example.transfer.domain.exception.DailyLimitExceededException;

@Component
public class WithdrawalLimitPolicy {

	private static final Long DAILY_LIMIT = 1_000_000L; // 100만원

	public void validate(Long currentTotal, Long requestAmount) {
		if (currentTotal + requestAmount > DAILY_LIMIT) {
			throw new DailyLimitExceededException("일일 출금 한도를 초과했습니다");
		}
	}
}
