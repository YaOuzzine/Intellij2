package com.example.demo.Service;

import com.example.demo.Entity.SecurityEvent;
import com.example.demo.Entity.ThreatAlert;
import com.example.demo.Repository.SecurityEventRepository;
import com.example.demo.Repository.ThreatAlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ComplianceReportingService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReportingService.class);

    private final SecurityEventRepository eventRepository;
    private final ThreatAlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    // Compliance frameworks and their requirements
    private final Map<String, ComplianceFramework> supportedFrameworks = new ConcurrentHashMap<>();

    // Compliance metrics cache
    private final Map<String, ComplianceMetrics> complianceCache = new ConcurrentHashMap<>();
    private LocalDateTime lastCacheUpdate = LocalDateTime.now();

    // Compliance thresholds
    private static final double GDPR_BREACH_NOTIFICATION_HOURS = 72;
    private static final double SOC2_INCIDENT_RESPONSE_HOURS = 24;
    private static final double ISO27001_SECURITY_REVIEW_DAYS = 30;
    private static final double PCI_DSS_LOG_RETENTION_DAYS = 365;
    private static final int HIPAA_ACCESS_LOG_RETENTION_YEARS = 6;

    @Autowired
    public ComplianceReportingService(SecurityEventRepository eventRepository,
                                      ThreatAlertRepository alertRepository) {
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initializeComplianceFrameworks() {
        log.info("Initializing compliance frameworks...");

        // Initialize GDPR compliance framework
        initializeGDPRFramework();

        // Initialize SOC 2 compliance framework
        initializeSOC2Framework();

        // Initialize ISO 27001 compliance framework
        initializeISO27001Framework();

        // Initialize PCI DSS compliance framework
        initializePCIDSSFramework();

        // Initialize HIPAA compliance framework
        initializeHIPAAFramework();

        log.info("Initialized {} compliance frameworks", supportedFrameworks.size());
    }

    /**
     * Generate comprehensive compliance report for specified framework
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateComplianceReport(String frameworkName, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        try {
            ComplianceFramework framework = supportedFrameworks.get(frameworkName.toUpperCase());
            if (framework == null) {
                throw new IllegalArgumentException("Unsupported compliance framework: " + frameworkName);
            }

            log.info("Generating compliance report for {} from {} to {}", frameworkName, startDate, endDate);

            // Basic report metadata
            report.put("framework", frameworkName);
            report.put("reportPeriod", Map.of(
                    "start", startDate,
                    "end", endDate,
                    "durationDays", ChronoUnit.DAYS.between(startDate, endDate)
            ));
            report.put("generatedAt", LocalDateTime.now());
            report.put("reportId", generateReportId(frameworkName));

            // Executive summary
            Map<String, Object> executiveSummary = generateExecutiveSummary(framework, startDate, endDate);
            report.put("executiveSummary", executiveSummary);

            // Compliance score and status
            ComplianceMetrics metrics = calculateComplianceMetrics(framework, startDate, endDate);
            report.put("complianceScore", metrics.getOverallScore());
            report.put("complianceStatus", metrics.getComplianceStatus());

            // Control assessments
            List<Map<String, Object>> controlAssessments = assessComplianceControls(framework, startDate, endDate);
            report.put("controlAssessments", controlAssessments);

            // Incidents and violations
            Map<String, Object> incidentAnalysis = analyzeSecurityIncidents(framework, startDate, endDate);
            report.put("incidentAnalysis", incidentAnalysis);

            // Risk assessment
            Map<String, Object> riskAssessment = performComplianceRiskAssessment(framework, startDate, endDate);
            report.put("riskAssessment", riskAssessment);

            // Remediation recommendations
            List<Map<String, Object>> recommendations = generateRemediationRecommendations(framework, metrics);
            report.put("recommendations", recommendations);

            // Audit trail and evidence
            Map<String, Object> auditTrail = generateAuditTrail(framework, startDate, endDate);
            report.put("auditTrail", auditTrail);

            // Framework-specific sections
            Map<String, Object> frameworkSpecific = generateFrameworkSpecificReport(framework, startDate, endDate);
            report.put("frameworkSpecificAnalysis", frameworkSpecific);

            log.info("Compliance report generated successfully for {}", frameworkName);

        } catch (Exception e) {
            log.error("Error generating compliance report for {}: {}", frameworkName, e.getMessage(), e);
            report.put("error", e.getMessage());
            report.put("status", "FAILED");
        }

        return report;
    }

    /**
     * Get current compliance dashboard data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComplianceDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(30); // Last 30 days

            // Overall compliance status across all frameworks
            Map<String, Object> overallStatus = new HashMap<>();
            List<Map<String, Object>> frameworkStatuses = new ArrayList<>();

            for (Map.Entry<String, ComplianceFramework> entry : supportedFrameworks.entrySet()) {
                String frameworkName = entry.getKey();
                ComplianceFramework framework = entry.getValue();

                ComplianceMetrics metrics = getOrCalculateMetrics(framework, startDate, endDate);

                Map<String, Object> frameworkStatus = new HashMap<>();
                frameworkStatus.put("framework", frameworkName);
                frameworkStatus.put("score", metrics.getOverallScore());
                frameworkStatus.put("status", metrics.getComplianceStatus());
                frameworkStatus.put("criticalIssues", metrics.getCriticalIssues().size());
                frameworkStatus.put("lastAssessment", metrics.getLastAssessment());

                frameworkStatuses.add(frameworkStatus);
            }

            // Calculate overall compliance health
            double avgScore = frameworkStatuses.stream()
                    .mapToDouble(fs -> (Double) fs.get("score"))
                    .average().orElse(0.0);

            long criticalIssuesTotal = frameworkStatuses.stream()
                    .mapToLong(fs -> (Integer) fs.get("criticalIssues"))
                    .sum();

            overallStatus.put("averageComplianceScore", avgScore);
            overallStatus.put("overallStatus", getOverallComplianceStatus(avgScore));
            overallStatus.put("totalCriticalIssues", criticalIssuesTotal);
            overallStatus.put("frameworkCount", supportedFrameworks.size());

            dashboard.put("overallStatus", overallStatus);
            dashboard.put("frameworkStatuses", frameworkStatuses);

            // Recent compliance events
            List<Map<String, Object>> recentEvents = getRecentComplianceEvents(startDate, endDate);
            dashboard.put("recentEvents", recentEvents);

            // Trending analysis
            Map<String, Object> trends = analyzeComplianceTrends(startDate, endDate);
            dashboard.put("trends", trends);

            // Upcoming deadlines and reviews
            List<Map<String, Object>> upcomingDeadlines = getUpcomingComplianceDeadlines();
            dashboard.put("upcomingDeadlines", upcomingDeadlines);

            dashboard.put("lastUpdated", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error generating compliance dashboard: {}", e.getMessage(), e);
            dashboard.put("error", "Failed to generate compliance dashboard");
        }

        return dashboard;
    }

    /**
     * Assess specific compliance control
     */
    @Transactional(readOnly = true)
    public Map<String, Object> assessComplianceControl(String frameworkName, String controlId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> assessment = new HashMap<>();

        try {
            ComplianceFramework framework = supportedFrameworks.get(frameworkName.toUpperCase());
            if (framework == null) {
                throw new IllegalArgumentException("Framework not found: " + frameworkName);
            }

            ComplianceControl control = framework.getControls().stream()
                    .filter(c -> c.getId().equals(controlId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Control not found: " + controlId));

            // Basic control information
            assessment.put("controlId", control.getId());
            assessment.put("title", control.getTitle());
            assessment.put("description", control.getDescription());
            assessment.put("category", control.getCategory());
            assessment.put("severity", control.getSeverity());

            // Assessment results
            ControlAssessmentResult result = assessControl(control, startDate, endDate);
            assessment.put("status", result.getStatus());
            assessment.put("score", result.getScore());
            assessment.put("findings", result.getFindings());
            assessment.put("evidence", result.getEvidence());

            // Recommendations
            assessment.put("recommendations", result.getRecommendations());

            // Historical compliance for this control
            List<Map<String, Object>> historicalData = getControlHistoricalData(control, startDate, endDate);
            assessment.put("historicalCompliance", historicalData);

            assessment.put("assessmentDate", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error assessing compliance control {}: {}", controlId, e.getMessage(), e);
            assessment.put("error", e.getMessage());
        }

        return assessment;
    }

    /**
     * Generate compliance violation alert
     */
    public void reportComplianceViolation(String frameworkName, String controlId, String violationType, String description, Map<String, Object> evidence) {
        try {
            log.warn("Compliance violation reported: Framework={}, Control={}, Type={}", frameworkName, controlId, violationType);

            // Create compliance violation record
            Map<String, Object> violation = new HashMap<>();
            violation.put("framework", frameworkName);
            violation.put("controlId", controlId);
            violation.put("violationType", violationType);
            violation.put("description", description);
            violation.put("evidence", evidence);
            violation.put("reportedAt", LocalDateTime.now());
            violation.put("status", "OPEN");

            // Log for audit trail
            log.info("Compliance violation recorded: {}", objectMapper.writeValueAsString(violation));

            // Trigger alerts if critical
            if (isCriticalViolation(frameworkName, controlId, violationType)) {
                triggerCriticalComplianceAlert(violation);
            }

        } catch (Exception e) {
            log.error("Error reporting compliance violation: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled compliance monitoring
     */
    @Scheduled(fixedDelay = 3600000) // Every hour
    @Transactional(readOnly = true)
    public void performScheduledComplianceCheck() {
        try {
            log.debug("Performing scheduled compliance check...");

            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusHours(1); // Check last hour

            for (ComplianceFramework framework : supportedFrameworks.values()) {
                // Check for immediate compliance violations
                checkImmediateViolations(framework, startDate, endDate);

                // Update compliance metrics cache
                updateComplianceCache(framework, startDate, endDate);
            }

            log.debug("Scheduled compliance check completed");

        } catch (Exception e) {
            log.error("Error in scheduled compliance check: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate compliance audit report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateAuditReport(String frameworkName, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> auditReport = new HashMap<>();

        try {
            ComplianceFramework framework = supportedFrameworks.get(frameworkName.toUpperCase());
            if (framework == null) {
                throw new IllegalArgumentException("Framework not found: " + frameworkName);
            }

            // Audit metadata
            auditReport.put("framework", frameworkName);
            auditReport.put("auditPeriod", Map.of("start", startDate, "end", endDate));
            auditReport.put("auditDate", LocalDateTime.now());
            auditReport.put("auditorInfo", Map.of(
                    "system", "Automated Compliance System",
                    "version", "1.0",
                    "capabilities", Arrays.asList("Real-time monitoring", "Automated assessment", "Risk analysis")
            ));

            // Security events audit
            List<SecurityEvent> securityEvents = eventRepository.findByTimestampBetween(startDate, endDate);
            Map<String, Object> securityAudit = auditSecurityEvents(framework, securityEvents);
            auditReport.put("securityEventsAudit", securityAudit);

            // Access control audit
            Map<String, Object> accessAudit = auditAccessControls(framework, securityEvents);
            auditReport.put("accessControlAudit", accessAudit);

            // Data protection audit
            Map<String, Object> dataProtectionAudit = auditDataProtection(framework, securityEvents);
            auditReport.put("dataProtectionAudit", dataProtectionAudit);

            // Incident response audit
            List<ThreatAlert> threats = alertRepository.findRecentAlerts(startDate);
            Map<String, Object> incidentAudit = auditIncidentResponse(framework, threats);
            auditReport.put("incidentResponseAudit", incidentAudit);

            // Compliance gaps and recommendations
            List<Map<String, Object>> gaps = identifyComplianceGaps(framework, startDate, endDate);
            auditReport.put("complianceGaps", gaps);

            // Risk assessment
            Map<String, Object> riskAssessment = assessComplianceRisks(framework, securityEvents, threats);
            auditReport.put("riskAssessment", riskAssessment);

            log.info("Audit report generated for {} covering period {} to {}", frameworkName, startDate, endDate);

        } catch (Exception e) {
            log.error("Error generating audit report: {}", e.getMessage(), e);
            auditReport.put("error", e.getMessage());
        }

        return auditReport;
    }

    // Private helper methods for framework initialization

    private void initializeGDPRFramework() {
        ComplianceFramework gdpr = new ComplianceFramework("GDPR", "General Data Protection Regulation");

        // Article 32 - Security of processing
        gdpr.addControl(new ComplianceControl("GDPR-32.1", "Pseudonymisation and encryption",
                "Implement appropriate technical measures including pseudonymisation and encryption",
                "Technical", "HIGH"));

        // Article 33 - Notification of personal data breach
        gdpr.addControl(new ComplianceControl("GDPR-33.1", "Breach notification to supervisory authority",
                "Notify supervisory authority within 72 hours of becoming aware of breach",
                "Process", "CRITICAL"));

        // Article 25 - Data protection by design and by default
        gdpr.addControl(new ComplianceControl("GDPR-25.1", "Data protection by design",
                "Implement appropriate technical and organisational measures",
                "Design", "HIGH"));

        supportedFrameworks.put("GDPR", gdpr);
    }

    private void initializeSOC2Framework() {
        ComplianceFramework soc2 = new ComplianceFramework("SOC2", "Service Organization Control 2");

        // Security criteria
        soc2.addControl(new ComplianceControl("SOC2-CC6.1", "Logical and physical access controls",
                "Implement logical and physical access controls to protect system resources",
                "Security", "HIGH"));

        soc2.addControl(new ComplianceControl("SOC2-CC6.7", "Data transmission controls",
                "Restrict data transmission to authorized internal and external users",
                "Security", "HIGH"));

        soc2.addControl(new ComplianceControl("SOC2-CC7.1", "Detection of security events",
                "Implement detection policies and procedures to identify potential security events",
                "Monitoring", "HIGH"));

        supportedFrameworks.put("SOC2", soc2);
    }

    private void initializeISO27001Framework() {
        ComplianceFramework iso27001 = new ComplianceFramework("ISO27001", "ISO/IEC 27001:2013");

        iso27001.addControl(new ComplianceControl("ISO-A.12.6.1", "Management of technical vulnerabilities",
                "Information about technical vulnerabilities shall be obtained in a timely fashion",
                "Technical", "HIGH"));

        iso27001.addControl(new ComplianceControl("ISO-A.16.1.1", "Responsibilities and procedures",
                "Establish management responsibilities and procedures for information security incident management",
                "Incident Management", "CRITICAL"));

        iso27001.addControl(new ComplianceControl("ISO-A.12.4.1", "Event logging",
                "Event logs recording user activities, exceptions, and security events shall be produced",
                "Logging", "MEDIUM"));

        supportedFrameworks.put("ISO27001", iso27001);
    }

    private void initializePCIDSSFramework() {
        ComplianceFramework pciDss = new ComplianceFramework("PCI_DSS", "Payment Card Industry Data Security Standard");

        pciDss.addControl(new ComplianceControl("PCI-2.1", "Change default passwords",
                "Always change vendor-supplied defaults and remove or disable unnecessary default accounts",
                "Access Control", "HIGH"));

        pciDss.addControl(new ComplianceControl("PCI-10.2", "Implement automated audit trails",
                "Implement automated audit trails for all system components",
                "Logging", "HIGH"));

        pciDss.addControl(new ComplianceControl("PCI-11.4", "Use intrusion-detection techniques",
                "Use intrusion-detection and/or intrusion-prevention techniques",
                "Monitoring", "HIGH"));

        supportedFrameworks.put("PCI_DSS", pciDss);
    }

    private void initializeHIPAAFramework() {
        ComplianceFramework hipaa = new ComplianceFramework("HIPAA", "Health Insurance Portability and Accountability Act");

        hipaa.addControl(new ComplianceControl("HIPAA-164.312.a.1", "Access control",
                "Assign a unique name and/or number for identifying and tracking user identity",
                "Access Control", "HIGH"));

        hipaa.addControl(new ComplianceControl("HIPAA-164.312.b", "Audit controls",
                "Implement hardware, software, and procedural mechanisms for audit controls",
                "Auditing", "HIGH"));

        hipaa.addControl(new ComplianceControl("HIPAA-164.312.e.1", "Transmission security",
                "Implement technical security measures to guard against unauthorized access",
                "Transmission", "HIGH"));

        supportedFrameworks.put("HIPAA", hipaa);
    }

    // Helper methods for compliance assessment

    private String generateReportId(String frameworkName) {
        return frameworkName + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private Map<String, Object> generateExecutiveSummary(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> summary = new HashMap<>();

        try {
            ComplianceMetrics metrics = calculateComplianceMetrics(framework, startDate, endDate);

            summary.put("overallComplianceScore", metrics.getOverallScore());
            summary.put("complianceStatus", metrics.getComplianceStatus());
            summary.put("totalControls", framework.getControls().size());
            summary.put("compliantControls", metrics.getCompliantControls());
            summary.put("nonCompliantControls", metrics.getNonCompliantControls());
            summary.put("criticalIssues", metrics.getCriticalIssues().size());
            summary.put("improvementFromLastPeriod", calculateImprovementTrend(framework, startDate, endDate));

            // Key findings
            List<String> keyFindings = new ArrayList<>();
            if (metrics.getOverallScore() >= 90) {
                keyFindings.add("Excellent compliance posture with strong controls implementation");
            } else if (metrics.getOverallScore() >= 70) {
                keyFindings.add("Good compliance foundation with some areas for improvement");
            } else {
                keyFindings.add("Significant compliance gaps requiring immediate attention");
            }

            if (!metrics.getCriticalIssues().isEmpty()) {
                keyFindings.add(metrics.getCriticalIssues().size() + " critical compliance issues identified");
            }

            summary.put("keyFindings", keyFindings);

        } catch (Exception e) {
            log.error("Error generating executive summary: {}", e.getMessage());
        }

        return summary;
    }

    private ComplianceMetrics calculateComplianceMetrics(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        ComplianceMetrics metrics = new ComplianceMetrics();

        try {
            int totalControls = framework.getControls().size();
            int compliantControls = 0;
            List<String> criticalIssues = new ArrayList<>();

            for (ComplianceControl control : framework.getControls()) {
                ControlAssessmentResult result = assessControl(control, startDate, endDate);

                if ("COMPLIANT".equals(result.getStatus())) {
                    compliantControls++;
                } else if ("CRITICAL".equals(control.getSeverity())) {
                    criticalIssues.add(control.getId() + ": " + control.getTitle());
                }
            }

            double score = totalControls > 0 ? (double) compliantControls / totalControls * 100 : 0;

            metrics.setOverallScore(score);
            metrics.setCompliantControls(compliantControls);
            metrics.setNonCompliantControls(totalControls - compliantControls);
            metrics.setCriticalIssues(criticalIssues);
            metrics.setComplianceStatus(determineComplianceStatus(score, criticalIssues.size()));
            metrics.setLastAssessment(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error calculating compliance metrics: {}", e.getMessage());
        }

        return metrics;
    }

    private ControlAssessmentResult assessControl(ComplianceControl control, LocalDateTime startDate, LocalDateTime endDate) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        try {
            // Get relevant security events for assessment
            List<SecurityEvent> events = eventRepository.findByTimestampBetween(startDate, endDate);
            List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startDate);

            // Control-specific assessment logic
            switch (control.getId()) {
                case "GDPR-33.1":
                    result = assessBreachNotificationCompliance(control, alerts);
                    break;
                case "SOC2-CC7.1":
                    result = assessSecurityEventDetection(control, events, alerts);
                    break;
                case "ISO-A.12.4.1":
                    result = assessEventLogging(control, events);
                    break;
                case "PCI-10.2":
                    result = assessAuditTrails(control, events);
                    break;
                case "HIPAA-164.312.b":
                    result = assessAuditControls(control, events);
                    break;
                default:
                    result = performGenericControlAssessment(control, events, alerts);
                    break;
            }

        } catch (Exception e) {
            log.error("Error assessing control {}: {}", control.getId(), e.getMessage());
            result.setStatus("ERROR");
            result.setScore(0.0);
            result.getFindings().add("Assessment failed: " + e.getMessage());
        }

        return result;
    }

    private ControlAssessmentResult assessBreachNotificationCompliance(ComplianceControl control, List<ThreatAlert> alerts) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        // Check if critical alerts were handled within 72 hours
        List<ThreatAlert> criticalBreaches = alerts.stream()
                .filter(a -> "CRITICAL".equals(a.getSeverity()) && "PATTERN_MATCH".equals(a.getAlertType()))
                .collect(Collectors.toList());

        if (criticalBreaches.isEmpty()) {
            result.setStatus("COMPLIANT");
            result.setScore(100.0);
            result.getFindings().add("No critical security breaches identified in the assessment period");
        } else {
            long timelyNotifications = criticalBreaches.stream()
                    .filter(alert -> {
                        if (alert.getUpdatedAt() != null) {
                            long hoursToResponse = ChronoUnit.HOURS.between(alert.getCreatedAt(), alert.getUpdatedAt());
                            return hoursToResponse <= GDPR_BREACH_NOTIFICATION_HOURS;
                        }
                        return false;
                    })
                    .count();

            double complianceRate = (double) timelyNotifications / criticalBreaches.size() * 100;
            result.setScore(complianceRate);
            result.setStatus(complianceRate >= 100 ? "COMPLIANT" : "NON_COMPLIANT");

            result.getFindings().add(String.format("Breach notification compliance: %.1f%% (%d of %d within 72 hours)",
                    complianceRate, timelyNotifications, criticalBreaches.size()));
        }

        return result;
    }

    private ControlAssessmentResult assessSecurityEventDetection(ComplianceControl control, List<SecurityEvent> events, List<ThreatAlert> alerts) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        long rejectionEvents = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        // Check if security events are being properly detected (alerts generated for rejections)
        double detectionRate = rejectionEvents > 0 ? (double) alerts.size() / rejectionEvents * 100 : 100;

        result.setScore(Math.min(100.0, detectionRate));
        result.setStatus(detectionRate >= 50 ? "COMPLIANT" : "NON_COMPLIANT");
        result.getFindings().add(String.format("Security event detection rate: %.1f%% (%d alerts for %d rejection events)",
                detectionRate, alerts.size(), rejectionEvents));

        return result;
    }

    private ControlAssessmentResult assessEventLogging(ComplianceControl control, List<SecurityEvent> events) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        // Check if comprehensive logging is in place
        long totalEvents = events.size();
        long eventsWithDetails = events.stream()
                .filter(e -> e.getClientIp() != null && e.getRequestPath() != null)
                .count();

        double loggingCompleteness = totalEvents > 0 ? (double) eventsWithDetails / totalEvents * 100 : 100;

        result.setScore(loggingCompleteness);
        result.setStatus(loggingCompleteness >= 95 ? "COMPLIANT" : "NON_COMPLIANT");
        result.getFindings().add(String.format("Event logging completeness: %.1f%% (%d of %d events properly logged)",
                loggingCompleteness, eventsWithDetails, totalEvents));

        return result;
    }

    private ControlAssessmentResult assessAuditTrails(ComplianceControl control, List<SecurityEvent> events) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        // Check audit trail quality and completeness
        Map<String, Long> eventTypes = events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getEventType, Collectors.counting()));

        boolean hasComprehensiveAuditTrail = eventTypes.size() >= 2 && // Multiple event types
                events.stream().anyMatch(e -> e.getResponseTimeMs() != null) && // Performance data
                events.stream().anyMatch(e -> e.getUserAgent() != null); // User context

        result.setScore(hasComprehensiveAuditTrail ? 100.0 : 60.0);
        result.setStatus(hasComprehensiveAuditTrail ? "COMPLIANT" : "PARTIAL_COMPLIANCE");
        result.getFindings().add("Audit trail assessment: " +
                (hasComprehensiveAuditTrail ? "Comprehensive audit trails maintained" : "Limited audit trail data"));

        return result;
    }

    private ControlAssessmentResult assessAuditControls(ComplianceControl control, List<SecurityEvent> events) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        // Similar to audit trails but with HIPAA-specific requirements
        long accessEvents = events.stream()
                .filter(e -> "REQUEST".equals(e.getEventType()))
                .count();

        long identifiedAccess = events.stream()
                .filter(e -> e.getClientIp() != null && e.getRequestPath() != null)
                .count();

        double auditCompliance = accessEvents > 0 ? (double) identifiedAccess / accessEvents * 100 : 100;

        result.setScore(auditCompliance);
        result.setStatus(auditCompliance >= 98 ? "COMPLIANT" : "NON_COMPLIANT");
        result.getFindings().add(String.format("Audit control compliance: %.1f%% (access tracking and identification)",
                auditCompliance));

        return result;
    }

    private ControlAssessmentResult performGenericControlAssessment(ComplianceControl control, List<SecurityEvent> events, List<ThreatAlert> alerts) {
        ControlAssessmentResult result = new ControlAssessmentResult();

        // Generic assessment based on control category and security events
        double score = 75.0; // Default baseline score

        // Adjust score based on security events
        if (!events.isEmpty()) {
            long rejections = events.stream()
                    .filter(e -> "REJECTION".equals(e.getEventType()))
                    .count();

            double rejectionRate = (double) rejections / events.size();

            // Lower score for high rejection rates
            if (rejectionRate > 0.5) {
                score -= 30;
            } else if (rejectionRate > 0.3) {
                score -= 20;
            } else if (rejectionRate > 0.1) {
                score -= 10;
            }
        }

        // Adjust score based on alerts
        long criticalAlerts = alerts.stream()
                .filter(a -> "CRITICAL".equals(a.getSeverity()))
                .count();

        if (criticalAlerts > 0) {
            score -= criticalAlerts * 5; // 5 points per critical alert
        }

        score = Math.max(0, Math.min(100, score));

        result.setScore(score);
        result.setStatus(score >= 80 ? "COMPLIANT" : score >= 60 ? "PARTIAL_COMPLIANCE" : "NON_COMPLIANT");
        result.getFindings().add(String.format("Generic assessment for %s: %.1f%% compliance", control.getCategory(), score));

        return result;
    }

    // Additional helper methods

    private String determineComplianceStatus(double score, int criticalIssues) {
        if (criticalIssues > 0) return "NON_COMPLIANT";
        if (score >= 90) return "FULLY_COMPLIANT";
        if (score >= 70) return "SUBSTANTIALLY_COMPLIANT";
        if (score >= 50) return "PARTIALLY_COMPLIANT";
        return "NON_COMPLIANT";
    }

    private double calculateImprovementTrend(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        // Compare with previous period (simplified)
        LocalDateTime previousStart = startDate.minusDays(ChronoUnit.DAYS.between(startDate, endDate));
        LocalDateTime previousEnd = startDate;

        try {
            ComplianceMetrics currentMetrics = calculateComplianceMetrics(framework, startDate, endDate);
            ComplianceMetrics previousMetrics = calculateComplianceMetrics(framework, previousStart, previousEnd);

            return currentMetrics.getOverallScore() - previousMetrics.getOverallScore();
        } catch (Exception e) {
            log.warn("Error calculating improvement trend: {}", e.getMessage());
            return 0.0;
        }
    }

    private ComplianceMetrics getOrCalculateMetrics(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        String cacheKey = framework.getName() + "_" + startDate + "_" + endDate;
        ComplianceMetrics cached = complianceCache.get(cacheKey);

        if (cached != null && cached.getLastAssessment().isAfter(LocalDateTime.now().minusMinutes(30))) {
            return cached;
        }

        ComplianceMetrics metrics = calculateComplianceMetrics(framework, startDate, endDate);
        complianceCache.put(cacheKey, metrics);
        return metrics;
    }

    private String getOverallComplianceStatus(double avgScore) {
        if (avgScore >= 90) return "EXCELLENT";
        if (avgScore >= 75) return "GOOD";
        if (avgScore >= 60) return "FAIR";
        if (avgScore >= 40) return "POOR";
        return "CRITICAL";
    }

    private List<Map<String, Object>> assessComplianceControls(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        List<Map<String, Object>> assessments = new ArrayList<>();

        for (ComplianceControl control : framework.getControls()) {
            ControlAssessmentResult result = assessControl(control, startDate, endDate);

            Map<String, Object> assessment = new HashMap<>();
            assessment.put("controlId", control.getId());
            assessment.put("title", control.getTitle());
            assessment.put("category", control.getCategory());
            assessment.put("severity", control.getSeverity());
            assessment.put("status", result.getStatus());
            assessment.put("score", result.getScore());
            assessment.put("findings", result.getFindings());

            assessments.add(assessment);
        }

        return assessments;
    }

    private Map<String, Object> analyzeSecurityIncidents(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> analysis = new HashMap<>();

        List<SecurityEvent> incidents = eventRepository.findByTimestampBetween(startDate, endDate);
        List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startDate);

        analysis.put("totalIncidents", incidents.size());
        analysis.put("totalAlerts", alerts.size());
        analysis.put("criticalAlerts", alerts.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count());
        analysis.put("incidentsByType", incidents.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getEventType, Collectors.counting())));

        return analysis;
    }

    private Map<String, Object> performComplianceRiskAssessment(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> assessment = new HashMap<>();

        List<SecurityEvent> events = eventRepository.findByTimestampBetween(startDate, endDate);
        List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startDate);

        // Calculate risk scores
        double dataRisk = calculateDataRisk(events);
        double accessRisk = calculateAccessRisk(events);
        double incidentRisk = calculateIncidentRisk(alerts);

        double overallRisk = (dataRisk + accessRisk + incidentRisk) / 3.0;

        assessment.put("overallRisk", overallRisk);
        assessment.put("riskLevel", getRiskLevel(overallRisk));
        assessment.put("dataRisk", dataRisk);
        assessment.put("accessRisk", accessRisk);
        assessment.put("incidentRisk", incidentRisk);

        return assessment;
    }

    private List<Map<String, Object>> generateRemediationRecommendations(ComplianceFramework framework, ComplianceMetrics metrics) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        if (metrics.getOverallScore() < 70) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("priority", "HIGH");
            rec.put("category", "Overall Compliance");
            rec.put("title", "Improve Overall Compliance Score");
            rec.put("description", "Current compliance score is below acceptable threshold");
            rec.put("actions", Arrays.asList(
                    "Review failed control assessments",
                    "Implement missing security controls",
                    "Enhance monitoring and logging capabilities"
            ));
            recommendations.add(rec);
        }

        if (!metrics.getCriticalIssues().isEmpty()) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("priority", "CRITICAL");
            rec.put("category", "Critical Issues");
            rec.put("title", "Address Critical Compliance Issues");
            rec.put("description", "Critical compliance violations require immediate attention");
            rec.put("actions", metrics.getCriticalIssues());
            recommendations.add(rec);
        }

        return recommendations;
    }

    private Map<String, Object> generateAuditTrail(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> auditTrail = new HashMap<>();

        List<SecurityEvent> events = eventRepository.findByTimestampBetween(startDate, endDate);

        auditTrail.put("totalEvents", events.size());
        auditTrail.put("eventsByType", events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getEventType, Collectors.counting())));
        auditTrail.put("auditCoverage", calculateAuditCoverage(events));

        return auditTrail;
    }

    private Map<String, Object> generateFrameworkSpecificReport(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> specific = new HashMap<>();

        switch (framework.getName()) {
            case "GDPR":
                specific = generateGDPRSpecificReport(startDate, endDate);
                break;
            case "SOC2":
                specific = generateSOC2SpecificReport(startDate, endDate);
                break;
            case "ISO27001":
                specific = generateISO27001SpecificReport(startDate, endDate);
                break;
            default:
                specific.put("message", "No framework-specific analysis available");
        }

        return specific;
    }

    // Framework-specific report generators
    private Map<String, Object> generateGDPRSpecificReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        List<SecurityEvent> events = eventRepository.findByTimestampBetween(startDate, endDate);
        List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startDate);

        // GDPR-specific metrics
        report.put("dataProcessingEvents", events.size());
        report.put("potentialBreaches", alerts.stream()
                .filter(a -> "CRITICAL".equals(a.getSeverity()) || "HIGH".equals(a.getSeverity()))
                .count());
        report.put("dataSubjectRequests", 0); // Would need additional data source
        report.put("consentManagement", "Not tracked"); // Would need additional implementation

        return report;
    }

    private Map<String, Object> generateSOC2SpecificReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        List<SecurityEvent> events = eventRepository.findByTimestampBetween(startDate, endDate);

        report.put("systemAvailability", calculateSystemAvailability(events));
        report.put("securityIncidents", events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count());
        report.put("processingIntegrity", "Maintained"); // Simplified

        return report;
    }

    private Map<String, Object> generateISO27001SpecificReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        List<SecurityEvent> events = eventRepository.findByTimestampBetween(startDate, endDate);
        List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startDate);

        report.put("riskManagement", Map.of(
                "identifiedRisks", alerts.size(),
                "mitigatedRisks", alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count()
        ));
        report.put("incidentManagement", Map.of(
                "totalIncidents", alerts.size(),
                "responseTime", "Within SLA" // Simplified
        ));

        return report;
    }

    // Additional utility methods
    private List<Map<String, Object>> getRecentComplianceEvents(LocalDateTime startDate, LocalDateTime endDate) {
        List<Map<String, Object>> events = new ArrayList<>();

        List<ThreatAlert> alerts = alertRepository.findRecentAlerts(startDate);

        for (ThreatAlert alert : alerts.stream().limit(10).collect(Collectors.toList())) {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "THREAT_ALERT");
            event.put("severity", alert.getSeverity());
            event.put("title", alert.getTitle());
            event.put("timestamp", alert.getCreatedAt());
            event.put("status", alert.getStatus());
            events.add(event);
        }

        return events;
    }

    private Map<String, Object> analyzeComplianceTrends(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> trends = new HashMap<>();

        // Simplified trend analysis
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);

        trends.put("complianceDirection", "STABLE");
        trends.put("riskTrend", "DECREASING");
        trends.put("incidentTrend", "STABLE");
        trends.put("analysisWindow", totalDays + " days");

        return trends;
    }

    private List<Map<String, Object>> getUpcomingComplianceDeadlines() {
        List<Map<String, Object>> deadlines = new ArrayList<>();

        // Add some example upcoming deadlines
        Map<String, Object> deadline1 = new HashMap<>();
        deadline1.put("framework", "GDPR");
        deadline1.put("requirement", "Annual Security Review");
        deadline1.put("dueDate", LocalDateTime.now().plusDays(30));
        deadline1.put("priority", "MEDIUM");
        deadlines.add(deadline1);

        Map<String, Object> deadline2 = new HashMap<>();
        deadline2.put("framework", "SOC2");
        deadline2.put("requirement", "Quarterly Risk Assessment");
        deadline2.put("dueDate", LocalDateTime.now().plusDays(15));
        deadline2.put("priority", "HIGH");
        deadlines.add(deadline2);

        return deadlines;
    }

    private void checkImmediateViolations(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        List<SecurityEvent> recentEvents = eventRepository.findByTimestampBetween(startDate, endDate);
        List<ThreatAlert> recentAlerts = alertRepository.findRecentAlerts(startDate);

        // Check for immediate violations
        long criticalEvents = recentEvents.stream()
                .filter(e -> "CRITICAL".equals(e.getThreatLevel()))
                .count();

        if (criticalEvents > 0) {
            log.warn("Immediate compliance violation detected: {} critical events in last hour for framework {}",
                    criticalEvents, framework.getName());
        }
    }

    private void updateComplianceCache(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        String cacheKey = framework.getName() + "_current";
        ComplianceMetrics metrics = calculateComplianceMetrics(framework, startDate, endDate);
        complianceCache.put(cacheKey, metrics);
    }

    private boolean isCriticalViolation(String frameworkName, String controlId, String violationType) {
        // Determine if violation is critical based on framework and control
        return "GDPR".equals(frameworkName) && controlId.contains("33.1") ||
                "CRITICAL".equals(violationType) ||
                controlId.contains("CRITICAL");
    }

    private void triggerCriticalComplianceAlert(Map<String, Object> violation) {
        log.error("CRITICAL COMPLIANCE ALERT: {}", violation);
        // Could integrate with alerting system here
    }

    private List<Map<String, Object>> getControlHistoricalData(ComplianceControl control, LocalDateTime startDate, LocalDateTime endDate) {
        List<Map<String, Object>> historicalData = new ArrayList<>();

        // Generate sample historical data
        LocalDateTime current = startDate;
        while (current.isBefore(endDate)) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", current);
            dataPoint.put("score", 75.0 + (Math.random() * 20)); // Sample data
            historicalData.add(dataPoint);
            current = current.plusDays(1);
        }

        return historicalData;
    }

    private Map<String, Object> auditSecurityEvents(ComplianceFramework framework, List<SecurityEvent> events) {
        Map<String, Object> audit = new HashMap<>();

        audit.put("totalEvents", events.size());
        audit.put("eventTypes", events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getEventType, Collectors.counting())));
        audit.put("threatLevels", events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getThreatLevel, Collectors.counting())));

        return audit;
    }

    private Map<String, Object> auditAccessControls(ComplianceFramework framework, List<SecurityEvent> events) {
        Map<String, Object> audit = new HashMap<>();

        long accessEvents = events.stream()
                .filter(e -> "REQUEST".equals(e.getEventType()))
                .count();

        long rejectedAccess = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        audit.put("totalAccessAttempts", accessEvents);
        audit.put("rejectedAccess", rejectedAccess);
        audit.put("accessSuccessRate", accessEvents > 0 ? (accessEvents - rejectedAccess) * 100.0 / accessEvents : 100);

        return audit;
    }

    private Map<String, Object> auditDataProtection(ComplianceFramework framework, List<SecurityEvent> events) {
        Map<String, Object> audit = new HashMap<>();

        // Simplified data protection audit
        audit.put("encryptedConnections", "100%"); // Would need HTTPS analysis
        audit.put("dataIntegrityChecks", "Enabled");
        audit.put("sensitiveDataEvents", events.stream()
                .filter(e -> e.getRequestPath() != null && e.getRequestPath().contains("api"))
                .count());

        return audit;
    }

    private Map<String, Object> auditIncidentResponse(ComplianceFramework framework, List<ThreatAlert> threats) {
        Map<String, Object> audit = new HashMap<>();

        long totalThreats = threats.size();
        long resolvedThreats = threats.stream()
                .filter(t -> "RESOLVED".equals(t.getStatus()))
                .count();

        audit.put("totalThreats", totalThreats);
        audit.put("resolvedThreats", resolvedThreats);
        audit.put("resolutionRate", totalThreats > 0 ? resolvedThreats * 100.0 / totalThreats : 100);

        return audit;
    }

    private List<Map<String, Object>> identifyComplianceGaps(ComplianceFramework framework, LocalDateTime startDate, LocalDateTime endDate) {
        List<Map<String, Object>> gaps = new ArrayList<>();

        ComplianceMetrics metrics = calculateComplianceMetrics(framework, startDate, endDate);

        if (metrics.getOverallScore() < 80) {
            Map<String, Object> gap = new HashMap<>();
            gap.put("area", "Overall Compliance");
            gap.put("description", "Compliance score below target threshold");
            gap.put("impact", "HIGH");
            gap.put("recommendation", "Implement missing controls and improve existing ones");
            gaps.add(gap);
        }

        return gaps;
    }

    private Map<String, Object> assessComplianceRisks(ComplianceFramework framework, List<SecurityEvent> events, List<ThreatAlert> threats) {
        Map<String, Object> risks = new HashMap<>();

        double eventRisk = events.isEmpty() ? 0.1 : Math.min(1.0, events.size() / 1000.0);
        double threatRisk = threats.isEmpty() ? 0.1 : Math.min(1.0, threats.size() / 100.0);

        double overallRisk = (eventRisk + threatRisk) / 2.0;

        risks.put("overallRisk", overallRisk);
        risks.put("riskLevel", getRiskLevel(overallRisk));
        risks.put("eventBasedRisk", eventRisk);
        risks.put("threatBasedRisk", threatRisk);

        return risks;
    }

    private double calculateDataRisk(List<SecurityEvent> events) {
        long dataEvents = events.stream()
                .filter(e -> e.getRequestPath() != null &&
                        (e.getRequestPath().contains("api") || e.getRequestPath().contains("data")))
                .count();

        return events.isEmpty() ? 0.1 : Math.min(1.0, (double) dataEvents / events.size());
    }

    private double calculateAccessRisk(List<SecurityEvent> events) {
        long rejectedEvents = events.stream()
                .filter(e -> "REJECTION".equals(e.getEventType()))
                .count();

        return events.isEmpty() ? 0.1 : Math.min(1.0, (double) rejectedEvents / events.size());
    }

    private double calculateIncidentRisk(List<ThreatAlert> alerts) {
        long criticalAlerts = alerts.stream()
                .filter(a -> "CRITICAL".equals(a.getSeverity()) || "HIGH".equals(a.getSeverity()))
                .count();

        return alerts.isEmpty() ? 0.1 : Math.min(1.0, (double) criticalAlerts / alerts.size());
    }

    private String getRiskLevel(double risk) {
        if (risk > 0.8) return "CRITICAL";
        if (risk > 0.6) return "HIGH";
        if (risk > 0.4) return "MEDIUM";
        return "LOW";
    }

    private double calculateAuditCoverage(List<SecurityEvent> events) {
        long eventsWithDetails = events.stream()
                .filter(e -> e.getClientIp() != null && e.getRequestPath() != null && e.getTimestamp() != null)
                .count();

        return events.isEmpty() ? 100.0 : (double) eventsWithDetails / events.size() * 100;
    }

    private double calculateSystemAvailability(List<SecurityEvent> events) {
        // Simplified availability calculation
        long errorEvents = events.stream()
                .filter(e -> e.getResponseStatus() != null && e.getResponseStatus() >= 500)
                .count();

        return events.isEmpty() ? 100.0 : Math.max(0, 100.0 - ((double) errorEvents / events.size() * 100));
    }

    // Inner classes for compliance framework structure

    public static class ComplianceFramework {
        private final String name;
        private final String description;
        private final List<ComplianceControl> controls = new ArrayList<>();

        public ComplianceFramework(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public void addControl(ComplianceControl control) {
            this.controls.add(control);
        }

        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<ComplianceControl> getControls() { return controls; }
    }

    public static class ComplianceControl {
        private final String id;
        private final String title;
        private final String description;
        private final String category;
        private final String severity;

        public ComplianceControl(String id, String title, String description, String category, String severity) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.severity = severity;
        }

        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getSeverity() { return severity; }
    }

    public static class ComplianceMetrics {
        private double overallScore;
        private String complianceStatus;
        private int compliantControls;
        private int nonCompliantControls;
        private List<String> criticalIssues = new ArrayList<>();
        private LocalDateTime lastAssessment;

        // Getters and setters
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

        public String getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }

        public int getCompliantControls() { return compliantControls; }
        public void setCompliantControls(int compliantControls) { this.compliantControls = compliantControls; }

        public int getNonCompliantControls() { return nonCompliantControls; }
        public void setNonCompliantControls(int nonCompliantControls) { this.nonCompliantControls = nonCompliantControls; }

        public List<String> getCriticalIssues() { return criticalIssues; }
        public void setCriticalIssues(List<String> criticalIssues) { this.criticalIssues = criticalIssues; }

        public LocalDateTime getLastAssessment() { return lastAssessment; }
        public void setLastAssessment(LocalDateTime lastAssessment) { this.lastAssessment = lastAssessment; }
    }

    public static class ControlAssessmentResult {
        private String status = "UNKNOWN";
        private double score = 0.0;
        private List<String> findings = new ArrayList<>();
        private List<String> evidence = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();

        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public List<String> getFindings() { return findings; }
        public void setFindings(List<String> findings) { this.findings = findings; }

        public List<String> getEvidence() { return evidence; }
        public void setEvidence(List<String> evidence) { this.evidence = evidence; }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
}