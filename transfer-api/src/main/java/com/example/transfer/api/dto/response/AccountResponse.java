package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;

import com.example.transfer.domain.entity.Account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "AccountResponse", description = "계좌 정보")
public class AccountResponse {

	@Schema(description = "계좌 ID", example = "1001")
	private Long id;

	@Schema(description = "계좌 소유자 이름", example = "홍길동")
	private String ownerName;

	@Schema(description = "계좌 잔액(원)", example = "150000")
	private Long balanceWon;

	@Schema(description = "계좌 상태", example = "ACTIVE")
	private String status;

	@Schema(description = "계좌 생성 시각", example = "2024-02-01T10:15:30")
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
