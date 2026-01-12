package com.example.transfer.api.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "FeeStatsResponse", description = "수수료 합계 통계")
public class FeeStatsResponse {

	@Schema(description = "대상 계좌 ID (전체 합계일 경우 null)", example = "1001")
	private Long accountId;

	@Schema(description = "집계 시작 시각", example = "2024-02-01T00:00:00")
	private LocalDateTime from;

	@Schema(description = "집계 종료 시각", example = "2024-02-29T23:59:59")
	private LocalDateTime to;

	@Schema(description = "수수료 합계(원)", example = "1200")
	private Long totalFeeWon;

	public static FeeStatsResponse of(Long accountId, LocalDateTime from, LocalDateTime to, Long totalFeeWon) {
		return FeeStatsResponse.builder()
			.accountId(accountId)
			.from(from)
			.to(to)
			.totalFeeWon(totalFeeWon)
			.build();
	}
}
