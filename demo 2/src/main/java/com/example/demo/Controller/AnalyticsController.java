// demo 2/src/main/java/com/example/demo/Controller/AnalyticsController.java
package com.example.demo.Controller;

import com.example.demo.Service.AnalyticsService;
import com.example.demo.Service.AISecurityService;
import com.example.demo.Service.ThreatAnalysisService;
import com.example.demo.Service.AlertingService;
import com.example.demo.Service.ComplianceReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:5173")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private final AISecurityService aiSecurityService;
    private final ThreatAnalysisService threatAnalysisService;
    private final AlertingService alertingService;
    private final ComplianceReportingService complianceReportingService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService,
                               AISecurityService aiSecurityService,
                               ThreatAnalysisService threatAnalysisService,
                               AlertingService alertingService,
                               ComplianceReportingService complianceReportingService) {
        this.analyticsService = analyticsService;
        this.aiSecurityService = aiSecurityService;
        this.threatAnalysisService = threatAnalysisService;
        this.alertingService = alertingService;
        this.complianceReportingService = complianceReportingService;
    }

    /**
     * Enhanced dashboard with AI insights - Primary endpoint for frontend
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getEnhancedDashboard() {
        try {
            log.info("Fetching enhanced analytics dashboard with AI insights");
            Map<String, Object> dashboardData = analyticsService.getEnhancedDashboardData();
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error fetching enhanced dashboard: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch dashboard data");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * AI Security Insights - Core AI analytics
     */
    @GetMapping("/ai/security-insights")
    public ResponseEntity<Map<String, Object>> getAISecurityInsights() {
        try {
            log.info("Fetching AI security insights");
            Map<String, Object> insights = aiSecurityService.generateSecurityInsights();
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error fetching AI security insights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate AI security insights", "message", e.getMessage()));
        }
    }

    /**
     * Behavioral analysis for specific IP or route
     */
    @GetMapping("/ai/behavioral-analysis")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> getBehavioralAnalysis(
            @RequestParam String identifier,
            @RequestParam(defaultValue = "ip") String type) {
        try {
            log.info("Performing behavioral analysis for {}: {}", type, identifier);

            CompletableFuture<Map<String, Object>> analysis;
            if ("ip".equals(type)) {
                analysis = aiSecurityService.analyzeBehavioralPattern(identifier);
            } else if ("route".equals(type)) {
                // For route analysis, we'll use the analytics service
                Map<String, Object> routeAnalysis = analyticsService.getRouteAnalyticsWithAI(identifier);
                analysis = CompletableFuture.completedFuture(routeAnalysis);
            } else {
                return ResponseEntity.badRequest()
                        .body(CompletableFuture.completedFuture(Map.of("error", "Invalid analysis type. Use 'ip' or 'route'")));
            }

            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error in behavioral analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CompletableFuture.completedFuture(Map.of("error", "Behavioral analysis failed", "message", e.getMessage())));
        }
    }

    /**
     * Threat predictions using AI
     */
    @GetMapping("/ai/threat-predictions")
    public ResponseEntity<Map<String, Object>> getThreatPredictions(
            @RequestParam(defaultValue = "6") int hoursAhead) {
        try {
            log.info("Generating threat predictions for {} hours ahead", hoursAhead);
            Map<String, Object> predictions = aiSecurityService.predictSecurityThreats(hoursAhead);
            return ResponseEntity.ok(predictions);
        } catch (Exception e) {
            log.error("Error generating threat predictions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate threat predictions", "message", e.getMessage()));
        }
    }

    /**
     * Real-time threat intelligence
     */
    @GetMapping("/threat-intelligence/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeThreatIntelligence() {
        try {
            log.info("Fetching real-time threat intelligence");
            Map<String, Object> intelligence = analyticsService.getRealTimeThreatIntelligence();
            return ResponseEntity.ok(intelligence);
        } catch (Exception e) {
            log.error("Error fetching real-time threat intelligence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch threat intelligence", "message", e.getMessage()));
        }
    }

    /**
     * Threat landscape analysis
     */
    @GetMapping("/threat-analysis/landscape")
    public ResponseEntity<Map<String, Object>> getThreatLandscape() {
        try {
            log.info("Fetching threat landscape analysis");
            Map<String, Object> landscape = threatAnalysisService.getThreatLandscape();
            return ResponseEntity.ok(landscape);
        } catch (Exception e) {
            log.error("Error fetching threat landscape: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch threat landscape", "message", e.getMessage()));
        }
    }

    /**
     * Security recommendations from threat analysis
     */
    @GetMapping("/recommendations/security")
    public ResponseEntity<Object> getSecurityRecommendations() {
        try {
            log.info("Fetching security recommendations");
            var recommendations = threatAnalysisService.getSecurityRecommendations();
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error fetching security recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch security recommendations", "message", e.getMessage()));
        }
    }

    /**
     * Active alerts from alerting service
     */
    @GetMapping("/alerts/active")
    public ResponseEntity<Object> getActiveAlerts() {
        try {
            log.info("Fetching active security alerts");
            var alerts = alertingService.getActiveAlerts();
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching active alerts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch active alerts", "message", e.getMessage()));
        }
    }

    /**
     * Alert statistics and metrics
     */
    @GetMapping("/alerts/statistics")
    public ResponseEntity<Map<String, Object>> getAlertStatistics() {
        try {
            log.info("Fetching alert statistics");
            Map<String, Object> statistics = alertingService.getAlertStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error fetching alert statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch alert statistics", "message", e.getMessage()));
        }
    }

    /**
     * Update alert status
     */
    @PutMapping("/alerts/{alertId}/status")
    public ResponseEntity<Object> updateAlertStatus(
            @PathVariable Long alertId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        try {
            log.info("Updating alert {} status to: {}", alertId, status);
            var updatedAlert = alertingService.updateAlertStatus(alertId, status, notes);
            return ResponseEntity.ok(updatedAlert);
        } catch (Exception e) {
            log.error("Error updating alert status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update alert status", "message", e.getMessage()));
        }
    }

    /**
     * Escalation recommendations
     */
    @GetMapping("/alerts/escalation-recommendations")
    public ResponseEntity<Object> getEscalationRecommendations() {
        try {
            log.info("Fetching escalation recommendations");
            var recommendations = alertingService.getEscalationRecommendations();
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            log.error("Error fetching escalation recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch escalation recommendations", "message", e.getMessage()));
        }
    }

    /**
     * Compliance dashboard
     */
    @GetMapping("/compliance/dashboard")
    public ResponseEntity<Map<String, Object>> getComplianceDashboard() {
        try {
            log.info("Fetching compliance dashboard");
            Map<String, Object> dashboard = complianceReportingService.getComplianceDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error fetching compliance dashboard: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch compliance dashboard", "message", e.getMessage()));
        }
    }

    /**
     * Generate compliance report for specific framework
     */
    @GetMapping("/compliance/report/{framework}")
    public ResponseEntity<Map<String, Object>> generateComplianceReport(
            @PathVariable String framework,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            log.info("Generating compliance report for {} from {} to {}", framework, startDate, endDate);
            Map<String, Object> report = complianceReportingService.generateComplianceReport(framework, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error generating compliance report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate compliance report", "message", e.getMessage()));
        }
    }

    /**
     * Assess specific compliance control
     */
    @GetMapping("/compliance/{framework}/control/{controlId}")
    public ResponseEntity<Map<String, Object>> assessComplianceControl(
            @PathVariable String framework,
            @PathVariable String controlId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            log.info("Assessing compliance control {} for framework {}", controlId, framework);
            Map<String, Object> assessment = complianceReportingService.assessComplianceControl(framework, controlId, startDate, endDate);
            return ResponseEntity.ok(assessment);
        } catch (Exception e) {
            log.error("Error assessing compliance control: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to assess compliance control", "message", e.getMessage()));
        }
    }

    /**
     * Route-specific analytics with AI enhancement
     */
    @GetMapping("/routes/{routeId}")
    public ResponseEntity<Map<String, Object>> getRouteAnalytics(@PathVariable String routeId) {
        try {
            log.info("Fetching AI-enhanced analytics for route: {}", routeId);
            Map<String, Object> analytics = analyticsService.getRouteAnalyticsWithAI(routeId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error fetching route analytics for {}: {}", routeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch route analytics", "message", e.getMessage()));
        }
    }

    /**
     * Comprehensive security report with AI insights
     */
    @GetMapping("/reports/security")
    public Mono<ResponseEntity<Map<String, Object>>> generateSecurityReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generating comprehensive security report from {} to {}", startDate, endDate);

        return analyticsService.generateSecurityReport(startDate, endDate)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating security report: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to generate security report", "message", e.getMessage())));
                });
    }

    /**
     * Trigger automated security response
     */
    @PostMapping("/security/automated-response")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> triggerAutomatedResponse(
            @RequestParam String routeId,
            @RequestParam String threatType,
            @RequestParam double threatScore) {
        try {
            log.info("Triggering automated security response for route: {} - Threat: {} - Score: {}",
                    routeId, threatType, threatScore);

            CompletableFuture<Map<String, Object>> response = analyticsService
                    .triggerAutomatedSecurityResponse(routeId, threatType, threatScore);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering automated response: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CompletableFuture.completedFuture(Map.of("error", "Failed to trigger automated response", "message", e.getMessage())));
        }
    }

    /**
     * Health check endpoint for analytics service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getAnalyticsHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "HEALTHY");
            health.put("analyticsService", "OPERATIONAL");
            health.put("aiServices", "OPERATIONAL");
            health.put("lastUpdated", LocalDateTime.now());
            health.put("version", "AI-Enhanced v2.0");

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error checking analytics health: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "UNHEALTHY", "error", e.getMessage()));
        }
    }

    /**
     * Get system metrics - Legacy endpoint for backward compatibility
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getLegacyMetrics() {
        try {
            log.info("Fetching legacy metrics (backward compatibility)");

            // This maintains compatibility with any existing frontend code
            Map<String, Object> legacyMetrics = new HashMap<>();

            // Enhanced dashboard data mapped to legacy format
            Map<String, Object> enhancedData = analyticsService.getEnhancedDashboardData();

            // Extract global metrics for legacy compatibility
            if (enhancedData.containsKey("globalMetrics")) {
                legacyMetrics.putAll((Map<String, Object>) enhancedData.get("globalMetrics"));
            }

            // Add AI-enhanced insights in a backward-compatible way
            legacyMetrics.put("enhancedAnalytics", true);
            legacyMetrics.put("aiInsights", enhancedData.get("aiSecurityInsights"));
            legacyMetrics.put("routes", enhancedData.get("routeAnalytics"));

            return ResponseEntity.ok(legacyMetrics);
        } catch (Exception e) {
            log.error("Error fetching legacy metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch metrics", "message", e.getMessage()));
        }
    }

    /**
     * Analytics configuration endpoint
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAnalyticsConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("aiEnabled", true);
            config.put("threatAnalysisEnabled", true);
            config.put("complianceEnabled", true);
            config.put("realTimeAlertsEnabled", true);
            config.put("behavioralAnalysisEnabled", true);
            config.put("version", "2.0");
            config.put("features", Map.of(
                    "aiSecurityInsights", true,
                    "threatPrediction", true,
                    "behavioralAnalysis", true,
                    "complianceReporting", true,
                    "realTimeAlerts", true,
                    "automatedResponse", true
            ));

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error fetching analytics config: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch configuration"));
        }
    }

    /**
     * Export analytics data for external analysis
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportAnalyticsData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "json") String format) {
        try {
            log.info("Exporting analytics data from {} to {} in {} format", startDate, endDate, format);

            Map<String, Object> exportData = new HashMap<>();

            // Generate comprehensive export data
            exportData.put("securityReport", analyticsService.generateSecurityReport(startDate, endDate));
            exportData.put("complianceReport", complianceReportingService.generateComplianceReport("SOC2", startDate, endDate));
            exportData.put("threatAnalysis", threatAnalysisService.getThreatLandscape());
            exportData.put("aiInsights", aiSecurityService.generateSecurityInsights());

            exportData.put("exportMetadata", Map.of(
                    "exportDate", LocalDateTime.now(),
                    "startDate", startDate,
                    "endDate", endDate,
                    "format", format,
                    "version", "2.0"
            ));

            return ResponseEntity.ok(exportData);
        } catch (Exception e) {
            log.error("Error exporting analytics data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to export analytics data", "message", e.getMessage()));
        }
    }
}