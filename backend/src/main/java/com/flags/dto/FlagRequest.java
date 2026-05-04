package com.flags.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FlagRequest {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Flag key must be lowercase alphanumeric with hyphens")
    private String flagKey;

    @NotBlank
    private String name;

    private String description;

    private boolean enabled = false;

    @Min(0) @Max(100)
    private int rolloutPercentage = 0;

    private String targetedUserIds = "[]";

    private String environments = "{\"production\":false,\"staging\":false,\"development\":true}";

    private String tags = "[]";
}
