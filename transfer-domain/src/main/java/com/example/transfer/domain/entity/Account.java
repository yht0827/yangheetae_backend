package com.example.transfer.domain.entity;

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

	@Column(nullable = false, length = 50)
	private String ownerName;

	@Column(nullable = false)
	private Long balanceWon = 0L;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountStatus status = AccountStatus.ACTIVE;

	public static Account create(String ownerName) {
		Account account = new Account();
		account.ownerName = ownerName;
		account.balanceWon = 0L;
		account.status = AccountStatus.ACTIVE;
		return account;
	}

	public void increaseBalance(Long amount) {
		this.balanceWon += amount;
	}

	public void decreaseBalance(Long amount) {
		if (this.balanceWon < amount) {
			throw new InsufficientBalanceException("잔액이 충분하지 않습니다.");
		}
		this.balanceWon -= amount;
	}

	public void delete() {
		this.status = AccountStatus.DELETED;
	}

	public boolean isActive() {
		return this.status == AccountStatus.ACTIVE;
	}
}