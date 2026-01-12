package com.example.transfer.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequest {

    @NotNull
    @Positive(message = "금액은 0보다 커야 합니다")
    private Long amountWon;
}
