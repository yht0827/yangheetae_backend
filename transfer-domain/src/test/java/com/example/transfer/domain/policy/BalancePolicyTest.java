package com.example.transfer.domain.policy;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.transfer.domain.exception.InsufficientBalanceException;

@DisplayName("잔액 정책")
class BalancePolicyTest {

	private final BalancePolicy balancePolicy = new BalancePolicy();

	@ParameterizedTest(name = "잔액 {0}원에서 {1}원 출금 -> {2}")
	@DisplayName("출금 가능 여부를 경계값으로 검증한다")
	@CsvSource({
		"50000, 30000, true",   // 충분한 잔액
		"10000, 10000, true",   // 정확히 일치
		"10000, 9999, true",    // 1원 남음
		"0, 0, true",           // 0원 출금
		"10000, 10001, false",  // 1원 부족
		"10000, 15000, false",  // 잔액 부족
		"0, 1, false"           // 0원에서 출금 시도
	})
	void validate_boundaryValues(Long balance, Long amount, boolean shouldPass) {
		if (shouldPass) {
			assertThatNoException()
				.isThrownBy(() -> balancePolicy.validate(balance, amount));
		} else {
			assertThatThrownBy(() -> balancePolicy.validate(balance, amount))
				.isInstanceOf(InsufficientBalanceException.class);
		}
	}

	@Test
	@DisplayName("잔액 부족 시 사용자 메시지를 반환한다")
	void validate_insufficientBalance_hasCorrectMessage() {
		assertThatThrownBy(() -> balancePolicy.validate(10_000L, 15_000L))
			.isInstanceOf(InsufficientBalanceException.class)
			.hasMessageContaining("잔액이 부족합니다");
	}
}
