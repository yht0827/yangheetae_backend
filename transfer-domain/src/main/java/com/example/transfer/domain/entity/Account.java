package com.example.transfer.domain.entity;

import java.util.concurrent.ThreadLocalRandom;

import com.example.transfer.domain.exception.InsufficientBalanceException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String accountNo;

	@Column(nullable = false, length = 50)
	private String ownerName;

	@Column(nullable = false)
	private Long balanceWon = 0L;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountStatus status = AccountStatus.ACTIVE;

	public static Account create(String ownerName) {
		validateOwnerName(ownerName);

		Account account = new Account();
		account.accountNo = generateAccountNo();
		account.ownerName = ownerName;
		account.balanceWon = 0L;
		account.status = AccountStatus.ACTIVE;
		return account;
	}

	private static String generateAccountNo() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int part1 = random.nextInt(100, 1000);       // 3자리
		int part2 = random.nextInt(1000, 10000);     // 4자리
		int part3 = random.nextInt(100000, 1000000); // 6자리
		return String.format("%03d-%04d-%06d", part1, part2, part3);
	}

	public void increaseBalance(Long amount) {
		validateAmount(amount);
		this.balanceWon += amount;
	}

	public void decreaseBalance(Long amount) {
		validateAmount(amount);
		if (this.balanceWon < amount) {
			throw new InsufficientBalanceException("잔액이 부족합니다");
		}
		this.balanceWon -= amount;
	}

	private static void validateOwnerName(String ownerName) {
		if (ownerName == null || ownerName.isBlank()) {
			throw new IllegalArgumentException("소유자명은 필수입니다");
		}
		if (ownerName.length() > 50) {
			throw new IllegalArgumentException("소유자명은 50자를 초과할 수 없습니다");
		}
	}

	private static void validateAmount(Long amount) {
		if (amount == null || amount <= 0) {
			throw new IllegalArgumentException("금액은 0보다 커야 합니다");
		}
	}

	public void delete() {
		if (this.status == AccountStatus.DELETED) {
			throw new IllegalStateException("이미 삭제된 계좌입니다");
		}
		this.status = AccountStatus.DELETED;
	}

	public boolean isActive() {
		return this.status == AccountStatus.ACTIVE;
	}
}