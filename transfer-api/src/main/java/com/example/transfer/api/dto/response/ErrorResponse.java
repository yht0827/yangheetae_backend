package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;

import com.example.transfer.api.exception.ErrorCode;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

	private String code;
	private String message;
	private LocalDateTime timestamp;

	public static ErrorResponse of(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.code(errorCode.name())
			.message(errorCode.getMessage())
			.timestamp(LocalDateTime.now())
			.build();
	}
}
