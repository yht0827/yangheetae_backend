package com.example.transfer.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.transfer.api.dto.request.TransferRequest;
import com.example.transfer.api.dto.response.TransferResponse;
import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.service.TransferCommandFacade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer", description = "이체 API")
public class TransferController {

	private final TransferCommandFacade transferCommandFacade;

	@PostMapping
	@Operation(summary = "이체")
	public ResponseEntity<TransferResponse> transfer(
		@Parameter(description = "중복 이체 방지를 위한 키", example = "transfer-20240201-0001", required = true)
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@Valid @RequestBody TransferRequest request) {
		TransferResult result = transferCommandFacade.transfer(
			request.getFromAccountNo(),
			request.getToAccountNo(),
			request.getAmountWon(),
			idempotencyKey
		);
		return ResponseEntity.ok(TransferResponse.from(result));
	}
}
