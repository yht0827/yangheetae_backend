package com.example.transfer.infra.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.transfer.TransferInfraTestApplication;

@ActiveProfiles("test")
@SpringBootTest(classes = TransferInfraTestApplication.class)
public abstract class IntegrationTestSupport {

	@Autowired
	private DatabaseCleanUp databaseCleanUp;

	@BeforeEach
	void cleanDatabase() {
		databaseCleanUp.truncateAllTables();
	}
}
