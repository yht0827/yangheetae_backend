package com.example.transfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "TransferRequest", description = "이체 요청 바디")
public class TransferRequest {

	@NotNull
	@Schema(description = "출금 계좌 ID", example = "1001")
	private Long fromAccountId;

	@NotNull
	@Schema(description = "입금 계좌 ID", example = "2001")
	private Long toAccountId;

	@NotNull
	@Positive
	@Schema(description = "이체 금액(원)", example = "50000")
	private Long amountWon;
}
