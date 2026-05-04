package com.flags.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flag_evaluation_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlagEvaluationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flag_key", nullable = false, length = 100)
    private String flagKey;

    @Column(name = "user_id")
    private String userId;

    @Column(length = 50)
    private String environment;

    @Column(nullable = false)
    private boolean result;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist protected void onCreate() { evaluatedAt = LocalDateTime.now(); }
}
