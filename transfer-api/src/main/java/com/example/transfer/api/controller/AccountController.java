package com.example.transfer.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.transfer.api.dto.request.CreateAccountRequest;
import com.example.transfer.api.dto.request.DepositRequest;
import com.example.transfer.api.dto.request.WithdrawRequest;
import com.example.transfer.api.dto.response.AccountResponse;
import com.example.transfer.api.dto.response.TransactionResponse;
import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.service.AccountCommandService;
import com.example.transfer.domain.service.AccountQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Account", description = "계좌 관리 API")
public class AccountController {

	private final AccountCommandService accountCommandService;
	private final AccountQueryService accountQueryService;

	@PostMapping
	@Operation(summary = "계좌 등록")
	public ResponseEntity<AccountResponse> createAccount(
		@Valid @RequestBody CreateAccountRequest request
	) {
		Account account = accountCommandService.createAccount(request.getOwnerName());
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(AccountResponse.from(account));
	}

	@GetMapping("/{id}")
	@Operation(summary = "계좌 조회")
	public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
		Account account = accountQueryService.getAccount(id);
		return ResponseEntity.ok(AccountResponse.from(account));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "계좌 삭제")
	public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
		accountCommandService.deleteAccount(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/deposit")
	@Operation(summary = "입금")
	public ResponseEntity<AccountResponse> deposit(
		@PathVariable Long id,
		@Valid @RequestBody DepositRequest request
	) {
		Account account = accountCommandService.deposit(id, request.getAmountWon());
		return ResponseEntity.ok(AccountResponse.from(account));
	}

	@PostMapping("/{id}/withdraw")
	@Operation(summary = "출금")
	public ResponseEntity<AccountResponse> withdraw(
		@PathVariable Long id,
		@Valid @RequestBody WithdrawRequest request
	) {
		Account account = accountCommandService.withdraw(id, request.getAmountWon());
		return ResponseEntity.ok(AccountResponse.from(account));
	}

	@GetMapping("/{id}/transactions")
	@Operation(summary = "거래내역 조회")
	public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id) {
		return ResponseEntity.ok(
			accountQueryService.getTransactions(id).stream()
				.map(TransactionResponse::from)
				.toList()
		);
	}
}
