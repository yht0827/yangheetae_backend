package com.example.transfer.domain.policy;

import org.springframework.stereotype.Component;

import com.example.transfer.domain.exception.DailyLimitExceededException;

@Component
public class TransferLimitPolicy {

	private static final Long DAILY_LIMIT = 3_000_000L; // 300만원

	public void validate(Long currentTotal, Long requestAmount) {
		if (currentTotal + requestAmount > DAILY_LIMIT) {
			throw new DailyLimitExceededException("일일 이체 한도를 초과했습니다");
		}
	}
}
