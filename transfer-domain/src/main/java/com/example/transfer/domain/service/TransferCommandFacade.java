package com.example.transfer.domain.service;

import org.springframework.stereotype.Service;

import com.example.transfer.domain.dto.TransferResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferCommandFacade {

	private final TransferService transferService;
	private final IdempotencyService idempotencyService;
	private final AccountQueryService accountQueryService;

	public TransferResult transfer(String fromAccountNo, String toAccountNo, Long amount, String idempotencyKey) {
		Long fromId = accountQueryService.resolveAccountId(fromAccountNo);
		Long toId = accountQueryService.resolveAccountId(toAccountNo);
		return idempotencyService.execute(idempotencyKey, () -> transferService.transfer(fromId, toId, amount));
	}
}
