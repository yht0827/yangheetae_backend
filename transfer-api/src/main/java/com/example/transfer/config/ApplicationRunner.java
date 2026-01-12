package com.example.transfer.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationRunner implements CommandLineRunner {

	private final AccountRepository accountRepository;

	@Override
	public void run(String... args) {
		if (accountRepository.findById(1L).isPresent()) {
			log.info("Sample data already exists. Skipping initialization.");
			return;
		}

		Account account1 = Account.create("홍길동");
		account1.increaseBalance(1_000_000L);
		accountRepository.save(account1);

		Account account2 = Account.create("김철수");
		account2.increaseBalance(500_000L);
		accountRepository.save(account2);

		Account account3 = Account.create("이영희");
		account3.increaseBalance(2_000_000L);
		accountRepository.save(account3);

		log.info("Sample data initialized: 3 accounts created");
	}
}
