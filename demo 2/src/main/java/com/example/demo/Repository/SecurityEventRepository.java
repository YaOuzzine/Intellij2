package com.example.demo.Repository;

import com.example.demo.Entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    // Find events by time range
    List<SecurityEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Find events by route ID
    List<SecurityEvent> findByRouteIdAndTimestampBetween(String routeId, LocalDateTime start, LocalDateTime end);

    // Find events by client IP
    List<SecurityEvent> findByClientIpAndTimestampBetween(String clientIp, LocalDateTime start, LocalDateTime end);

    // Find events by event type
    List<SecurityEvent> findByEventTypeAndTimestampBetween(String eventType, LocalDateTime start, LocalDateTime end);

    // Find events by threat level
    List<SecurityEvent> findByThreatLevelAndTimestampBetween(String threatLevel, LocalDateTime start, LocalDateTime end);

    // Count events by route and time range
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.routeId = :routeId AND e.timestamp BETWEEN :start AND :end")
    Long countByRouteIdAndTimeRange(@Param("routeId") String routeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Count rejections by route and time range
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.routeId = :routeId AND e.eventType = 'REJECTION' AND e.timestamp BETWEEN :start AND :end")
    Long countRejectionsByRouteIdAndTimeRange(@Param("routeId") String routeId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Get top attacking IPs
    @Query("SELECT e.clientIp, COUNT(e) as count FROM SecurityEvent e WHERE e.eventType = 'REJECTION' AND e.timestamp >= :since GROUP BY e.clientIp ORDER BY count DESC")
    List<Object[]> findTopAttackingIPs(@Param("since") LocalDateTime since);

    // Get threat level distribution
    @Query("SELECT e.threatLevel, COUNT(e) FROM SecurityEvent e WHERE e.timestamp >= :since GROUP BY e.threatLevel")
    List<Object[]> getThreatLevelDistribution(@Param("since") LocalDateTime since);

    // Get hourly event counts for time series
    @Query("SELECT FUNCTION('DATE_TRUNC', 'hour', e.timestamp) as hour, COUNT(e) FROM SecurityEvent e WHERE e.timestamp >= :since GROUP BY hour ORDER BY hour")
    List<Object[]> getHourlyEventCounts(@Param("since") LocalDateTime since);

    // Get minute-level event counts for real-time monitoring
    @Query("SELECT FUNCTION('DATE_TRUNC', 'minute', e.timestamp) as minute, COUNT(e) FROM SecurityEvent e WHERE e.timestamp >= :since GROUP BY minute ORDER BY minute")
    List<Object[]> getMinutelyEventCounts(@Param("since") LocalDateTime since);

    // Find suspicious patterns (multiple rejections from same IP)
    @Query("SELECT e.clientIp, COUNT(e) as count FROM SecurityEvent e WHERE e.eventType = 'REJECTION' AND e.timestamp >= :since GROUP BY e.clientIp HAVING COUNT(e) >= :threshold ORDER BY count DESC")
    List<Object[]> findSuspiciousIPs(@Param("since") LocalDateTime since, @Param("threshold") Long threshold);

    // Average response time by route
    @Query("SELECT e.routeId, AVG(CAST(e.responseTimeMs AS double)) FROM SecurityEvent e WHERE e.responseTimeMs IS NOT NULL AND e.timestamp >= :since GROUP BY e.routeId")
    List<Object[]> getAverageResponseTimeByRoute(@Param("since") LocalDateTime since);

    // Delete old events for cleanup
    @Query("DELETE FROM SecurityEvent e WHERE e.timestamp < :cutoff")
    void deleteEventsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}