package com.example.transfer.domain.policy;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.transfer.domain.exception.DailyLimitExceededException;

@DisplayName("이체 한도 정책")
class TransferLimitPolicyTest {

	private final TransferLimitPolicy policy = new TransferLimitPolicy();

	@ParameterizedTest(name = "당일 이체 {0}원 + {1}원 요청 -> {2}")
	@DisplayName("일일 이체 한도 경계값을 검증한다")
	@CsvSource({
		"2000000, 500000, true",    // 200만 + 50만 = 250만 (한도 내)
		"2800000, 200000, true",    // 280만 + 20만 = 300만 (정확히 한도)
		"2999999, 1, true",         // 2,999,999 + 1 = 300만 (경계값)
		"3000000, 0, true",         // 한도 도달 + 0원 요청
		"0, 3000000, true",         // 첫 이체 300만원
		"2800000, 300000, false",   // 280만 + 30만 = 310만 (한도 초과)
		"3000000, 1, false",        // 300만 + 1원 (한도 1원 초과)
		"0, 3100000, false"         // 첫 이체 310만원 (한도 초과)
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
	@DisplayName("한도 초과 시 사용자 메시지를 반환한다")
	void validate_exceedsLimit_hasCorrectMessage() {
		assertThatThrownBy(() -> policy.validate(2_800_000L, 300_000L))
			.isInstanceOf(DailyLimitExceededException.class)
			.hasMessageContaining("일일 이체 한도를 초과했습니다");
	}
}
