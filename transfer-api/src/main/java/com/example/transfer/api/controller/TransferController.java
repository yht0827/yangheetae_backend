package com.example.transfer.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.transfer.api.dto.request.TransferRequest;
import com.example.transfer.api.dto.response.TransferResponse;
import com.example.transfer.domain.dto.TransferResult;
import com.example.transfer.domain.service.TransferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer", description = "이체 API")
public class TransferController {

	private final TransferService transferService;

	@PostMapping
	@Operation(summary = "이체")
	public ResponseEntity<TransferResponse> transfer(
		@Valid @RequestBody TransferRequest request) {
		TransferResult result = transferService.transfer(
			request.getFromAccountId(),
			request.getToAccountId(),
			request.getAmountWon()
		);
		return ResponseEntity.ok(TransferResponse.from(result));
	}
}