package com.example.transfer.infra.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;

@Component
public class DatabaseCleanUp implements InitializingBean {

	@PersistenceContext
	private EntityManager entityManager;

	private final List<String> tableNames = new ArrayList<>();

	@Override
	public void afterPropertiesSet() {
		entityManager.getMetamodel().getEntities().stream()
			.filter(entity -> entity.getJavaType().getAnnotation(Entity.class) != null)
			.map(entity -> entity.getJavaType().getAnnotation(Table.class))
			.filter(Objects::nonNull)
			.map(Table::name)
			.forEach(tableNames::add);
	}

	@Transactional
	public void truncateAllTables() {
		entityManager.flush();
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

		for (String table : tableNames) {
			entityManager.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
		}

		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
	}
}
