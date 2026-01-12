package com.example.transfer.domain.policy;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.transfer.domain.exception.DailyLimitExceededException;

@DisplayName("출금 한도 정책")
class WithdrawalLimitPolicyTest {

	private final WithdrawalLimitPolicy policy = new WithdrawalLimitPolicy();

	@ParameterizedTest(name = "당일 출금 {0}원 + {1}원 요청 -> {2}")
	@DisplayName("일일 출금 한도 경계값을 검증한다")
	@CsvSource({
		"500000, 300000, true",    // 50만 + 30만 = 80만 (한도 내)
		"900000, 100000, true",    // 90만 + 10만 = 100만 (정확히 한도)
		"999999, 1, true",         // 999,999 + 1 = 100만 (경계값)
		"1000000, 0, true",        // 한도 도달 + 0원 요청
		"0, 1000000, true",        // 첫 출금 100만원
		"900000, 200000, false",   // 90만 + 20만 = 110만 (한도 초과)
		"1000000, 1, false",       // 100만 + 1원 (한도 1원 초과)
		"0, 1100000, false"        // 첫 출금 110만원 (한도 초과)
	})
	void validate_boundaryValues(Long currentTotal, Long requestAmount, boolean shouldPass) {
		if (shouldPass) {
			assertThatNoException()
				.isThrownBy(() -> policy.validate(currentTotal, requestAmount));
		} else {
			assertThatThrownBy(() -> policy.validate(currentTotal, requestAmount))
				.isInstanceOf(DailyLimitExceededException.class);
		}
	}

	@Test
	@DisplayName("출금 한도 초과 시 사용자 메시지를 반환한다")
	void validate_exceedsLimit_hasCorrectMessage() {
		assertThatThrownBy(() -> policy.validate(900_000L, 200_000L))
			.isInstanceOf(DailyLimitExceededException.class)
			.hasMessageContaining("일일 출금 한도를 초과했습니다");
	}
}
