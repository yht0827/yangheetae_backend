package com.example.transfer.domain.exception;

public class DailyLimitExceededException extends RuntimeException {
	public DailyLimitExceededException(String message) {
		super(message);
	}
}
