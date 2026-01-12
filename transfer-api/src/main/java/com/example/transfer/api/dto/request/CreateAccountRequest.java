package com.example.transfer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "소유자명은 필수입니다")
    private String ownerName;
}
