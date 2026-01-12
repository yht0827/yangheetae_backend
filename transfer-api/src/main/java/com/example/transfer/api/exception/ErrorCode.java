package com.example.transfer.api.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 400 Bad Request
	INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "금액은 0보다 커야 합니다"),
	SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "동일 계좌로 이체할 수 없습니다"),

	// 404 Not Found
	ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다"),

	// 409 Conflict
	INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "잔액이 부족합니다"),
	DAILY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "일일 한도를 초과했습니다"),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "동시 요청으로 처리에 실패했습니다"),
	IDEMPOTENT_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "동일한 요청이 처리 중입니다");

	private final HttpStatus status;
	private final String message;
}
