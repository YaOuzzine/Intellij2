package com.example.demo.Repository;

import com.example.demo.Entity.ThreatPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThreatPatternRepository extends JpaRepository<ThreatPattern, Long> {

    List<ThreatPattern> findByIsActiveTrue();

    List<ThreatPattern> findByPatternTypeAndIsActiveTrue(String patternType);

    List<ThreatPattern> findByThreatLevelAndIsActiveTrue(String threatLevel);

    @Query("SELECT tp FROM ThreatPattern tp WHERE tp.isActive = true AND tp.autoBlock = true")
    List<ThreatPattern> findActiveAutoBlockPatterns();

    @Query("SELECT tp FROM ThreatPattern tp WHERE tp.lastTriggered >= :since ORDER BY tp.triggerCount DESC")
    List<ThreatPattern> findRecentlyTriggeredPatterns(@Param("since") LocalDateTime since);

    @Query("SELECT tp FROM ThreatPattern tp WHERE tp.triggerCount > :threshold ORDER BY tp.triggerCount DESC")
    List<ThreatPattern> findHighVolumePatterns(@Param("threshold") Long threshold);
}