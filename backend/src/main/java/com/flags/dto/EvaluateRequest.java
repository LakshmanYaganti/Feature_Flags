package com.flags.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EvaluateRequest {
    @NotBlank
    private String flagKey;
    private String userId;
    private String environment = "production";
}
