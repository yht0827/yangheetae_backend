package com.example.transfer.infra.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.transfer.domain.entity.Account;
import com.example.transfer.infra.config.JpaConfig;

@DataJpaTest
@Import(JpaConfig.class)
class AccountJpaRepositoryDataJpaTest {

	@Autowired
	private AccountJpaRepository accountJpaRepository;

	@Test
	@DisplayName("H2 기반 DataJpaTest에서 계좌 저장/조회가 가능하다")
	void saveAndFind() {
		Account account = Account.create("H2 사용자");
		Account saved = accountJpaRepository.saveAndFlush(account);

		assertThat(saved.getId()).isNotNull();
		assertThat(accountJpaRepository.findActiveById(saved.getId())).isPresent();
	}
}
