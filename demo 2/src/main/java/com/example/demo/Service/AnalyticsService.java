// demo 2/src/main/java/com/example/demo/Service/AnalyticsService.java
package com.example.demo.Service;

import com.example.demo.Entity.SecurityEvent;
import com.example.demo.Entity.ThreatAlert;
import com.example.demo.Filter.RequestCountFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    // Injected AI Services
    private final AISecurityService aiSecurityService;
    private final ThreatAnalysisService threatAnalysisService;
    private final AlertingService alertingService;
    private final SecurityEventService securityEventService;
    private final ComplianceReportingService complianceReportingService;

    // Real-time metrics storage
    private final Map<String, RouteMetrics> routeMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> routeRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> routeRejectionCounts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> routeResponseTimes = new ConcurrentHashMap<>();

    // AI-enhanced metrics
    private final Map<String, AIMetrics> aiMetricsCache = new ConcurrentHashMap<>();
    private LocalDateTime lastAIAnalysis = LocalDateTime.now();

    // Performance tracking
    private final Map<String, Double> routeThreatScores = new ConcurrentHashMap<>();
    private final Map<String, String> routeSecurityStatus = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnalyticsService(AISecurityService aiSecurityService,
                            ThreatAnalysisService threatAnalysisService,
                            AlertingService alertingService,
                            SecurityEventService securityEventService,
                            ComplianceReportingService complianceReportingService) {
        this.aiSecurityService = aiSecurityService;
        this.threatAnalysisService = threatAnalysisService;
        this.alertingService = alertingService;
        this.securityEventService = securityEventService;
        this.complianceReportingService = complianceReportingService;
    }

    @PostConstruct
    public void initializeAnalytics() {
        log.info("Initializing Enhanced Analytics Service with AI Integration...");
        // Initialize any default metrics or load historical data
        initializeRouteMetrics();
        log.info("Analytics Service initialized successfully");
    }

    /**
     * Record a request for analytics (called by filters)
     */
    public void recordRequest(String routeId) {
        if (routeId == null) return;

        routeRequestCounts.computeIfAbsent(routeId, k -> new AtomicLong(0)).incrementAndGet();

        // Update route metrics
        RouteMetrics metrics = routeMetricsMap.computeIfAbsent(routeId, k -> new RouteMetrics(routeId));
        metrics.incrementRequests();

        // Record security event for AI analysis
        recordSecurityEventAsync(routeId, "REQUEST");

        log.trace("Recorded request for route: {}", routeId);
    }

    /**
     * Record a rejection for analytics
     */
    public void recordRejection(String routeId, String reason) {
        if (routeId == null) return;

        routeRejectionCounts.computeIfAbsent(routeId, k -> new AtomicLong(0)).incrementAndGet();

        // Update route metrics
        RouteMetrics metrics = routeMetricsMap.computeIfAbsent(routeId, k -> new RouteMetrics(routeId));
        metrics.incrementRejections();
        metrics.addRejectionReason(reason);

        // Record security event for AI analysis
        recordSecurityEventAsync(routeId, "REJECTION", reason);

        log.debug("Recorded rejection for route: {} - Reason: {}", routeId, reason);
    }

    /**
     * Record response time for analytics
     */
    public void recordResponseTime(String routeId, long responseTimeMs) {
        if (routeId == null) return;

        RouteMetrics metrics = routeMetricsMap.computeIfAbsent(routeId, k -> new RouteMetrics(routeId));
        metrics.addResponseTime(responseTimeMs);

        // Keep sliding window of response times for trend analysis
        routeResponseTimes.computeIfAbsent(routeId, k -> new ArrayList<>()).add(responseTimeMs);

        // Trigger AI analysis if response time is unusually high
        if (responseTimeMs > 5000) {
            triggerAnomalyAnalysis(routeId, "HIGH_RESPONSE_TIME", responseTimeMs);
        }

        log.trace("Recorded response time for route: {} - {}ms", routeId, responseTimeMs);
    }

    /**
     * Get comprehensive analytics dashboard data with AI insights
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEnhancedDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        try {
            log.debug("Generating enhanced dashboard data with AI insights...");

            // Basic metrics (from RequestCountFilter)
            RequestCountFilter.MinuteMetrics minuteMetrics = RequestCountFilter.getMinuteMetrics();

            dashboard.put("globalMetrics", Map.of(
                    "totalRequests", RequestCountFilter.getTotalRequestCount(),
                    "totalRejections", RequestCountFilter.getTotalRejectedCount(),
                    "currentMinuteRequests", minuteMetrics.getRequestsCurrentMinute(),
                    "previousMinuteRequests", minuteMetrics.getRequestsPreviousMinute(),
                    "currentMinuteRejections", minuteMetrics.getRejectedCurrentMinute(),
                    "previousMinuteRejections", minuteMetrics.getRejectedPreviousMinute()
            ));

            // Route-specific metrics with AI enhancement
            List<Map<String, Object>> routeAnalytics = getRouteAnalytics();
            dashboard.put("routeAnalytics", routeAnalytics);

            // AI Security Insights
            Map<String, Object> aiInsights = aiSecurityService.generateSecurityInsights();
            dashboard.put("aiSecurityInsights", aiInsights);

            // Threat Analysis
            Map<String, Object> threatLandscape = threatAnalysisService.getThreatLandscape();
            dashboard.put("threatLandscape", threatLandscape);

            // Active Alerts
            List<Map<String, Object>> activeAlerts = alertingService.getActiveAlerts();
            dashboard.put("activeAlerts", activeAlerts);

            // Security Recommendations
            List<Map<String, Object>> securityRecommendations = threatAnalysisService.getSecurityRecommendations();
            dashboard.put("securityRecommendations", securityRecommendations);

            // Compliance Dashboard
            Map<String, Object> complianceStatus = complianceReportingService.getComplianceDashboard();
            dashboard.put("complianceStatus", complianceStatus);

            // Real-time Security Health Score
            double securityHealthScore = calculateOverallSecurityHealth();
            dashboard.put("securityHealthScore", securityHealthScore);

            // Performance trends
            Map<String, Object> performanceTrends = calculatePerformanceTrends();
            dashboard.put("performanceTrends", performanceTrends);

            // AI-powered threat predictions
            Map<String, Object> threatPredictions = aiSecurityService.predictSecurityThreats(6);
            dashboard.put("threatPredictions", threatPredictions);

            dashboard.put("lastUpdated", LocalDateTime.now());
            dashboard.put("analyticsVersion", "AI-Enhanced v2.0");

            log.info("Enhanced dashboard data generated successfully with {} route analytics", routeAnalytics.size());

        } catch (Exception e) {
            log.error("Error generating enhanced dashboard data: {}", e.getMessage(), e);
            dashboard.put("error", "Failed to generate dashboard data");
            dashboard.put("errorDetails", e.getMessage());
        }

        return dashboard;
    }

    /**
     * Get AI-enhanced analytics for a specific route
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRouteAnalyticsWithAI(String routeId) {
        Map<String, Object> analytics = new HashMap<>();

        try {
            RouteMetrics metrics = routeMetricsMap.get(routeId);
            if (metrics == null) {
                analytics.put("error", "Route not found: " + routeId);
                return analytics;
            }

            // Basic metrics
            analytics.put("routeId", routeId);
            analytics.put("totalRequests", metrics.getTotalRequests());
            analytics.put("totalRejections", metrics.getTotalRejections());
            analytics.put("rejectionRate", metrics.getRejectionRate());
            analytics.put("averageResponseTime", metrics.getAverageResponseTime());
            analytics.put("rejectionReasons", metrics.getRejectionReasons());

            // AI-enhanced analysis
            String clientIp = "route-analysis"; // Placeholder for route-level analysis
            CompletableFuture<Map<String, Object>> behavioralAnalysis =
                    aiSecurityService.analyzeBehavioralPattern(routeId + "_route");

            try {
                Map<String, Object> aiAnalysis = behavioralAnalysis.get();
                analytics.put("aiAnalysis", aiAnalysis);
            } catch (Exception e) {
                log.warn("AI analysis unavailable for route {}: {}", routeId, e.getMessage());
                analytics.put("aiAnalysis", Map.of("status", "UNAVAILABLE"));
            }

            // Threat score and security status
            double threatScore = routeThreatScores.getOrDefault(routeId, 0.0);
            String securityStatus = routeSecurityStatus.getOrDefault(routeId, "UNKNOWN");

            analytics.put("threatScore", threatScore);
            analytics.put("securityStatus", securityStatus);

            // Performance trends for this route
            analytics.put("performanceTrend", calculateRouteTrend(routeId));

            // Security recommendations for this route
            analytics.put("recommendations", generateRouteRecommendations(metrics, threatScore));

        } catch (Exception e) {
            log.error("Error generating route analytics for {}: {}", routeId, e.getMessage(), e);
            analytics.put("error", e.getMessage());
        }

        return analytics;
    }

    /**
     * Get real-time threat intelligence
     */
    public Map<String, Object> getRealTimeThreatIntelligence() {
        Map<String, Object> intelligence = new HashMap<>();

        try {
            // Active threats from alerting service
            List<Map<String, Object>> activeThreats = alertingService.getActiveAlerts();
            intelligence.put("activeThreats", activeThreats);

            // AI threat predictions
            Map<String, Object> predictions = aiSecurityService.predictSecurityThreats(1);
            intelligence.put("threatPredictions", predictions);

            // Recent attack patterns
            LocalDateTime since = LocalDateTime.now().minusHours(1);
            Map<String, Object> recentPatterns = threatAnalysisService.getThreatLandscape();
            intelligence.put("recentPatterns", recentPatterns);

            // Real-time security metrics
            intelligence.put("realTimeMetrics", Map.of(
                    "currentThreatLevel", calculateCurrentThreatLevel(),
                    "attacksInLastHour", getAttacksInLastHour(),
                    "topAttackVectors", getTopAttackVectors(),
                    "geographicThreats", getGeographicThreats()
            ));

            intelligence.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error generating real-time threat intelligence: {}", e.getMessage(), e);
            intelligence.put("error", e.getMessage());
        }

        return intelligence;
    }

    /**
     * Trigger automated security response based on AI analysis
     */
    @Async
    public CompletableFuture<Map<String, Object>> triggerAutomatedSecurityResponse(String routeId, String threatType, double threatScore) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Triggering automated security response for route: {} - Threat: {} - Score: {}",
                    routeId, threatType, threatScore);

            // Update threat score
            routeThreatScores.put(routeId, threatScore);

            // Determine security status based on threat score
            String securityStatus = determineSecurityStatus(threatScore);
            routeSecurityStatus.put(routeId, securityStatus);

            // Generate automated recommendations
            List<String> recommendations = generateAutomatedRecommendations(threatType, threatScore);

            // Create alert if threat score is high
            if (threatScore > 0.7) {
                // This would trigger the alerting service
                log.warn("High threat score detected for route {}: {}", routeId, threatScore);
            }

            response.put("routeId", routeId);
            response.put("threatType", threatType);
            response.put("threatScore", threatScore);
            response.put("securityStatus", securityStatus);
            response.put("recommendations", recommendations);
            response.put("responseTimestamp", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error in automated security response: {}", e.getMessage(), e);
            response.put("error", e.getMessage());
        }

        return CompletableFuture.completedFuture(response);
    }

    /**
     * Record a request with full context for enhanced analytics
     */
    public void recordRequestWithContext(String routeId, String clientIp, String requestPath, String method, String userAgent) {
        if (routeId == null) return;

        // Update in-memory metrics
        recordRequest(routeId);

        // Create detailed security event with full context
        SecurityEvent event = new SecurityEvent("REQUEST", routeId, clientIp);
        event.setRequestPath(requestPath);
        event.setRequestMethod(method);
        event.setUserAgent(userAgent);
        event.setTimestamp(LocalDateTime.now());

        // Record for AI analysis
        securityEventService.recordEventAsync(event);

        log.trace("Recorded request with context for route: {} from IP: {}", routeId, clientIp);
    }

    /**
     * Record a rejection with full context for enhanced analytics
     */
    public void recordRejectionWithContext(String routeId, String clientIp, String requestPath, String method,
                                           String userAgent, String rejectionReason, Integer statusCode) {
        if (routeId == null) return;

        // Update in-memory metrics
        recordRejection(routeId, rejectionReason);

        // Create detailed security event with full context
        SecurityEvent event = new SecurityEvent("REJECTION", routeId, clientIp);
        event.setRequestPath(requestPath);
        event.setRequestMethod(method);
        event.setUserAgent(userAgent);
        event.setRejectionReason(rejectionReason);
        event.setResponseStatus(statusCode);
        event.setTimestamp(LocalDateTime.now());

        // Record for AI analysis - this will automatically trigger threat analysis
        securityEventService.recordEventAsync(event);

        log.debug("Recorded rejection with context for route: {} from IP: {} - Reason: {}", routeId, clientIp, rejectionReason);
    }

    /**
     * Generate comprehensive security report with AI insights
     */
    @Transactional(readOnly = true)
    public Mono<Map<String, Object>> generateSecurityReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating comprehensive security report from {} to {}", startDate, endDate);

        return generateAIEnhancedExecutiveSummary(startDate, endDate)
                .map(executiveSummary -> {
                    Map<String, Object> report = new HashMap<>();

                    try {
                        // Executive summary with AI insights
                        report.put("executiveSummary", executiveSummary);

                        // Security events analysis
                        Map<String, Object> eventsAnalysis = securityEventService.getAggregatedMetrics(null, startDate, endDate);
                        report.put("securityEventsAnalysis", eventsAnalysis);

                        // AI threat analysis
                        Map<String, Object> aiThreatAnalysis = aiSecurityService.generateSecurityInsights();
                        report.put("aiThreatAnalysis", aiThreatAnalysis);

                        // Compliance report
                        Map<String, Object> complianceReport = complianceReportingService.generateComplianceReport("SOC2", startDate, endDate);
                        report.put("complianceAnalysis", complianceReport);

                        // Route security analysis
                        List<Map<String, Object>> routeSecurityAnalysis = generateRouteSecurityAnalysis();
                        report.put("routeSecurityAnalysis", routeSecurityAnalysis);

                        // Recommendations and action items
                        List<Map<String, Object>> actionItems = generateSecurityActionItems();
                        report.put("actionItems", actionItems);

                        report.put("reportPeriod", Map.of("start", startDate, "end", endDate));
                        report.put("generatedAt", LocalDateTime.now());

                    } catch (Exception e) {
                        log.error("Error generating security report: {}", e.getMessage(), e);
                        report.put("error", e.getMessage());
                    }

                    return report;
                });
    }

    /**
     * Scheduled AI analysis and metrics update
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void performScheduledAIAnalysis() {
        try {
            log.debug("Performing scheduled AI analysis...");

            // Update AI metrics for all routes
            for (String routeId : routeMetricsMap.keySet()) {
                updateAIMetricsForRoute(routeId);
            }

            // Clean up old metrics
            cleanupOldMetrics();

            // Update global threat assessment
            updateGlobalThreatAssessment();

            lastAIAnalysis = LocalDateTime.now();

            log.debug("Scheduled AI analysis completed");

        } catch (Exception e) {
            log.error("Error in scheduled AI analysis: {}", e.getMessage(), e);
        }
    }

    // Private helper methods

    private void initializeRouteMetrics() {
        // Initialize any default route metrics if needed
        log.debug("Initializing route metrics storage");
    }

    @Async
    protected void recordSecurityEventAsync(String routeId, String eventType) {
        recordSecurityEventAsync(routeId, eventType, null);
    }

    @Async
    protected void recordSecurityEventAsync(String routeId, String eventType, String reason) {
        try {
            // This would be recorded through SecurityEventService
            SecurityEvent event = new SecurityEvent(eventType, routeId, "analytics-system");
            if (reason != null) {
                event.setRejectionReason(reason);
            }
            securityEventService.recordEventAsync(event);
        } catch (Exception e) {
            log.warn("Failed to record security event: {}", e.getMessage());
        }
    }

    @Async
    protected void triggerAnomalyAnalysis(String routeId, String anomalyType, Object value) {
        try {
            log.debug("Triggering anomaly analysis for route: {} - Type: {} - Value: {}", routeId, anomalyType, value);
            // This could trigger the ThreatAnalysisService for further investigation
        } catch (Exception e) {
            log.warn("Failed to trigger anomaly analysis: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> getRouteAnalytics() {
        List<Map<String, Object>> routeAnalytics = new ArrayList<>();

        for (Map.Entry<String, RouteMetrics> entry : routeMetricsMap.entrySet()) {
            String routeId = entry.getKey();
            RouteMetrics metrics = entry.getValue();

            Map<String, Object> routeData = new HashMap<>();
            routeData.put("routeId", routeId);
            routeData.put("totalRequests", metrics.getTotalRequests());
            routeData.put("totalRejections", metrics.getTotalRejections());
            routeData.put("rejectionRate", metrics.getRejectionRate());
            routeData.put("averageResponseTime", metrics.getAverageResponseTime());
            routeData.put("threatScore", routeThreatScores.getOrDefault(routeId, 0.0));
            routeData.put("securityStatus", routeSecurityStatus.getOrDefault(routeId, "NORMAL"));
            routeData.put("lastActivity", metrics.getLastActivity());

            routeAnalytics.add(routeData);
        }

        return routeAnalytics;
    }

    private double calculateOverallSecurityHealth() {
        try {
            // Combine multiple factors for overall security health
            double routeHealthSum = routeMetricsMap.values().stream()
                    .mapToDouble(this::calculateRouteHealth)
                    .average().orElse(100.0);

            // Factor in global threat level
            double threatPenalty = routeThreatScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0) * 30; // Max 30 point penalty

            // Factor in recent rejection rate
            long totalRequests = RequestCountFilter.getTotalRequestCount();
            long totalRejections = RequestCountFilter.getTotalRejectedCount();
            double globalRejectionRate = totalRequests > 0 ? (double) totalRejections / totalRequests : 0.0;
            double rejectionPenalty = globalRejectionRate * 40; // Max 40 point penalty

            double healthScore = Math.max(0, Math.min(100, routeHealthSum - threatPenalty - rejectionPenalty));

            log.trace("Calculated overall security health: {:.2f}", healthScore);
            return healthScore;

        } catch (Exception e) {
            log.warn("Error calculating security health: {}", e.getMessage());
            return 75.0; // Default moderate health score
        }
    }

    private double calculateRouteHealth(RouteMetrics metrics) {
        double baseHealth = 100.0;

        // Penalize high rejection rates
        double rejectionPenalty = metrics.getRejectionRate() * 50; // Max 50% penalty for 100% rejection rate

        // Penalize slow response times
        double responsePenalty = metrics.getAverageResponseTime() > 2000 ? 20 : 0;

        return Math.max(0, baseHealth - rejectionPenalty - responsePenalty);
    }

    private Map<String, Object> calculatePerformanceTrends() {
        Map<String, Object> trends = new HashMap<>();

        try {
            // Calculate trends for each route
            Map<String, Object> routeTrends = new HashMap<>();
            for (String routeId : routeMetricsMap.keySet()) {
                routeTrends.put(routeId, calculateRouteTrend(routeId));
            }

            trends.put("routeTrends", routeTrends);
            trends.put("globalTrend", calculateGlobalTrend());
            trends.put("calculatedAt", LocalDateTime.now());

        } catch (Exception e) {
            log.warn("Error calculating performance trends: {}", e.getMessage());
        }

        return trends;
    }

    private String calculateRouteTrend(String routeId) {
        try {
            RouteMetrics metrics = routeMetricsMap.get(routeId);
            if (metrics == null) return "UNKNOWN";

            // Simple trend calculation based on recent vs historical performance
            // This is simplified - in a real implementation, you'd analyze historical data

            double currentRejectionRate = metrics.getRejectionRate();
            if (currentRejectionRate < 0.1) return "IMPROVING";
            if (currentRejectionRate > 0.3) return "DEGRADING";
            return "STABLE";

        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String calculateGlobalTrend() {
        long totalRequests = RequestCountFilter.getTotalRequestCount();
        long totalRejections = RequestCountFilter.getTotalRejectedCount();

        if (totalRequests == 0) return "NO_DATA";

        double globalRejectionRate = (double) totalRejections / totalRequests;

        if (globalRejectionRate < 0.05) return "EXCELLENT";
        if (globalRejectionRate < 0.15) return "GOOD";
        if (globalRejectionRate < 0.3) return "FAIR";
        return "POOR";
    }

    private String determineSecurityStatus(double threatScore) {
        if (threatScore > 0.8) return "CRITICAL";
        if (threatScore > 0.6) return "HIGH_RISK";
        if (threatScore > 0.4) return "MODERATE_RISK";
        if (threatScore > 0.2) return "LOW_RISK";
        return "NORMAL";
    }

    private List<String> generateAutomatedRecommendations(String threatType, double threatScore) {
        List<String> recommendations = new ArrayList<>();

        if (threatScore > 0.8) {
            recommendations.add("IMMEDIATE: Consider blocking suspicious IP addresses");
            recommendations.add("IMMEDIATE: Increase monitoring frequency");
            recommendations.add("IMMEDIATE: Review and strengthen access controls");
        } else if (threatScore > 0.6) {
            recommendations.add("HIGH: Review recent security events");
            recommendations.add("HIGH: Consider additional rate limiting");
            recommendations.add("HIGH: Monitor for pattern escalation");
        } else if (threatScore > 0.4) {
            recommendations.add("MEDIUM: Increase logging detail");
            recommendations.add("MEDIUM: Review security policies");
        } else {
            recommendations.add("LOW: Continue normal monitoring");
        }

        // Threat-type specific recommendations
        switch (threatType) {
            case "HIGH_RESPONSE_TIME":
                recommendations.add("Investigate performance bottlenecks");
                recommendations.add("Consider scaling resources");
                break;
            case "HIGH_REJECTION_RATE":
                recommendations.add("Review IP filtering rules");
                recommendations.add("Analyze attack patterns");
                break;
            case "ANOMALY":
                recommendations.add("Investigate unusual traffic patterns");
                recommendations.add("Review behavioral analysis");
                break;
        }

        return recommendations;
    }

    private List<String> generateRouteRecommendations(RouteMetrics metrics, double threatScore) {
        List<String> recommendations = new ArrayList<>();

        if (metrics.getRejectionRate() > 0.5) {
            recommendations.add("High rejection rate - Review IP filtering configuration");
        }

        if (metrics.getAverageResponseTime() > 3000) {
            recommendations.add("Slow response times - Consider performance optimization");
        }

        if (threatScore > 0.6) {
            recommendations.add("Elevated threat level - Implement additional security measures");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Route performance is within normal parameters");
        }

        return recommendations;
    }

    private Mono<Map<String, Object>> generateAIEnhancedExecutiveSummary(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Gather comprehensive security data
            List<SecurityEvent> events = new ArrayList<>();
            List<Map<String, Object>> alerts = new ArrayList<>();

            try {
                events = securityEventService.getEventsInTimeRange(startDate, endDate);
                alerts = alertingService.getActiveAlerts();
            } catch (Exception e) {
                log.warn("Could not fetch some data for executive summary: {}", e.getMessage());
            }

            // Calculate comprehensive metrics
            long totalEvents = events.size();
            long criticalEvents = events.stream()
                    .filter(e -> "CRITICAL".equals(e.getThreatLevel()))
                    .count();

            long highThreatAlerts = alerts.stream()
                    .filter(alert -> {
                        String severity = (String) alert.get("severity");
                        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
                    })
                    .count();

            // Get additional context data
            long totalRequests = RequestCountFilter.getTotalRequestCount();
            long totalRejections = RequestCountFilter.getTotalRejectedCount();
            List<Map<String, Object>> geographicThreats = getGeographicThreats();
            List<String> topAttackVectors = getTopAttackVectors();

            // Build comprehensive data package for AI
            Map<String, Object> securityData = new HashMap<>();
            securityData.put("totalEvents", totalEvents);
            securityData.put("criticalEvents", criticalEvents);
            securityData.put("highThreatAlerts", highThreatAlerts);
            securityData.put("totalRequests", totalRequests);
            securityData.put("totalRejections", totalRejections);
            securityData.put("geographicThreats", geographicThreats);
            securityData.put("topAttackVectors", topAttackVectors);
            securityData.put("analysisStartDate", startDate);
            securityData.put("analysisEndDate", endDate);

            // Get AI-powered analysis (now returns Mono)
            return openAIAnalyticsService.generateExecutiveSummary(securityData)
                    .map(aiAnalysis -> {
                        Map<String, Object> summary = new HashMap<>();

                        // Combine base metrics with AI insights
                        summary.put("totalSecurityEvents", totalEvents);
                        summary.put("criticalEvents", criticalEvents);
                        summary.put("highThreatAlerts", highThreatAlerts);
                        summary.put("aiAnalysis", aiAnalysis);
                        summary.put("geographicThreatsCount", geographicThreats.size());
                        summary.put("topAttackVectors", topAttackVectors);

                        // Determine overall posture from AI or fallback logic
                        if (aiAnalysis.containsKey("structuredAnalysis") && !aiAnalysis.containsKey("error")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> structured = (Map<String, Object>) aiAnalysis.get("structuredAnalysis");
                            summary.put("overallSecurityPosture", structured.getOrDefault("riskLevel", "MODERATE"));
                            summary.put("securityPostureScore", structured.getOrDefault("securityPostureScore", 5));
                            summary.put("aiConfidence", "HIGH");
                        } else {
                            // Fallback logic (same as before)
                            if (totalEvents > 0) {
                                double criticalEventRatio = (double) criticalEvents / totalEvents;
                                double rejectionRate = totalRequests > 0 ? (double) totalRejections / totalRequests : 0;

                                if (criticalEventRatio > 0.1 || rejectionRate > 0.5) {
                                    summary.put("overallSecurityPosture", "CONCERNING");
                                    summary.put("securityPostureScore", 3);
                                } else if (criticalEventRatio > 0.05 || rejectionRate > 0.2) {
                                    summary.put("overallSecurityPosture", "MODERATE");
                                    summary.put("securityPostureScore", 6);
                                } else {
                                    summary.put("overallSecurityPosture", "GOOD");
                                    summary.put("securityPostureScore", 8);
                                }
                            } else {
                                summary.put("overallSecurityPosture", "NO_DATA");
                                summary.put("securityPostureScore", 5);
                            }
                            summary.put("aiConfidence", "LOW");
                        }

                        summary.put("analysisTimestamp", LocalDateTime.now());
                        summary.put("analysisWindow", java.time.Duration.between(startDate, endDate).toHours() + " hours");

                        return summary;
                    });

        } catch (Exception e) {
            log.error("Error generating AI-enhanced executive summary: {}", e.getMessage(), e);
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "Failed to generate AI summary");
            errorSummary.put("fallbackData", generateBasicSummary(startDate, endDate));
            return Mono.just(errorSummary);
        }
    }

    private Map<String, Object> generateBasicSummary(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> basic = new HashMap<>();

        try {
            // Basic metrics without AI
            long totalRequests = RequestCountFilter.getTotalRequestCount();
            long totalRejections = RequestCountFilter.getTotalRejectedCount();

            basic.put("totalRequests", totalRequests);
            basic.put("totalRejections", totalRejections);
            basic.put("rejectionRate", totalRequests > 0 ? (double) totalRejections / totalRequests : 0);
            basic.put("analysisType", "Basic statistical analysis");
            basic.put("timestamp", LocalDateTime.now());

            if (totalRequests == 0) {
                basic.put("summary", "No request data available for analysis period");
                basic.put("riskLevel", "UNKNOWN");
            } else {
                double rejectionRate = (double) totalRejections / totalRequests;
                if (rejectionRate > 0.3) {
                    basic.put("summary", "High rejection rate detected - potential security issues");
                    basic.put("riskLevel", "HIGH");
                } else if (rejectionRate > 0.1) {
                    basic.put("summary", "Moderate rejection rate - continue monitoring");
                    basic.put("riskLevel", "MEDIUM");
                } else {
                    basic.put("summary", "Low rejection rate - system appears stable");
                    basic.put("riskLevel", "LOW");
                }
            }

        } catch (Exception e) {
            log.error("Error generating basic summary: {}", e.getMessage());
            basic.put("error", "Failed to generate basic summary");
        }

        return basic;
    }

    private List<Map<String, Object>> generateRouteSecurityAnalysis() {
        List<Map<String, Object>> analysis = new ArrayList<>();

        for (Map.Entry<String, RouteMetrics> entry : routeMetricsMap.entrySet()) {
            String routeId = entry.getKey();
            RouteMetrics metrics = entry.getValue();

            Map<String, Object> routeAnalysis = new HashMap<>();
            routeAnalysis.put("routeId", routeId);
            routeAnalysis.put("securityScore", calculateRouteHealth(metrics));
            routeAnalysis.put("threatLevel", routeThreatScores.getOrDefault(routeId, 0.0));
            routeAnalysis.put("status", routeSecurityStatus.getOrDefault(routeId, "NORMAL"));

            analysis.add(routeAnalysis);
        }

        return analysis;
    }

    private List<Map<String, Object>> generateSecurityActionItems() {
        List<Map<String, Object>> actionItems = new ArrayList<>();

        // Generate action items based on current security state
        Map<String, Object> item1 = new HashMap<>();
        item1.put("priority", "HIGH");
        item1.put("title", "Review high-risk routes");
        item1.put("description", "Analyze routes with elevated threat scores");
        item1.put("dueDate", LocalDateTime.now().plusDays(1));
        actionItems.add(item1);

        return actionItems;
    }

    @Async
    protected void updateAIMetricsForRoute(String routeId) {
        try {
            // This would perform AI analysis for the specific route
            log.trace("Updating AI metrics for route: {}", routeId);

            RouteMetrics metrics = routeMetricsMap.get(routeId);
            if (metrics != null) {
                // Perform AI analysis and update threat scores
                double threatScore = calculateRouteThreatScore(metrics);
                routeThreatScores.put(routeId, threatScore);

                String securityStatus = determineSecurityStatus(threatScore);
                routeSecurityStatus.put(routeId, securityStatus);
            }
        } catch (Exception e) {
            log.warn("Error updating AI metrics for route {}: {}", routeId, e.getMessage());
        }
    }

    private double calculateRouteThreatScore(RouteMetrics metrics) {
        double score = 0.0;

        // Factor in rejection rate
        score += metrics.getRejectionRate() * 0.6;

        // Factor in response time anomalies
        if (metrics.getAverageResponseTime() > 5000) {
            score += 0.3;
        }

        // Factor in recent activity patterns
        // This would be more sophisticated in a real implementation

        return Math.min(1.0, score);
    }

    private void cleanupOldMetrics() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

            // Clean up old response time data
            routeResponseTimes.values().forEach(list -> {
                if (list.size() > 1000) { // Keep only recent 1000 entries
                    list.subList(0, list.size() - 1000).clear();
                }
            });

            log.trace("Cleaned up old metrics data");
        } catch (Exception e) {
            log.warn("Error cleaning up old metrics: {}", e.getMessage());
        }
    }

    private void updateGlobalThreatAssessment() {
        try {
            // Update global threat level based on all routes
            double avgThreatScore = routeThreatScores.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0.0);

            // This could trigger global alerts or policy changes
            if (avgThreatScore > 0.7) {
                log.warn("Global threat level is elevated: {:.2f}", avgThreatScore);
            }

        } catch (Exception e) {
            log.warn("Error updating global threat assessment: {}", e.getMessage());
        }
    }

    private String calculateCurrentThreatLevel() {
        double avgThreat = routeThreatScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        return determineSecurityStatus(avgThreat);
    }

    private long getAttacksInLastHour() {
        return routeRejectionCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
    }

    private List<String> getTopAttackVectors() {
        Map<String, Long> rejectionCounts = new HashMap<>();

        for (RouteMetrics metrics : routeMetricsMap.values()) {
            metrics.getRejectionReasons().forEach((reason, count) ->
                    rejectionCounts.merge(reason, count.longValue(), Long::sum));
        }

        if (rejectionCounts.isEmpty()) {
            return new ArrayList<>(); // Return empty list instead of fake data
        }

        return rejectionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    @Autowired
    private GeolocationService geolocationService;

    @Autowired
    private OpenAIAnalyticsService openAIAnalyticsService;

    private List<Map<String, Object>> getGeographicThreats() {
        List<Map<String, Object>> threats = new ArrayList<>();

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            // Get recent security events
            List<SecurityEvent> recentEvents = new ArrayList<>();
            try {
                recentEvents = securityEventService.getEventsInTimeRange(since, LocalDateTime.now());
            } catch (Exception e) {
                log.warn("Could not fetch security events for geographic analysis: {}", e.getMessage());
                return threats; // Return empty list if we can't get events
            }

            // Group events by IP address (exclude localhost)
            Map<String, List<SecurityEvent>> eventsByIp = recentEvents.stream()
                    .filter(e -> e.getClientIp() != null &&
                            !e.getClientIp().equals("127.0.0.1") &&
                            !e.getClientIp().equals("::1"))
                    .collect(Collectors.groupingBy(SecurityEvent::getClientIp));

            // Analyze each IP for geographic patterns
            Map<String, GeographicThreatData> countryThreats = new HashMap<>();

            for (Map.Entry<String, List<SecurityEvent>> entry : eventsByIp.entrySet()) {
                String ip = entry.getKey();
                List<SecurityEvent> ipEvents = entry.getValue();

                // Get geolocation data
                GeolocationService.GeolocationData location = geolocationService.getLocation(ip);
                String country = location.getCountry();

                // Skip if country is unknown or local
                if ("Unknown".equals(country) || "Local".equals(country)) {
                    continue;
                }

                // Count rejections and total events for this IP
                long rejections = ipEvents.stream()
                        .filter(e -> "REJECTION".equals(e.getEventType()))
                        .count();

                long totalEvents = ipEvents.size();

                // Update country-level threat data
                GeographicThreatData threatData = countryThreats.computeIfAbsent(country,
                        k -> new GeographicThreatData(country));

                threatData.addIP(ip, totalEvents, rejections, location.getCity());
            }

            // Convert to threat list and filter for suspicious countries
            for (GeographicThreatData threatData : countryThreats.values()) {
                if (threatData.isSuspicious()) {
                    Map<String, Object> threat = new HashMap<>();
                    threat.put("country", threatData.getCountry());
                    threat.put("uniqueIPs", threatData.getUniqueIPs());
                    threat.put("totalEvents", threatData.getTotalEvents());
                    threat.put("totalRejections", threatData.getTotalRejections());
                    threat.put("rejectionRate", Math.round(threatData.getRejectionRate() * 100.0) / 100.0);
                    threat.put("threatLevel", threatData.getThreatLevel());
                    threat.put("cities", threatData.getTopCities());
                    threat.put("suspiciousIPs", threatData.getSuspiciousIPs().size());

                    threats.add(threat);
                }
            }

            // Sort by threat level and total rejections
            threats.sort((a, b) -> {
                String levelA = (String) a.get("threatLevel");
                String levelB = (String) b.get("threatLevel");
                int levelComparison = getThreatLevelValue(levelB) - getThreatLevelValue(levelA);
                if (levelComparison != 0) return levelComparison;

                // If same threat level, sort by total rejections
                Long rejectionsA = (Long) a.get("totalRejections");
                Long rejectionsB = (Long) b.get("totalRejections");
                return rejectionsB.compareTo(rejectionsA);
            });

            log.info("Identified {} geographic threats from {} countries",
                    threats.size(), countryThreats.size());

        } catch (Exception e) {
            log.error("Error analyzing geographic threats: {}", e.getMessage(), e);
        }

        return threats;
    }

    private static class GeographicThreatData {
        private final String country;
        private final Set<String> uniqueIPs = new HashSet<>();
        private final Map<String, Integer> cityCount = new HashMap<>();
        private final List<String> suspiciousIPs = new ArrayList<>();
        private long totalEvents = 0;
        private long totalRejections = 0;

        public GeographicThreatData(String country) {
            this.country = country;
        }

        public void addIP(String ip, long events, long rejections, String city) {
            uniqueIPs.add(ip);
            totalEvents += events;
            totalRejections += rejections;

            if (city != null && !city.equals("Unknown") && !city.trim().isEmpty()) {
                cityCount.merge(city, 1, Integer::sum);
            }

            // Mark as suspicious if high rejection rate or high total rejections
            double rejectionRate = events > 0 ? (double) rejections / events : 0;
            if (rejectionRate > 0.5 || rejections > 10) {
                suspiciousIPs.add(ip);
            }
        }

        public boolean isSuspicious() {
            // Consider suspicious if:
            // 1. Multiple IPs from same country (potential coordinated attack)
            // 2. High overall rejection rate from this country
            // 3. High total rejections from this country
            // 4. Multiple suspicious IPs
            return uniqueIPs.size() >= 2 ||
                    getRejectionRate() > 0.3 ||
                    totalRejections > 15 ||
                    suspiciousIPs.size() >= 2;
        }

        public String getThreatLevel() {
            if (totalRejections > 100 || getRejectionRate() > 0.7 || suspiciousIPs.size() > 5) {
                return "CRITICAL";
            }
            if (totalRejections > 50 || getRejectionRate() > 0.5 || suspiciousIPs.size() > 3) {
                return "HIGH";
            }
            if (totalRejections > 20 || getRejectionRate() > 0.3 || suspiciousIPs.size() > 1) {
                return "MEDIUM";
            }
            return "LOW";
        }

        public double getRejectionRate() {
            return totalEvents > 0 ? (double) totalRejections / totalEvents : 0;
        }

        public List<String> getTopCities() {
            return cityCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        // Getters
        public String getCountry() { return country; }
        public int getUniqueIPs() { return uniqueIPs.size(); }
        public long getTotalEvents() { return totalEvents; }
        public long getTotalRejections() { return totalRejections; }
        public List<String> getSuspiciousIPs() { return suspiciousIPs; }
    }

    private int getThreatLevelValue(String level) {
        return switch (level) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    // Inner class for route metrics
    private static class RouteMetrics {
        private final String routeId;
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalRejections = new AtomicLong(0);
        private final Map<String, AtomicLong> rejectionReasons = new ConcurrentHashMap<>();
        private final List<Long> responseTimes = new ArrayList<>();
        private LocalDateTime lastActivity = LocalDateTime.now();

        public RouteMetrics(String routeId) {
            this.routeId = routeId;
        }

        public void incrementRequests() {
            totalRequests.incrementAndGet();
            lastActivity = LocalDateTime.now();
        }

        public void incrementRejections() {
            totalRejections.incrementAndGet();
            lastActivity = LocalDateTime.now();
        }

        public void addRejectionReason(String reason) {
            if (reason != null) {
                rejectionReasons.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();
            }
        }

        public synchronized void addResponseTime(long responseTime) {
            responseTimes.add(responseTime);
            // Keep only recent 100 response times
            if (responseTimes.size() > 100) {
                responseTimes.remove(0);
            }
            lastActivity = LocalDateTime.now();
        }

        public long getTotalRequests() { return totalRequests.get(); }
        public long getTotalRejections() { return totalRejections.get(); }

        public double getRejectionRate() {
            long requests = totalRequests.get();
            return requests > 0 ? (double) totalRejections.get() / requests : 0.0;
        }

        public synchronized double getAverageResponseTime() {
            return responseTimes.isEmpty() ? 0.0 :
                    responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }

        public Map<String, AtomicLong> getRejectionReasons() { return rejectionReasons; }
        public LocalDateTime getLastActivity() { return lastActivity; }
    }

    // Inner class for AI metrics
    private static class AIMetrics {
        private double threatScore;
        private String securityStatus;
        private LocalDateTime lastAnalysis;
        private Map<String, Object> behavioralInsights;

        // Getters and setters
        public double getThreatScore() { return threatScore; }
        public void setThreatScore(double threatScore) { this.threatScore = threatScore; }

        public String getSecurityStatus() { return securityStatus; }
        public void setSecurityStatus(String securityStatus) { this.securityStatus = securityStatus; }

        public LocalDateTime getLastAnalysis() { return lastAnalysis; }
        public void setLastAnalysis(LocalDateTime lastAnalysis) { this.lastAnalysis = lastAnalysis; }

        public Map<String, Object> getBehavioralInsights() { return behavioralInsights; }
        public void setBehavioralInsights(Map<String, Object> behavioralInsights) {
            this.behavioralInsights = behavioralInsights;
        }
    }
}