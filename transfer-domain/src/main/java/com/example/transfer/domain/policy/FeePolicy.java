package com.example.transfer.domain.policy;

import org.springframework.stereotype.Component;

@Component
public class FeePolicy {

	private static final double FEE_RATE = 0.01; // 1%

	public Long calculate(Long amount) {
		return (long)Math.floor(amount * FEE_RATE);
	}
}
