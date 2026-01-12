package com.example.transfer.api.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.transfer.api.dto.response.FeeStatsResponse;
import com.example.transfer.domain.service.StatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "통계 API")
public class StatisticsController {

	private final StatisticsService statisticsService;

	@GetMapping("/fees")
	@Operation(summary = "수수료 합계 조회")
	public ResponseEntity<FeeStatsResponse> getFeeStats(
		@Parameter(description = "대상 계좌 ID, 미입력 시 전체 합계", example = "1001")
		@RequestParam(required = false) Long accountId,
		@Parameter(description = "집계 시작 시각", required = true)
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
		@Parameter(description = "집계 종료 시각", required = true)
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		Long total = statisticsService.getFeeTotal(accountId, from, to);
		return ResponseEntity.ok(FeeStatsResponse.of(accountId, from, to, total));
	}
}
