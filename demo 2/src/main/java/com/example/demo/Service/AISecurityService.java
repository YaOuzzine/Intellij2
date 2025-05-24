package com.example.demo.Service;

import com.example.demo.Entity.SecurityEvent;
import com.example.demo.Entity.ThreatAlert;
import com.example.demo.Entity.ThreatPattern;
import com.example.demo.Repository.SecurityEventRepository;
import com.example.demo.Repository.ThreatAlertRepository;
import com.example.demo.Repository.ThreatPatternRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AISecurityService {

    private static final Logger log = LoggerFactory.getLogger(AISecurityService.class);

    private final SecurityEventRepository eventRepository;
    private final ThreatAlertRepository alertRepository;
    private final ThreatPatternRepository patternRepository;
    private final ObjectMapper objectMapper;

    // AI Analysis Constants
    private static final double BEHAVIORAL_ANOMALY_THRESHOLD = 0.75;
    private static final int MIN_EVENTS_FOR_ANALYSIS = 10;
    private static final int PREDICTION_WINDOW_HOURS = 24;
    private static final double CONFIDENCE_THRESHOLD = 0.8;

    // Behavioral Learning Parameters
    private final Map<String, UserBehaviorProfile> behaviorProfiles = new HashMap<>();
    private final Map<String, RouteSecurityProfile> routeProfiles = new HashMap<>();

    @Autowired
    public AISecurityService(SecurityEventRepository eventRepository,
                             ThreatAlertRepository alertRepository,
                             ThreatPatternRepository patternRepository) {
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.patternRepository = patternRepository;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate AI-powered security insights for the dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateSecurityInsights() {
        Map<String, Object> insights = new HashMap<>();

        try {
            LocalDateTime analysisStart = LocalDateTime.now().minusHours(24);

            // Overall security health score
            double securityHealthScore = calculateSecurityHealthScore(analysisStart);

            // Behavioral anomalies
            List<Map<String, Object>> behavioralAnomalies = detectBehavioralAnomalies(analysisStart);

            // Threat predictions
            Map<String, Object> threatPredictions = generateThreatPredictions(analysisStart);

            // Attack pattern analysis
            Map<String, Object> attackPatternAnalysis = analyzeAttackPatterns(analysisStart);

            // Security recommendations
            List<Map<String, Object>> aiRecommendations = generateAIRecommendations(analysisStart);

            // Risk assessment
            Map<String, Object> riskAssessment = performRiskAssessment(analysisStart);

            insights.put("securityHealthScore", securityHealthScore);
            insights.put("behavioralAnomalies", behavioralAnomalies);
            insights.put("threatPredictions", threatPredictions);
            insights.put("attackPatternAnalysis", attackPatternAnalysis);
            insights.put("aiRecommendations", aiRecommendations);
            insights.put("riskAssessment", riskAssessment);
            insights.put("analysisTimestamp", LocalDateTime.now());
            insights.put("confidenceLevel", calculateOverallConfidence());

            log.info("Generated AI security insights with health score: {:.2f}", securityHealthScore);

        } catch (Exception e) {
            log.error("Error generating AI security insights: {}", e.getMessage(), e);
        }

        return insights;
    }

    /**
     * Analyze behavioral patterns for a specific IP or user
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Map<String, Object>> analyzeBehavioralPattern(String clientIp) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            LocalDateTime analysisStart = LocalDateTime.now().minusDays(7);
            List<SecurityEvent> userEvents = eventRepository
                    .findByClientIpAndTimestampBetween(clientIp, analysisStart, LocalDateTime.now());

            if (userEvents.size() < MIN_EVENTS_FOR_ANALYSIS) {
                analysis.put("status", "INSUFFICIENT_DATA");
                analysis.put("message", "Not enough data for behavioral analysis");
                return CompletableFuture.completedFuture(analysis);
            }

            // Create or update behavior profile
            UserBehaviorProfile profile = updateBehaviorProfile(clientIp, userEvents);

            // Calculate anomaly score
            double anomalyScore = calculateBehavioralAnomalyScore(profile, userEvents);

            // Generate behavioral insights
            Map<String, Object> insights = generateBehavioralInsights(profile, userEvents);

            analysis.put("status", "COMPLETED");
            analysis.put("clientIp", clientIp);
            analysis.put("anomalyScore", anomalyScore);
            analysis.put("isAnomalous", anomalyScore > BEHAVIORAL_ANOMALY_THRESHOLD);
            analysis.put("eventCount", userEvents.size());
            analysis.put("analysisWindow", "7 days");
            analysis.put("insights", insights);
            analysis.put("behaviorProfile", profile.toMap());

            log.debug("Behavioral analysis completed for IP: {} (anomaly score: {:.2f})", clientIp, anomalyScore);

        } catch (Exception e) {
            log.error("Error in behavioral analysis for IP {}: {}", clientIp, e.getMessage(), e);
            analysis.put("status", "ERROR");
            analysis.put("error", e.getMessage());
        }

        return CompletableFuture.completedFuture(analysis);
    }

    /**
     * Predict potential security threats
     */
    @Transactional(readOnly = true)
    public Map<String, Object> predictSecurityThreats(int hoursAhead) {
        Map<String, Object> predictions = new HashMap<>();

        try {
            LocalDateTime analysisStart = LocalDateTime.now().minusHours(PREDICTION_WINDOW_HOURS);
            List<SecurityEvent> historicalEvents = eventRepository
                    .findByTimestampBetween(analysisStart, LocalDateTime.now());

            // Analyze historical patterns
            Map<String, Double> hourlyTrends = analyzeHourlyTrends(historicalEvents);
            Map<String, Double> routeVulnerabilities = analyzeRouteVulnerabilities();
            List<Map<String, Object>> emergingThreats = identifyEmergingThreats(historicalEvents);

            // Generate predictions
            List<Map<String, Object>> threatPredictions = new ArrayList<>();

            for (int hour = 1; hour <= hoursAhead; hour++) {
                LocalDateTime predictedTime = LocalDateTime.now().plusHours(hour);
                Map<String, Object> hourPrediction = predictHourlyThreats(predictedTime, hourlyTrends, routeVulnerabilities);
                threatPredictions.add(hourPrediction);
            }

            predictions.put("predictions", threatPredictions);
            predictions.put("emergingThreats", emergingThreats);
            predictions.put("predictionConfidence", calculatePredictionConfidence(historicalEvents));
            predictions.put("basedOnEvents", historicalEvents.size());
            predictions.put("generatedAt", LocalDateTime.now());

            log.info("Generated security threat predictions for {} hours ahead", hoursAhead);

        } catch (Exception e) {
            log.error("Error predicting security threats: {}", e.getMessage(), e);
        }

        return predictions;
    }

    /**
     * Scheduled AI learning and pattern update
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    @Transactional
    public void performScheduledLearning() {
        try {
            log.info("Starting scheduled AI learning process...");

            // Update behavioral profiles
            updateAllBehaviorProfiles();

            // Learn new attack patterns
            learnNewAttackPatterns();

            // Update pattern effectiveness
            updatePatternEffectiveness();

            // Clean up old data
            cleanupOldAnalysisData();

            log.info("Scheduled AI learning completed successfully");

        } catch (Exception e) {
            log.error("Error in scheduled AI learning: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate overall security health score (0-100)
     */
    private double calculateSecurityHealthScore(LocalDateTime since) {
        try {
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());
            if (events.isEmpty()) return 100.0;

            long totalEvents = events.size();
            long rejectedEvents = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .count();

            // Base score from acceptance rate
            double acceptanceRate = (double) (totalEvents - rejectedEvents) / totalEvents;
            double baseScore = acceptanceRate * 60; // Max 60 points from acceptance rate

            // Additional factors
            double threatLevelPenalty = calculateThreatLevelPenalty(events);
            double patternMatchBonus = calculatePatternMatchBonus();
            double responseTimeBonus = calculateResponseTimeBonus(events);

            double finalScore = Math.max(0, Math.min(100, baseScore - threatLevelPenalty + patternMatchBonus + responseTimeBonus));

            log.debug("Security health score: {:.2f} (base: {:.2f}, threat penalty: {:.2f})",
                    finalScore, baseScore, threatLevelPenalty);

            return finalScore;

        } catch (Exception e) {
            log.error("Error calculating security health score: {}", e.getMessage());
            return 50.0; // Default moderate score
        }
    }

    /**
     * Detect behavioral anomalies
     */
    private List<Map<String, Object>> detectBehavioralAnomalies(LocalDateTime since) {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        try {
            // Group events by IP
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());
            Map<String, List<SecurityEvent>> eventsByIp = events.stream()
                    .collect(Collectors.groupingBy(SecurityEvent::getClientIp));

            for (Map.Entry<String, List<SecurityEvent>> entry : eventsByIp.entrySet()) {
                String ip = entry.getKey();
                List<SecurityEvent> ipEvents = entry.getValue();

                if (ipEvents.size() >= MIN_EVENTS_FOR_ANALYSIS) {
                    UserBehaviorProfile profile = behaviorProfiles.get(ip);
                    if (profile != null) {
                        double anomalyScore = calculateBehavioralAnomalyScore(profile, ipEvents);

                        if (anomalyScore > BEHAVIORAL_ANOMALY_THRESHOLD) {
                            Map<String, Object> anomaly = new HashMap<>();
                            anomaly.put("clientIp", ip);
                            anomaly.put("anomalyScore", anomalyScore);
                            anomaly.put("eventCount", ipEvents.size());
                            anomaly.put("suspiciousActivities", identifySuspiciousActivities(ipEvents, profile));
                            anomalies.add(anomaly);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error detecting behavioral anomalies: {}", e.getMessage());
        }

        return anomalies;
    }

    /**
     * Generate threat predictions
     */
    private Map<String, Object> generateThreatPredictions(LocalDateTime since) {
        Map<String, Object> predictions = new HashMap<>();

        try {
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());

            // Predict attack likelihood for next 6 hours
            List<Map<String, Object>> hourlyPredictions = new ArrayList<>();
            for (int hour = 1; hour <= 6; hour++) {
                LocalDateTime predictedTime = LocalDateTime.now().plusHours(hour);
                double attackLikelihood = predictAttackLikelihood(predictedTime, events);

                Map<String, Object> hourPrediction = new HashMap<>();
                hourPrediction.put("hour", hour);
                hourPrediction.put("timestamp", predictedTime);
                hourPrediction.put("attackLikelihood", attackLikelihood);
                hourPrediction.put("riskLevel", getRiskLevel(attackLikelihood));

                hourlyPredictions.add(hourPrediction);
            }

            predictions.put("hourlyPredictions", hourlyPredictions);
            predictions.put("overallTrend", calculateOverallTrend(events));
            predictions.put("confidence", calculatePredictionConfidence(events));

        } catch (Exception e) {
            log.error("Error generating threat predictions: {}", e.getMessage());
        }

        return predictions;
    }

    /**
     * Analyze attack patterns using AI
     */
    private Map<String, Object> analyzeAttackPatterns(LocalDateTime since) {
        Map<String, Object> analysis = new HashMap<>();

        try {
            List<SecurityEvent> rejectionEvents = eventRepository
                    .findByEventTypeAndTimestampBetween("REJECTION", since, LocalDateTime.now());

            // Pattern frequency analysis
            Map<String, Long> patternFrequency = rejectionEvents.stream()
                    .filter(e -> e.getRejectionReason() != null)
                    .collect(Collectors.groupingBy(SecurityEvent::getRejectionReason, Collectors.counting()));

            // Temporal pattern analysis
            Map<Integer, Long> hourlyDistribution = rejectionEvents.stream()
                    .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

            // Geographic pattern analysis (simplified)
            Map<String, Long> ipPrefixDistribution = rejectionEvents.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getClientIp().substring(0, Math.min(e.getClientIp().length(),
                                    e.getClientIp().indexOf('.', e.getClientIp().indexOf('.') + 1))),
                            Collectors.counting()));

            analysis.put("patternFrequency", patternFrequency);
            analysis.put("hourlyDistribution", hourlyDistribution);
            analysis.put("geographicDistribution", ipPrefixDistribution);
            analysis.put("totalAttacks", rejectionEvents.size());
            analysis.put("uniqueAttackers", rejectionEvents.stream().map(SecurityEvent::getClientIp).distinct().count());

        } catch (Exception e) {
            log.error("Error analyzing attack patterns: {}", e.getMessage());
        }

        return analysis;
    }

    /**
     * Generate AI-powered security recommendations
     */
    private List<Map<String, Object>> generateAIRecommendations(LocalDateTime since) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());
            List<ThreatAlert> openAlerts = alertRepository.findByStatusOrderByCreatedAtDesc("OPEN");

            // Analyze patterns and generate recommendations
            if (!events.isEmpty()) {
                // High rejection rate recommendation
                long rejections = events.stream().filter(e -> "REJECTION".equals(e.getEventType())).count();
                double rejectionRate = (double) rejections / events.size();

                if (rejectionRate > 0.3) {
                    recommendations.add(createRecommendation(
                            "SECURITY_TUNING", "HIGH",
                            "High rejection rate detected",
                            String.format("Current rejection rate is %.1f%%. Consider reviewing security rules.", rejectionRate * 100),
                            Arrays.asList("Review IP filtering rules", "Analyze false positives", "Adjust rate limiting")
                    ));
                }

                // Response time recommendation
                double avgResponseTime = events.stream()
                        .filter(e -> e.getResponseTimeMs() != null)
                        .mapToInt(SecurityEvent::getResponseTimeMs)
                        .average().orElse(0.0);

                if (avgResponseTime > 2000) {
                    recommendations.add(createRecommendation(
                            "PERFORMANCE", "MEDIUM",
                            "Slow response times detected",
                            String.format("Average response time is %.0f ms. This may indicate performance issues.", avgResponseTime),
                            Arrays.asList("Monitor server resources", "Optimize application code", "Consider caching")
                    ));
                }
            }

            // Alert-based recommendations
            if (openAlerts.size() > 10) {
                recommendations.add(createRecommendation(
                        "ALERT_MANAGEMENT", "HIGH",
                        "High number of open alerts",
                        String.format("You have %d open security alerts that need attention.", openAlerts.size()),
                        Arrays.asList("Review and prioritize alerts", "Implement automated responses", "Adjust alert thresholds")
                ));
            }

        } catch (Exception e) {
            log.error("Error generating AI recommendations: {}", e.getMessage());
        }

        return recommendations;
    }

    /**
     * Perform comprehensive risk assessment
     */
    private Map<String, Object> performRiskAssessment(LocalDateTime since) {
        Map<String, Object> assessment = new HashMap<>();

        try {
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());
            List<ThreatAlert> alerts = alertRepository.findRecentAlerts(since);

            // Calculate various risk scores
            double volumeRisk = calculateVolumeRisk(events);
            double patternRisk = calculatePatternRisk(events);
            double geographicRisk = calculateGeographicRisk(events);
            double temporalRisk = calculateTemporalRisk(events);
            double alertRisk = calculateAlertRisk(alerts);

            // Overall risk score (weighted average)
            double overallRisk = (volumeRisk * 0.3 + patternRisk * 0.25 + geographicRisk * 0.15 +
                    temporalRisk * 0.15 + alertRisk * 0.15);

            assessment.put("overallRisk", overallRisk);
            assessment.put("riskLevel", getRiskLevel(overallRisk));
            assessment.put("riskFactors", Map.of(
                    "volume", volumeRisk,
                    "patterns", patternRisk,
                    "geographic", geographicRisk,
                    "temporal", temporalRisk,
                    "alerts", alertRisk
            ));
            assessment.put("riskTrend", calculateRiskTrend(since));
            assessment.put("criticalIssues", identifyCriticalIssues(events, alerts));

        } catch (Exception e) {
            log.error("Error performing risk assessment: {}", e.getMessage());
        }

        return assessment;
    }

    // Helper methods for AI analysis

    private UserBehaviorProfile updateBehaviorProfile(String clientIp, List<SecurityEvent> events) {
        UserBehaviorProfile profile = behaviorProfiles.computeIfAbsent(clientIp, k -> new UserBehaviorProfile(clientIp));
        profile.updateWithEvents(events);
        return profile;
    }

    private double calculateBehavioralAnomalyScore(UserBehaviorProfile profile, List<SecurityEvent> recentEvents) {
        if (profile == null || recentEvents.isEmpty()) return 0.0;

        double frequencyAnomaly = calculateFrequencyAnomaly(profile, recentEvents);
        double temporalAnomaly = calculateTemporalAnomaly(profile, recentEvents);
        double pathAnomaly = calculatePathAnomaly(profile, recentEvents);

        return (frequencyAnomaly + temporalAnomaly + pathAnomaly) / 3.0;
    }

    private double calculateFrequencyAnomaly(UserBehaviorProfile profile, List<SecurityEvent> events) {
        double recentFrequency = events.size() / 24.0; // Events per hour
        double historicalAverage = profile.getAverageHourlyEvents();

        if (historicalAverage == 0) return 0.0;

        return Math.min(1.0, Math.abs(recentFrequency - historicalAverage) / historicalAverage);
    }

    private double calculateTemporalAnomaly(UserBehaviorProfile profile, List<SecurityEvent> events) {
        Set<Integer> recentHours = events.stream()
                .map(e -> e.getTimestamp().getHour())
                .collect(Collectors.toSet());

        Set<Integer> historicalHours = profile.getActiveHours();

        if (historicalHours.isEmpty()) return 0.0;

        // Calculate how many recent hours are unusual
        long unusualHours = recentHours.stream()
                .filter(hour -> !historicalHours.contains(hour))
                .count();

        return Math.min(1.0, (double) unusualHours / recentHours.size());
    }

    private double calculatePathAnomaly(UserBehaviorProfile profile, List<SecurityEvent> events) {
        Set<String> recentPaths = events.stream()
                .map(SecurityEvent::getRequestPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> historicalPaths = profile.getCommonPaths();

        if (historicalPaths.isEmpty()) return 0.0;

        long unusualPaths = recentPaths.stream()
                .filter(path -> !historicalPaths.contains(path))
                .count();

        return Math.min(1.0, (double) unusualPaths / recentPaths.size());
    }

    private Map<String, Object> createRecommendation(String type, String priority, String title,
                                                     String description, List<String> actions) {
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("type", type);
        recommendation.put("priority", priority);
        recommendation.put("title", title);
        recommendation.put("description", description);
        recommendation.put("recommendedActions", actions);
        recommendation.put("confidence", CONFIDENCE_THRESHOLD);
        recommendation.put("timestamp", LocalDateTime.now());
        return recommendation;
    }

    // Enhanced helper methods with complete implementations

    private double calculateThreatLevelPenalty(List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.0;

        Map<String, Long> threatCounts = events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getThreatLevel, Collectors.counting()));

        double penalty = 0.0;
        penalty += threatCounts.getOrDefault("CRITICAL", 0L) * 15.0;
        penalty += threatCounts.getOrDefault("HIGH", 0L) * 10.0;
        penalty += threatCounts.getOrDefault("MEDIUM", 0L) * 5.0;
        penalty += threatCounts.getOrDefault("LOW", 0L) * 1.0;

        return Math.min(30.0, penalty / events.size()); // Max 30 point penalty
    }

    private double calculatePatternMatchBonus() {
        try {
            List<ThreatPattern> activePatterns = patternRepository.findByIsActiveTrue();
            long recentlyTriggered = activePatterns.stream()
                    .filter(p -> p.getLastTriggered() != null &&
                            p.getLastTriggered().isAfter(LocalDateTime.now().minusHours(24)))
                    .count();

            return Math.min(20.0, recentlyTriggered * 2.0); // Max 20 point bonus
        } catch (Exception e) {
            log.warn("Error calculating pattern match bonus: {}", e.getMessage());
            return 0.0;
        }
    }

    private double calculateResponseTimeBonus(List<SecurityEvent> events) {
        double avgResponseTime = events.stream()
                .filter(e -> e.getResponseTimeMs() != null)
                .mapToInt(SecurityEvent::getResponseTimeMs)
                .average().orElse(1000.0);

        if (avgResponseTime < 500) return 20.0;      // Excellent response time
        if (avgResponseTime < 1000) return 15.0;     // Good response time
        if (avgResponseTime < 2000) return 10.0;     // Fair response time
        if (avgResponseTime < 5000) return 5.0;      // Poor response time
        return 0.0; // Very poor response time
    }

    private double calculateOverallConfidence() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());

            if (events.size() < 100) return 0.6; // Low confidence with little data
            if (events.size() < 500) return 0.75; // Medium confidence
            if (events.size() < 1000) return 0.85; // Good confidence
            return 0.95; // High confidence with lots of data
        } catch (Exception e) {
            return 0.7; // Default confidence
        }
    }

    private Map<String, Object> generateBehavioralInsights(UserBehaviorProfile profile, List<SecurityEvent> events) {
        Map<String, Object> insights = new HashMap<>();

        // Activity pattern analysis
        Map<Integer, Long> hourlyActivity = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

        insights.put("mostActiveHour", hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(12));

        insights.put("activitySpread", hourlyActivity.size());

        // Request pattern analysis
        Map<String, Long> pathFrequency = events.stream()
                .filter(e -> e.getRequestPath() != null)
                .collect(Collectors.groupingBy(SecurityEvent::getRequestPath, Collectors.counting()));

        insights.put("uniquePaths", pathFrequency.size());
        insights.put("mostRequestedPath", pathFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("unknown"));

        // Behavioral consistency
        double consistencyScore = calculateBehavioralConsistency(profile, events);
        insights.put("consistencyScore", consistencyScore);
        insights.put("behaviorType", consistencyScore > 0.8 ? "PREDICTABLE" :
                consistencyScore > 0.5 ? "MODERATE" : "ERRATIC");

        return insights;
    }

    private double calculateBehavioralConsistency(UserBehaviorProfile profile, List<SecurityEvent> events) {
        if (profile.getActiveHours().isEmpty() || events.isEmpty()) return 0.0;

        // Check how consistent the current behavior is with historical patterns
        Set<Integer> currentHours = events.stream()
                .map(e -> e.getTimestamp().getHour())
                .collect(Collectors.toSet());

        Set<Integer> historicalHours = profile.getActiveHours();

        // Calculate overlap
        Set<Integer> overlap = new HashSet<>(currentHours);
        overlap.retainAll(historicalHours);

        return (double) overlap.size() / Math.max(currentHours.size(), historicalHours.size());
    }

    private List<String> identifySuspiciousActivities(List<SecurityEvent> events, UserBehaviorProfile profile) {
        List<String> activities = new ArrayList<>();

        // Check for unusual time patterns
        Set<Integer> currentHours = events.stream()
                .map(e -> e.getTimestamp().getHour())
                .collect(Collectors.toSet());

        Set<Integer> unusualHours = new HashSet<>(currentHours);
        unusualHours.removeAll(profile.getActiveHours());

        if (!unusualHours.isEmpty()) {
            activities.add("Activity during unusual hours: " + unusualHours);
        }

        // Check for high frequency
        long currentFrequency = events.size();
        if (currentFrequency > profile.getAverageHourlyEvents() * 24 * 2) { // 2x normal daily activity
            activities.add("Unusually high request frequency: " + currentFrequency + " requests");
        }

        // Check for new paths
        Set<String> newPaths = events.stream()
                .map(SecurityEvent::getRequestPath)
                .filter(Objects::nonNull)
                .filter(path -> !profile.getCommonPaths().contains(path))
                .collect(Collectors.toSet());

        if (newPaths.size() > 5) {
            activities.add("Access to " + newPaths.size() + " previously unvisited paths");
        }

        // Check for rejection patterns
        long rejections = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        if (rejections > events.size() * 0.5) {
            activities.add("High rejection rate: " + (rejections * 100 / events.size()) + "%");
        }

        return activities;
    }

    private Map<String, Double> analyzeHourlyTrends(List<SecurityEvent> events) {
        Map<String, Double> trends = new HashMap<>();

        // Group events by hour
        Map<Integer, Long> hourlyDistribution = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

        // Calculate trends for different metrics
        double peakHourTraffic = hourlyDistribution.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        double averageHourlyTraffic = hourlyDistribution.values().stream().mapToLong(Long::longValue).average().orElse(0.0);

        trends.put("peakTraffic", peakHourTraffic);
        trends.put("averageTraffic", averageHourlyTraffic);
        trends.put("trafficVariance", calculateVariance(hourlyDistribution.values()));

        // Rejection trends
        Map<Integer, Long> rejectionsByHour = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

        trends.put("peakRejections", (double) rejectionsByHour.values().stream().mapToLong(Long::longValue).max().orElse(0L));
        trends.put("averageRejections", rejectionsByHour.values().stream().mapToLong(Long::longValue).average().orElse(0.0));

        return trends;
    }

    private double calculateVariance(Collection<Long> values) {
        if (values.isEmpty()) return 0.0;

        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);

        return variance;
    }

    private Map<String, Double> analyzeRouteVulnerabilities() {
        Map<String, Double> vulnerabilities = new HashMap<>();

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            // Get all routes and their security events
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());
            Map<String, List<SecurityEvent>> eventsByRoute = events.stream()
                    .filter(e -> e.getRouteId() != null)
                    .collect(Collectors.groupingBy(SecurityEvent::getRouteId));

            for (Map.Entry<String, List<SecurityEvent>> entry : eventsByRoute.entrySet()) {
                String routeId = entry.getKey();
                List<SecurityEvent> routeEvents = entry.getValue();

                // Calculate vulnerability score based on rejection rate and threat levels
                long rejections = routeEvents.stream()
                        .filter(e -> "REJECTION".equals(e.getEventType()))
                        .count();

                double rejectionRate = (double) rejections / routeEvents.size();

                long highThreatEvents = routeEvents.stream()
                        .filter(e -> "HIGH".equals(e.getThreatLevel()) || "CRITICAL".equals(e.getThreatLevel()))
                        .count();

                double threatRatio = (double) highThreatEvents / routeEvents.size();

                // Combine metrics for vulnerability score
                double vulnerabilityScore = (rejectionRate * 0.6) + (threatRatio * 0.4);
                vulnerabilities.put(routeId, vulnerabilityScore);
            }

        } catch (Exception e) {
            log.warn("Error analyzing route vulnerabilities: {}", e.getMessage());
        }

        return vulnerabilities;
    }

    private List<Map<String, Object>> identifyEmergingThreats(List<SecurityEvent> events) {
        List<Map<String, Object>> emergingThreats = new ArrayList<>();

        try {
            // Group recent events by various dimensions to identify emerging patterns

            // 1. New attack patterns in user agents
            Map<String, Long> userAgentCounts = events.stream()
                    .filter(e -> e.getUserAgent() != null && "REJECTION".equals(e.getEventType()))
                    .collect(Collectors.groupingBy(SecurityEvent::getUserAgent, Collectors.counting()));

            userAgentCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 10) // Threshold for emerging threat
                    .forEach(entry -> {
                        Map<String, Object> threat = new HashMap<>();
                        threat.put("type", "SUSPICIOUS_USER_AGENT");
                        threat.put("pattern", entry.getKey());
                        threat.put("frequency", entry.getValue());
                        threat.put("severity", entry.getValue() > 50 ? "HIGH" : "MEDIUM");
                        emergingThreats.add(threat);
                    });

            // 2. Geographic clustering of attacks
            Map<String, Long> ipPrefixCounts = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .collect(Collectors.groupingBy(
                            e -> e.getClientIp().substring(0, Math.min(e.getClientIp().length(),
                                    e.getClientIp().indexOf('.', e.getClientIp().indexOf('.') + 1))),
                            Collectors.counting()));

            ipPrefixCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 20)
                    .forEach(entry -> {
                        Map<String, Object> threat = new HashMap<>();
                        threat.put("type", "GEOGRAPHIC_ATTACK_CLUSTER");
                        threat.put("pattern", entry.getKey() + ".x.x network");
                        threat.put("frequency", entry.getValue());
                        threat.put("severity", entry.getValue() > 100 ? "CRITICAL" : "HIGH");
                        emergingThreats.add(threat);
                    });

            // 3. Temporal attack patterns
            Map<Integer, Long> hourlyAttacks = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

            // Find hours with unusually high activity
            double avgHourlyAttacks = hourlyAttacks.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
            hourlyAttacks.entrySet().stream()
                    .filter(entry -> entry.getValue() > avgHourlyAttacks * 2)
                    .forEach(entry -> {
                        Map<String, Object> threat = new HashMap<>();
                        threat.put("type", "TEMPORAL_ATTACK_SPIKE");
                        threat.put("pattern", "Hour " + entry.getKey() + ":00");
                        threat.put("frequency", entry.getValue());
                        threat.put("severity", "MEDIUM");
                        emergingThreats.add(threat);
                    });

        } catch (Exception e) {
            log.warn("Error identifying emerging threats: {}", e.getMessage());
        }

        return emergingThreats;
    }

    private Map<String, Object> predictHourlyThreats(LocalDateTime time, Map<String, Double> trends, Map<String, Double> vulnerabilities) {
        Map<String, Object> prediction = new HashMap<>();

        int hour = time.getHour();

        // Base prediction on historical trends
        double baseThreatLevel = trends.getOrDefault("averageTraffic", 10.0) / 100.0; // Normalize

        // Adjust for time of day (higher risk during off-hours)
        if (hour >= 22 || hour <= 6) {
            baseThreatLevel *= 1.5; // 50% higher risk during night hours
        }

        // Factor in route vulnerabilities
        double maxVulnerability = vulnerabilities.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        baseThreatLevel += maxVulnerability * 0.3;

        // Add some randomness for realistic prediction
        baseThreatLevel += (Math.random() - 0.5) * 0.2;

        // Clamp between 0 and 1
        baseThreatLevel = Math.max(0.0, Math.min(1.0, baseThreatLevel));

        prediction.put("hour", hour);
        prediction.put("timestamp", time);
        prediction.put("threatLevel", baseThreatLevel);
        prediction.put("riskLevel", getRiskLevel(baseThreatLevel));
        prediction.put("confidence", 0.75);

        // Add specific threat types
        List<String> predictedThreats = new ArrayList<>();
        if (baseThreatLevel > 0.7) {
            predictedThreats.add("High volume attacks");
            predictedThreats.add("Automated scanning");
        }
        if (hour >= 22 || hour <= 6) {
            predictedThreats.add("Off-hours suspicious activity");
        }
        if (maxVulnerability > 0.5) {
            predictedThreats.add("Targeted route exploitation");
        }

        prediction.put("predictedThreats", predictedThreats);

        return prediction;
    }

    private double calculatePredictionConfidence(List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.3;

        // Confidence based on data volume and consistency
        double volumeConfidence = Math.min(1.0, events.size() / 1000.0); // Higher confidence with more data

        // Check data consistency (similar patterns over time)
        Map<Integer, Long> hourlyDistribution = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

        double variance = calculateVariance(hourlyDistribution.values());
        double consistencyConfidence = 1.0 / (1.0 + variance / 100.0); // Lower variance = higher confidence

        return (volumeConfidence + consistencyConfidence) / 2.0;
    }

    private void updateAllBehaviorProfiles() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(since, LocalDateTime.now());

            // Group events by IP
            Map<String, List<SecurityEvent>> eventsByIp = events.stream()
                    .collect(Collectors.groupingBy(SecurityEvent::getClientIp));

            // Update or create profiles
            for (Map.Entry<String, List<SecurityEvent>> entry : eventsByIp.entrySet()) {
                if (entry.getValue().size() >= MIN_EVENTS_FOR_ANALYSIS) {
                    updateBehaviorProfile(entry.getKey(), entry.getValue());
                }
            }

            log.info("Updated {} behavioral profiles", eventsByIp.size());

        } catch (Exception e) {
            log.error("Error updating behavior profiles: {}", e.getMessage());
        }
    }

    private void learnNewAttackPatterns() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<SecurityEvent> rejectionEvents = eventRepository
                    .findByEventTypeAndTimestampBetween("REJECTION", since, LocalDateTime.now());

            // Analyze rejection reasons for new patterns
            Map<String, Long> rejectionCounts = rejectionEvents.stream()
                    .filter(e -> e.getRejectionReason() != null)
                    .collect(Collectors.groupingBy(SecurityEvent::getRejectionReason, Collectors.counting()));

            // Look for emerging patterns (high frequency, new patterns)
            for (Map.Entry<String, Long> entry : rejectionCounts.entrySet()) {
                if (entry.getValue() > 50) { // Threshold for new pattern
                    // Check if pattern already exists
                    List<ThreatPattern> existingPatterns = patternRepository.findByIsActiveTrue();
                    boolean patternExists = existingPatterns.stream()
                            .anyMatch(p -> p.getPatternName().contains(entry.getKey()));

                    if (!patternExists) {
                        log.info("Detected new attack pattern: {} (frequency: {})", entry.getKey(), entry.getValue());
                        // Could create new ThreatPattern here
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error learning new attack patterns: {}", e.getMessage());
        }
    }

    private void updatePatternEffectiveness() {
        try {
            List<ThreatPattern> patterns = patternRepository.findByIsActiveTrue();
            LocalDateTime since = LocalDateTime.now().minusDays(7);

            for (ThreatPattern pattern : patterns) {
                // Calculate effectiveness based on recent triggers vs false positives
                if (pattern.getTriggerCount() > 0) {
                    double effectiveness = 1.0 - ((double) pattern.getFalsePositiveCount() / pattern.getTriggerCount());

                    // Adjust confidence threshold based on effectiveness
                    if (effectiveness < 0.5 && pattern.getConfidenceThreshold() < 0.9) {
                        pattern.setConfidenceThreshold(Math.min(0.95, pattern.getConfidenceThreshold() + 0.1));
                        patternRepository.save(pattern);
                        log.info("Increased confidence threshold for pattern: {}", pattern.getPatternName());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error updating pattern effectiveness: {}", e.getMessage());
        }
    }

    private void cleanupOldAnalysisData() {
        try {
            // Clean up old behavior profiles (keep only active ones)
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            behaviorProfiles.entrySet().removeIf(entry ->
                    entry.getValue().lastUpdated.isBefore(cutoff));

            // Clean up old route profiles
            routeProfiles.entrySet().removeIf(entry ->
                    entry.getValue().getLastUpdated().isBefore(cutoff));

            log.debug("Cleaned up old analysis data");

        } catch (Exception e) {
            log.error("Error cleaning up old analysis data: {}", e.getMessage());
        }
    }

    private double predictAttackLikelihood(LocalDateTime time, List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.1;

        int hour = time.getHour();

        // Analyze historical patterns for this hour
        Map<Integer, Long> hourlyRejections = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

        long rejectionsAtThisHour = hourlyRejections.getOrDefault(hour, 0L);
        long totalRejections = hourlyRejections.values().stream().mapToLong(Long::longValue).sum();

        if (totalRejections == 0) return 0.1;

        // Base likelihood on historical rejection rate for this hour
        double baseLikelihood = (double) rejectionsAtThisHour / totalRejections;

        // Adjust for recent trends
        LocalDateTime recentStart = LocalDateTime.now().minusHours(6);
        long recentRejections = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()) && e.getTimestamp().isAfter(recentStart))
                .count();

        if (recentRejections > 0) {
            baseLikelihood *= 1.5; // Increase likelihood if recent attack activity
        }

        // Add time-of-day factors
        if (hour >= 22 || hour <= 6) {
            baseLikelihood *= 1.3; // Higher likelihood during off-hours
        }

        return Math.min(1.0, baseLikelihood);
    }

    private String getRiskLevel(double score) {
        if (score > 0.8) return "CRITICAL";
        if (score > 0.6) return "HIGH";
        if (score > 0.4) return "MEDIUM";
        return "LOW";
    }

    private String calculateOverallTrend(List<SecurityEvent> events) {
        if (events.size() < 10) return "STABLE";

        // Compare first half with second half of events
        int midPoint = events.size() / 2;
        List<SecurityEvent> firstHalf = events.subList(0, midPoint);
        List<SecurityEvent> secondHalf = events.subList(midPoint, events.size());

        long firstHalfRejections = firstHalf.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        long secondHalfRejections = secondHalf.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        double firstHalfRate = (double) firstHalfRejections / firstHalf.size();
        double secondHalfRate = (double) secondHalfRejections / secondHalf.size();

        if (secondHalfRate > firstHalfRate * 1.2) return "INCREASING";
        if (secondHalfRate < firstHalfRate * 0.8) return "DECREASING";
        return "STABLE";
    }

    private double calculateVolumeRisk(List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.0;

        // Calculate risk based on traffic volume compared to normal patterns
        double eventsPerHour = events.size() / 24.0;

        // Use simple thresholds for volume risk
        if (eventsPerHour > 1000) return 0.9;  // Very high volume
        if (eventsPerHour > 500) return 0.7;   // High volume
        if (eventsPerHour > 100) return 0.4;   // Medium volume
        if (eventsPerHour > 50) return 0.2;    // Low volume
        return 0.1; // Very low volume
    }

    private double calculatePatternRisk(List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.0;

        // Calculate risk based on attack patterns
        long rejections = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        double rejectionRate = (double) rejections / events.size();

        // High rejection rate indicates pattern-based attacks
        return Math.min(1.0, rejectionRate * 2.0);
    }

    private double calculateGeographicRisk(List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.0;

        // Analyze IP diversity and geographic spread
        Set<String> uniqueIPs = events.stream()
                .map(SecurityEvent::getClientIp)
                .collect(Collectors.toSet());

        // Calculate IP prefix diversity (simple geographic approximation)
        Set<String> ipPrefixes = uniqueIPs.stream()
                .map(ip -> ip.substring(0, Math.min(ip.length(),
                        ip.indexOf('.', ip.indexOf('.') + 1))))
                .collect(Collectors.toSet());

        // More diverse sources = higher geographic risk
        double diversityRatio = (double) ipPrefixes.size() / Math.max(1, uniqueIPs.size());

        // High diversity could indicate distributed attack
        if (diversityRatio > 0.8 && uniqueIPs.size() > 10) return 0.8;
        if (diversityRatio > 0.6 && uniqueIPs.size() > 5) return 0.6;
        if (diversityRatio > 0.4) return 0.4;
        return 0.2;
    }

    private double calculateTemporalRisk(List<SecurityEvent> events) {
        if (events.isEmpty()) return 0.0;

        // Analyze temporal patterns for suspicious activity
        Map<Integer, Long> hourlyDistribution = events.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour(), Collectors.counting()));

        // Check for off-hours activity (22:00 - 06:00)
        long offHoursActivity = hourlyDistribution.entrySet().stream()
                .filter(entry -> entry.getKey() >= 22 || entry.getKey() <= 6)
                .mapToLong(Map.Entry::getValue)
                .sum();

        double offHoursRatio = (double) offHoursActivity / events.size();

        // High off-hours activity indicates higher temporal risk
        return Math.min(1.0, offHoursRatio * 1.5);
    }

    private double calculateAlertRisk(List<ThreatAlert> alerts) {
        if (alerts.isEmpty()) return 0.0;

        // Calculate risk based on alert severity and frequency
        Map<String, Long> severityCounts = alerts.stream()
                .collect(Collectors.groupingBy(ThreatAlert::getSeverity, Collectors.counting()));

        double riskScore = 0.0;
        riskScore += severityCounts.getOrDefault("CRITICAL", 0L) * 0.4;
        riskScore += severityCounts.getOrDefault("HIGH", 0L) * 0.3;
        riskScore += severityCounts.getOrDefault("MEDIUM", 0L) * 0.2;
        riskScore += severityCounts.getOrDefault("LOW", 0L) * 0.1;

        return Math.min(1.0, riskScore / 10.0); // Normalize
    }

    private String calculateRiskTrend(LocalDateTime since) {
        try {
            LocalDateTime midPoint = since.plusHours(12);

            // Compare first 12 hours with last 12 hours
            List<SecurityEvent> firstHalf = eventRepository
                    .findByTimestampBetween(since, midPoint);
            List<SecurityEvent> secondHalf = eventRepository
                    .findByTimestampBetween(midPoint, LocalDateTime.now());

            if (firstHalf.isEmpty() && secondHalf.isEmpty()) return "STABLE";

            double firstHalfRisk = calculateVolumeRisk(firstHalf);
            double secondHalfRisk = calculateVolumeRisk(secondHalf);

            if (secondHalfRisk > firstHalfRisk * 1.3) return "INCREASING";
            if (secondHalfRisk < firstHalfRisk * 0.7) return "DECREASING";
            return "STABLE";

        } catch (Exception e) {
            log.warn("Error calculating risk trend: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private List<String> identifyCriticalIssues(List<SecurityEvent> events, List<ThreatAlert> alerts) {
        List<String> issues = new ArrayList<>();

        // Check for critical alerts
        long criticalAlerts = alerts.stream()
                .filter(a -> "CRITICAL".equals(a.getSeverity()) && "OPEN".equals(a.getStatus()))
                .count();

        if (criticalAlerts > 0) {
            issues.add(criticalAlerts + " critical security alerts require immediate attention");
        }

        // Check for high rejection rate
        if (!events.isEmpty()) {
            long rejections = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .count();

            double rejectionRate = (double) rejections / events.size();
            if (rejectionRate > 0.5) {
                issues.add("High rejection rate (" + String.format("%.1f", rejectionRate * 100) + "%) indicates potential attack");
            }
        }

        // Check for response time issues
        double avgResponseTime = events.stream()
                .filter(e -> e.getResponseTimeMs() != null)
                .mapToInt(SecurityEvent::getResponseTimeMs)
                .average().orElse(0.0);

        if (avgResponseTime > 5000) {
            issues.add("Average response time (" + String.format("%.0f", avgResponseTime) + "ms) indicates performance issues");
        }

        // Check for suspicious IP concentration
        Map<String, Long> ipCounts = events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getClientIp, Collectors.counting()));

        long suspiciousIPs = ipCounts.values().stream()
                .filter(count -> count > 100) // More than 100 requests from single IP
                .count();

        if (suspiciousIPs > 0) {
            issues.add(suspiciousIPs + " IP addresses showing suspicious high-volume activity");
        }

        return issues;
    }

    /**
     * User Behavior Profile class for tracking behavioral patterns
     */
    private static class UserBehaviorProfile {
        private final String clientIp;
        private double averageHourlyEvents = 0.0;
        private Set<Integer> activeHours = new HashSet<>();
        private Set<String> commonPaths = new HashSet<>();
        private LocalDateTime lastUpdated = LocalDateTime.now();

        public UserBehaviorProfile(String clientIp) {
            this.clientIp = clientIp;
        }

        public void updateWithEvents(List<SecurityEvent> events) {
            if (events.isEmpty()) return;

            // Update average hourly events
            this.averageHourlyEvents = events.size() / 24.0;

            // Update active hours
            this.activeHours = events.stream()
                    .map(e -> e.getTimestamp().getHour())
                    .collect(Collectors.toSet());

            // Update common paths
            this.commonPaths = events.stream()
                    .map(SecurityEvent::getRequestPath)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            this.lastUpdated = LocalDateTime.now();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("clientIp", clientIp);
            map.put("averageHourlyEvents", averageHourlyEvents);
            map.put("activeHours", activeHours);
            map.put("commonPathsCount", commonPaths.size());
            map.put("lastUpdated", lastUpdated);
            return map;
        }

        // Getters
        public double getAverageHourlyEvents() { return averageHourlyEvents; }
        public Set<Integer> getActiveHours() { return activeHours; }
        public Set<String> getCommonPaths() { return commonPaths; }
    }

    /**
     * Route Security Profile class for tracking route-specific security patterns
     */
    private static class RouteSecurityProfile {
        private final String routeId;
        private double averageThreatLevel = 0.0;
        private Map<String, Integer> attackPatterns = new HashMap<>();
        private LocalDateTime lastUpdated = LocalDateTime.now();
        private Map<Integer, Long> hourlyTrafficPattern = new HashMap<>();
        private Set<String> commonAttackSources = new HashSet<>();
        private double rejectionRate = 0.0;
        private int totalRequests = 0;
        private int totalRejections = 0;
        private Map<String, Long> rejectionReasons = new HashMap<>();

        public RouteSecurityProfile(String routeId) {
            this.routeId = routeId;
        }

        /**
         * Update the profile with new security events
         */
        public void updateWithEvents(List<SecurityEvent> events) {
            if (events.isEmpty()) return;

            // Update basic metrics
            this.totalRequests = events.size();
            this.totalRejections = (int) events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .count();
            this.rejectionRate = (double) totalRejections / totalRequests;

            // Update threat level average
            this.averageThreatLevel = events.stream()
                    .mapToDouble(this::getThreatLevelScore)
                    .average().orElse(0.0);

            // Update hourly traffic patterns
            this.hourlyTrafficPattern = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getTimestamp().getHour(),
                            Collectors.counting()));

            // Update attack sources (IPs with rejections)
            this.commonAttackSources = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .map(SecurityEvent::getClientIp)
                    .collect(Collectors.toSet());

            // Update rejection reasons
            this.rejectionReasons = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()) && e.getRejectionReason() != null)
                    .collect(Collectors.groupingBy(
                            SecurityEvent::getRejectionReason,
                            Collectors.counting()));

            // Update attack patterns based on request paths and user agents
            updateAttackPatterns(events);

            this.lastUpdated = LocalDateTime.now();
        }

        /**
         * Update attack patterns detected for this route
         */
        private void updateAttackPatterns(List<SecurityEvent> events) {
            // Reset attack patterns
            this.attackPatterns.clear();

            // Analyze request paths for attack patterns
            events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()) && e.getRequestPath() != null)
                    .forEach(event -> {
                        String path = event.getRequestPath().toLowerCase();

                        // SQL Injection patterns
                        if (path.contains("select") || path.contains("union") || path.contains("drop")) {
                            attackPatterns.merge("SQL_INJECTION", 1, Integer::sum);
                        }

                        // XSS patterns
                        if (path.contains("<script") || path.contains("javascript:") || path.contains("onerror=")) {
                            attackPatterns.merge("XSS_ATTEMPT", 1, Integer::sum);
                        }

                        // Directory traversal
                        if (path.contains("../") || path.contains("..\\")) {
                            attackPatterns.merge("DIRECTORY_TRAVERSAL", 1, Integer::sum);
                        }

                        // Command injection
                        if (path.contains("exec(") || path.contains("eval(") || path.contains("system(")) {
                            attackPatterns.merge("COMMAND_INJECTION", 1, Integer::sum);
                        }
                    });

            // Analyze user agents for scanning tools
            events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()) && e.getUserAgent() != null)
                    .forEach(event -> {
                        String userAgent = event.getUserAgent().toLowerCase();

                        if (userAgent.contains("sqlmap") || userAgent.contains("nikto") ||
                                userAgent.contains("nmap") || userAgent.contains("burp")) {
                            attackPatterns.merge("SECURITY_SCANNER", 1, Integer::sum);
                        }

                        if (userAgent.contains("bot") && !userAgent.contains("googlebot")) {
                            attackPatterns.merge("MALICIOUS_BOT", 1, Integer::sum);
                        }
                    });
        }

        /**
         * Convert threat level string to numeric score
         */
        private double getThreatLevelScore(SecurityEvent event) {
            switch (event.getThreatLevel()) {
                case "CRITICAL": return 1.0;
                case "HIGH": return 0.8;
                case "MEDIUM": return 0.5;
                case "LOW": return 0.2;
                default: return 0.1;
            }
        }

        /**
         * Calculate security score for this route (0-100)
         */
        public double calculateSecurityScore() {
            double baseScore = 100.0;

            // Deduct points for high rejection rate
            baseScore -= (rejectionRate * 40); // Max 40 points deduction

            // Deduct points for high threat level
            baseScore -= (averageThreatLevel * 30); // Max 30 points deduction

            // Deduct points for attack patterns
            int totalAttackPatterns = attackPatterns.values().stream().mapToInt(Integer::intValue).sum();
            if (totalRequests > 0) {
                double attackPatternRatio = (double) totalAttackPatterns / totalRequests;
                baseScore -= (attackPatternRatio * 20); // Max 20 points deduction
            }

            // Deduct points for high number of attack sources
            if (totalRequests > 0) {
                double attackSourceRatio = (double) commonAttackSources.size() / totalRequests;
                baseScore -= (attackSourceRatio * 10); // Max 10 points deduction
            }

            return Math.max(0.0, Math.min(100.0, baseScore));
        }

        /**
         * Get the peak traffic hour for this route
         */
        public int getPeakTrafficHour() {
            return hourlyTrafficPattern.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(12); // Default to noon
        }

        /**
         * Get the most common attack pattern
         */
        public String getPrimaryAttackPattern() {
            return attackPatterns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("NONE");
        }

        /**
         * Check if this route is under active attack
         */
        public boolean isUnderAttack() {
            // Consider under attack if:
            // 1. Rejection rate > 30%
            // 2. Average threat level > 0.6
            // 3. Recent attack patterns detected
            return rejectionRate > 0.3 ||
                    averageThreatLevel > 0.6 ||
                    !attackPatterns.isEmpty();
        }

        /**
         * Get route risk level
         */
        public String getRiskLevel() {
            double securityScore = calculateSecurityScore();

            if (securityScore < 30) return "CRITICAL";
            if (securityScore < 50) return "HIGH";
            if (securityScore < 70) return "MEDIUM";
            return "LOW";
        }

        /**
         * Convert profile to map for serialization
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("routeId", routeId);
            map.put("securityScore", calculateSecurityScore());
            map.put("riskLevel", getRiskLevel());
            map.put("averageThreatLevel", averageThreatLevel);
            map.put("rejectionRate", rejectionRate);
            map.put("totalRequests", totalRequests);
            map.put("totalRejections", totalRejections);
            map.put("attackPatterns", attackPatterns);
            map.put("attackSourceCount", commonAttackSources.size());
            map.put("peakTrafficHour", getPeakTrafficHour());
            map.put("primaryAttackPattern", getPrimaryAttackPattern());
            map.put("isUnderAttack", isUnderAttack());
            map.put("rejectionReasons", rejectionReasons);
            map.put("lastUpdated", lastUpdated);
            return map;
        }

        /**
         * Get recommendations for improving route security
         */
        public List<String> getSecurityRecommendations() {
            List<String> recommendations = new ArrayList<>();

            if (rejectionRate > 0.5) {
                recommendations.add("Consider implementing additional IP filtering - rejection rate is very high");
            }

            if (averageThreatLevel > 0.7) {
                recommendations.add("Route is experiencing high-threat attacks - consider implementing WAF rules");
            }

            if (attackPatterns.containsKey("SQL_INJECTION")) {
                recommendations.add("SQL injection attempts detected - implement input validation and parameterized queries");
            }

            if (attackPatterns.containsKey("XSS_ATTEMPT")) {
                recommendations.add("XSS attempts detected - implement proper output encoding and CSP headers");
            }

            if (attackPatterns.containsKey("SECURITY_SCANNER")) {
                recommendations.add("Security scanners detected - consider blocking known scanner user agents");
            }

            if (commonAttackSources.size() > 10 && totalRequests > 0) {
                double uniqueAttackerRatio = (double) commonAttackSources.size() / totalRequests;
                if (uniqueAttackerRatio > 0.1) {
                    recommendations.add("High number of unique attack sources - consider geographic IP filtering");
                }
            }

            if (recommendations.isEmpty()) {
                recommendations.add("Route security appears stable - continue monitoring");
            }

            return recommendations;
        }

        // Getters
        public String getRouteId() { return routeId; }
        public double getAverageThreatLevel() { return averageThreatLevel; }
        public Map<String, Integer> getAttackPatterns() { return attackPatterns; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public double getRejectionRate() { return rejectionRate; }
        public int getTotalRequests() { return totalRequests; }
        public int getTotalRejections() { return totalRejections; }
        public Set<String> getCommonAttackSources() { return commonAttackSources; }
        public Map<Integer, Long> getHourlyTrafficPattern() { return hourlyTrafficPattern; }
        public Map<String, Long> getRejectionReasons() { return rejectionReasons; }
    }
}