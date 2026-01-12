package com.example.transfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "WithdrawRequest", description = "출금 요청 바디")
public class WithdrawRequest {

	@NotNull
	@Positive(message = "금액은 0보다 커야 합니다")
	@Schema(description = "출금 금액(원)", example = "80000")
	private Long amountWon;
}
