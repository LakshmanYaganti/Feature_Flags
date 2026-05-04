package com.flags.repository;

import com.flags.model.FlagEvaluationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface FlagEvaluationLogRepository extends JpaRepository<FlagEvaluationLog, UUID> {

    @Query("SELECT l FROM FlagEvaluationLog l WHERE l.flagKey = :key ORDER BY l.evaluatedAt DESC")
    List<FlagEvaluationLog> findRecentByFlagKey(@Param("key") String key, Pageable pageable);

    @Query("SELECT l.flagKey, COUNT(l), SUM(CASE WHEN l.result = true THEN 1 ELSE 0 END) FROM FlagEvaluationLog l GROUP BY l.flagKey")
    List<Object[]> getFlagStats();
}
