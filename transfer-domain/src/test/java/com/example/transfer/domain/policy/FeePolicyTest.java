package com.example.transfer.domain.policy;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("수수료 정책")
class FeePolicyTest {

	private final FeePolicy feePolicy = new FeePolicy();

	@Test
	@DisplayName("1만원 이체 시 수수료는 100원이다")
	void calculate_10000won_returns100won() {
		Long fee = feePolicy.calculate(10_000L);

		assertThat(fee).isEqualTo(100L);
	}

	@Test
	@DisplayName("100원 미만 이체는 수수료가 0원이다")
	void calculate_99won_returns0won_floor() {
		Long fee = feePolicy.calculate(99L);

		assertThat(fee).isEqualTo(0L);
	}

	@ParameterizedTest
	@DisplayName("수수료 계산을 다양한 금액에서 검증한다")
	@CsvSource({
		"0, 0",
		"1, 0",
		"99, 0",
		"100, 1",
		"1000, 10",
		"10000, 100",
		"100000, 1000",
		"1000000, 10000"
	})
	void calculate_boundaryValues(Long amount, Long expectedFee) {
		Long fee = feePolicy.calculate(amount);

		assertThat(fee).isEqualTo(expectedFee);
	}

	@Test
	@DisplayName("수수료 계산은 올림 없이 내림을 사용한다")
	void calculate_usesFloor_notRound() {
		// 150원 * 0.01 = 1.5 → floor → 1원
		Long fee = feePolicy.calculate(150L);

		assertThat(fee).isEqualTo(1L);
	}
}
