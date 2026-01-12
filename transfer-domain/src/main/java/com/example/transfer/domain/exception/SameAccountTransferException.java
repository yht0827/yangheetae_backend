package com.example.transfer.domain.exception;

public class SameAccountTransferException extends RuntimeException {
	public SameAccountTransferException(String message) {
		super(message);
	}
}
