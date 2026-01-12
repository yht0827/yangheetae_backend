package com.example.transfer.domain.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.transfer.domain.repository.TransactionEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

	private final TransactionEntryRepository transactionEntryRepository;

	public Long getFeeTotal(Long accountId, LocalDateTime from, LocalDateTime to) {
		return transactionEntryRepository.sumFees(accountId, from, to);
	}
}
