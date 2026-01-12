package com.example.transfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "DepositRequest", description = "입금 요청 바디")
public class DepositRequest {

	@NotNull
	@Positive(message = "금액은 0보다 커야 합니다")
	@Schema(description = "입금 금액(원)", example = "100000")
	private Long amountWon;
}
