package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;

import com.example.transfer.domain.entity.Account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

	private Long id;
	private String ownerName;
	private Long balanceWon;
	private String status;
	private LocalDateTime createdAt;

	public static AccountResponse from(Account account) {
		return AccountResponse.builder()
			.id(account.getId())
			.ownerName(account.getOwnerName())
			.balanceWon(account.getBalanceWon())
			.status(account.getStatus().name())
			.createdAt(account.getCreatedAt())
			.build();
	}
}
