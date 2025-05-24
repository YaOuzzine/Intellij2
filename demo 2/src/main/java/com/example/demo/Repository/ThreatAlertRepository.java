package com.example.demo.Repository;

import com.example.demo.Entity.ThreatAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThreatAlertRepository extends JpaRepository<ThreatAlert, Long> {

    List<ThreatAlert> findByStatusOrderByCreatedAtDesc(String status);

    List<ThreatAlert> findBySeverityAndStatusOrderByCreatedAtDesc(String severity, String status);

    List<ThreatAlert> findBySourceIpAndStatusOrderByCreatedAtDesc(String sourceIp, String status);

    @Query("SELECT ta FROM ThreatAlert ta WHERE ta.status = 'OPEN' AND ta.severity IN ('HIGH', 'CRITICAL') ORDER BY ta.threatScore DESC, ta.createdAt DESC")
    List<ThreatAlert> findActiveCriticalAlerts();

    @Query("SELECT ta FROM ThreatAlert ta WHERE ta.createdAt >= :since ORDER BY ta.threatScore DESC")
    List<ThreatAlert> findRecentAlerts(@Param("since") LocalDateTime since);

    @Query("SELECT ta.severity, COUNT(ta) FROM ThreatAlert ta WHERE ta.createdAt >= :since GROUP BY ta.severity")
    List<Object[]> getAlertSeverityDistribution(@Param("since") LocalDateTime since);

    Optional<ThreatAlert> findBySourceIpAndTargetRouteAndStatus(String sourceIp, String targetRoute, String status);

    @Query("SELECT COUNT(ta) FROM ThreatAlert ta WHERE ta.status = 'OPEN'")
    Long countOpenAlerts();
}
