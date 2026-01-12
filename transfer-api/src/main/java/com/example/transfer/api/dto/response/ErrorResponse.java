package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;

import com.example.transfer.api.exception.ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "ErrorResponse", description = "에러 응답")
public class ErrorResponse {

	@Schema(description = "에러 코드", example = "ACCOUNT_NOT_FOUND")
	private String code;

	@Schema(description = "에러 메시지", example = "계좌를 찾을 수 없습니다")
	private String message;

	@Schema(description = "발생 시각", example = "2024-02-01T10:15:30")
	private LocalDateTime timestamp;

	public static ErrorResponse of(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.code(errorCode.name())
			.message(errorCode.getMessage())
			.timestamp(LocalDateTime.now())
			.build();
	}
}
