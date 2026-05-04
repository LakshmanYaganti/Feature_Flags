package com.flags.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvaluateResponse {
    private String flagKey;
    private boolean enabled;
    private String reason;
    private String userId;
    private String environment;
}
