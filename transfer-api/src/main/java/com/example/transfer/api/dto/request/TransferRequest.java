package com.example.transfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "TransferRequest", description = "이체 요청 바디")
public class TransferRequest {

	@NotBlank
	@Pattern(regexp = "\\d{3}-\\d{4}-\\d{6}", message = "계좌번호 형식이 올바르지 않습니다")
	@Schema(description = "출금 계좌번호", example = "110-1234-567890")
	private String fromAccountNo;

	@NotBlank
	@Pattern(regexp = "\\d{3}-\\d{4}-\\d{6}", message = "계좌번호 형식이 올바르지 않습니다")
	@Schema(description = "입금 계좌번호", example = "110-5678-123456")
	private String toAccountNo;

	@NotNull
	@Positive
	@Schema(description = "이체 금액(원)", example = "50000")
	private Long amountWon;
}
