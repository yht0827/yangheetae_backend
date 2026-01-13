package com.example.transfer.domain.repository;

import java.util.Optional;

import com.example.transfer.domain.entity.Account;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(Long id);

	Optional<Account> findActiveById(Long id);

	Optional<Account> findByIdWithLock(Long id);

	Optional<Account> findByAccountNo(String accountNo);

	Optional<Account> findActiveByAccountNo(String accountNo);
}
