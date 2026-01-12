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
public class DailyUsage {

	@Id
	private Long accountId;

	@Id
	private LocalDate usageDate;

	@Column(nullable = false)
	private Long withdrawalTotalWon = 0L;

	@Column(nullable = false)
	private Long transferTotalWon = 0L;

	public static DailyUsage create(Long accountId, LocalDate date) {
		DailyUsage usage = new DailyUsage();
		usage.accountId = accountId;
		usage.usageDate = date;
		usage.withdrawalTotalWon = 0L;
		usage.transferTotalWon = 0L;
		return usage;
	}

	public void addWithdrawal(Long amount) {
		this.withdrawalTotalWon += amount;
	}

	public void addTransfer(Long amount) {
		this.transferTotalWon += amount;
	}
}