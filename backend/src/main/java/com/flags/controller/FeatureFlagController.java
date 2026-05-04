package com.flags.controller;

import com.flags.dto.EvaluateRequest;
import com.flags.dto.EvaluateResponse;
import com.flags.dto.FlagRequest;
import com.flags.model.FeatureFlag;
import com.flags.repository.FlagEvaluationLogRepository;
import com.flags.service.FeatureFlagService;
import com.flags.service.FlagEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService flagService;
    private final FlagEvaluationService evaluationService;
    private final FlagEvaluationLogRepository logRepository;

    @GetMapping("/flags")
    public ResponseEntity<List<FeatureFlag>> getAllFlags() {
        return ResponseEntity.ok(flagService.getAllFlags());
    }

    @GetMapping("/flags/{key}")
    public ResponseEntity<FeatureFlag> getFlagByKey(@PathVariable String key) {
        return ResponseEntity.ok(flagService.getFlagByKey(key));
    }

    @PostMapping("/flags")
    public ResponseEntity<FeatureFlag> createFlag(@Valid @RequestBody FlagRequest request) {
        return ResponseEntity.ok(flagService.createFlag(request));
    }

    @PutMapping("/flags/{id}")
    public ResponseEntity<FeatureFlag> updateFlag(@PathVariable UUID id, @Valid @RequestBody FlagRequest request) {
        return ResponseEntity.ok(flagService.updateFlag(id, request));
    }

    @PatchMapping("/flags/{id}/toggle")
    public ResponseEntity<FeatureFlag> toggleFlag(@PathVariable UUID id) {
        return ResponseEntity.ok(flagService.toggleFlag(id));
    }

    @DeleteMapping("/flags/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID id) {
        flagService.deleteFlag(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flags/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(@Valid @RequestBody EvaluateRequest request) {
        return ResponseEntity.ok(evaluationService.evaluate(request));
    }

    @GetMapping("/flags/{key}/logs")
    public ResponseEntity<List<?>> getFlagLogs(@PathVariable String key,
                                                @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(logRepository.findRecentByFlagKey(key, PageRequest.of(0, limit)));
    }

    @GetMapping("/stats")
    public ResponseEntity<List<Map<String, Object>>> getStats() {
        List<Map<String, Object>> stats = logRepository.getFlagStats().stream()
                .map(row -> Map.of(
                        "flagKey", row[0],
                        "totalEvaluations", row[1],
                        "trueCount", row[2]
                )).toList();
        return ResponseEntity.ok(stats);
    }
}
