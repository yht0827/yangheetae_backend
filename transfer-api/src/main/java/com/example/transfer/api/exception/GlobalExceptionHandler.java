package com.example.transfer.api.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.transfer.api.dto.response.ErrorResponse;
import com.example.transfer.domain.exception.AccountNotFoundException;
import com.example.transfer.domain.exception.DailyLimitExceededException;
import com.example.transfer.domain.exception.IdempotentRequestInProgressException;
import com.example.transfer.domain.exception.InsufficientBalanceException;
import com.example.transfer.domain.exception.SameAccountTransferException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(AccountNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException e) {
		return createErrorResponse(ErrorCode.ACCOUNT_NOT_FOUND);
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException e) {
		return createErrorResponse(ErrorCode.INSUFFICIENT_BALANCE);
	}

	@ExceptionHandler(DailyLimitExceededException.class)
	public ResponseEntity<ErrorResponse> handleDailyLimitExceeded(DailyLimitExceededException e) {
		return createErrorResponse(ErrorCode.DAILY_LIMIT_EXCEEDED);
	}

	@ExceptionHandler(SameAccountTransferException.class)
	public ResponseEntity<ErrorResponse> handleSameAccountTransfer(SameAccountTransferException e) {
		return createErrorResponse(ErrorCode.SAME_ACCOUNT_TRANSFER);
	}

	@ExceptionHandler(IdempotentRequestInProgressException.class)
	public ResponseEntity<ErrorResponse> handleIdempotentInProgress(IdempotentRequestInProgressException e) {
		return createErrorResponse(ErrorCode.IDEMPOTENT_REQUEST_IN_PROGRESS);
	}

	private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode) {
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
	}
}
