package com.example.transfer.infra.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EntityScan("com.example.transfer.domain.entity")
@EnableJpaRepositories("com.example.transfer.infra.repository")
public class JpaConfig {
}
