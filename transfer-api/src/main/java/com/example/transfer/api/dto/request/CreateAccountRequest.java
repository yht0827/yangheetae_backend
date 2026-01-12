package com.example.transfer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(name = "CreateAccountRequest", description = "계좌 생성 요청 바디")
public class CreateAccountRequest {

	@NotBlank(message = "소유자명은 필수입니다")
	@Schema(description = "계좌 소유자 이름", example = "홍길동")
	private String ownerName;
}
