package com.example.transfer.infra.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.domain.repository.AccountRepository;

import jakarta.persistence.LockModeType;

public interface AccountJpaRepository extends JpaRepository<Account, Long>, AccountRepository {

	@Override
	@Query("SELECT a FROM Account a WHERE a.id = :id AND a.status = 'ACTIVE'")
	Optional<Account> findActiveById(@Param("id") Long id);

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Account a WHERE a.id = :id AND a.status = 'ACTIVE'")
	Optional<Account> findByIdWithLock(@Param("id") Long id);
}
