package com.example.transfer.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_usages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(DailyUsageId.class)
public class DailyUsage extends BaseTimeEntity {

	@Id
	private Long accountId;

	@Id
	private LocalDate usageDate;

	@Column(nullable = false)
	private Long withdrawalTotalWon = 0L;

	@Column(nullable = false)
	private Long transferTotalWon = 0L;

	public static DailyUsage create(Long accountId, LocalDate date) {
		validateAccountId(accountId);
		validateDate(date);

		DailyUsage usage = new DailyUsage();
		usage.accountId = accountId;
		usage.usageDate = date;
		usage.withdrawalTotalWon = 0L;
		usage.transferTotalWon = 0L;
		return usage;
	}

	public void addWithdrawal(Long amount) {
		validateAmount(amount);
		this.withdrawalTotalWon += amount;
	}

	public void addTransfer(Long amount) {
		validateAmount(amount);
		this.transferTotalWon += amount;
	}

	private static void validateAccountId(Long accountId) {
		if (accountId == null || accountId <= 0) {
			throw new IllegalArgumentException("유효하지 않은 계좌 ID입니다");
		}
	}

	private static void validateDate(LocalDate date) {
		if (date == null) {
			throw new IllegalArgumentException("날짜는 필수입니다");
		}
		if (date.isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("미래 날짜는 사용할 수 없습니다");
		}
	}

	private static void validateAmount(Long amount) {
		if (amount == null || amount <= 0) {
			throw new IllegalArgumentException("금액은 0보다 커야 합니다");
		}
	}
}