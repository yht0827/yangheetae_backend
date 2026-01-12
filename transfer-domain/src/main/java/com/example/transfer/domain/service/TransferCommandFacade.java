package com.example.transfer.domain.service;

import org.springframework.stereotype.Service;

import com.example.transfer.domain.dto.TransferResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferCommandFacade {

	private final TransferService transferService;
	private final IdempotencyService idempotencyService;

	public TransferResult transfer(Long fromId, Long toId, Long amount, String idempotencyKey) {
		return idempotencyService.execute(idempotencyKey, () -> transferService.transfer(fromId, toId, amount));
	}
}
