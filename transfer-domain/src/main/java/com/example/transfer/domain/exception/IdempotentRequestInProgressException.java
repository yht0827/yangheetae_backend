package com.example.transfer.domain.exception;

public class IdempotentRequestInProgressException extends RuntimeException {

	public IdempotentRequestInProgressException(String message) {
		super(message);
	}
}
