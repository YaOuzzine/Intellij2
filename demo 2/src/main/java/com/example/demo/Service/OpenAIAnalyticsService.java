// demo 2/src/main/java/com/example/demo/Service/OpenAIAnalyticsService.java
package com.example.demo.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIAnalyticsService.class);

    @Autowired
    private OpenAIService openAIService;

    public Map<String, Object> generateExecutiveSummary(Map<String, Object> securityData) {
        Map<String, Object> result = new HashMap<>();

        try {
            String systemPrompt = """
                You are a senior cybersecurity analyst with 20+ years of experience. 
                Analyze the provided security data and generate a comprehensive executive summary.
                Your analysis should be professional, actionable, and focused on business impact.
                
                Format your response as structured sections:
                1. SECURITY POSTURE: Overall assessment (1-10 scale with explanation)
                2. KEY THREATS: Most critical current threats (be specific)
                3. BUSINESS IMPACT: Potential impact on operations
                4. IMMEDIATE ACTIONS: Top 3 priority actions
                5. RISK LEVEL: Overall risk assessment (LOW/MEDIUM/HIGH/CRITICAL)
                
                Keep your response concise but comprehensive.
                """;

            String userPrompt = buildExecutiveSummaryPrompt(securityData);

            String aiAnalysis = openAIService.generateChatCompletion(systemPrompt, userPrompt)
                    .block(); // Block for synchronous execution

            if (aiAnalysis != null && !aiAnalysis.contains("Error") && !aiAnalysis.contains("not configured")) {
                // Parse AI response into structured data
                Map<String, Object> parsedAnalysis = parseAIResponse(aiAnalysis);

                result.put("aiAnalysis", aiAnalysis);
                result.put("structuredAnalysis", parsedAnalysis);
                result.put("confidence", "HIGH");
                result.put("generatedBy", "OpenAI GPT");
                result.put("timestamp", LocalDateTime.now());

                log.info("Generated AI executive summary successfully");
            } else {
                log.warn("AI analysis failed or not configured, using fallback");
                result.put("error", "AI analysis unavailable");
                result.put("fallbackAnalysis", generateFallbackSummary(securityData));
            }

        } catch (Exception e) {
            log.error("Error generating AI executive summary: {}", e.getMessage(), e);
            result.put("error", "AI analysis unavailable");
            result.put("fallbackAnalysis", generateFallbackSummary(securityData));
        }

        return result;
    }

    private String buildExecutiveSummaryPrompt(Map<String, Object> securityData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("CYBERSECURITY EXECUTIVE SUMMARY REQUEST\n\n");

        prompt.append("CURRENT SECURITY METRICS:\n");
        prompt.append("- Total Security Events: ").append(securityData.getOrDefault("totalEvents", 0)).append("\n");
        prompt.append("- Critical Events: ").append(securityData.getOrDefault("criticalEvents", 0)).append("\n");
        prompt.append("- High Threat Alerts: ").append(securityData.getOrDefault("highThreatAlerts", 0)).append("\n");
        prompt.append("- Total Requests: ").append(securityData.getOrDefault("totalRequests", 0)).append("\n");
        prompt.append("- Rejected Requests: ").append(securityData.getOrDefault("totalRejections", 0)).append("\n");

        // Add rejection rate calculation
        Long totalRequests = (Long) securityData.getOrDefault("totalRequests", 0L);
        Long totalRejections = (Long) securityData.getOrDefault("totalRejections", 0L);
        if (totalRequests > 0) {
            double rejectionRate = (double) totalRejections / totalRequests * 100;
            prompt.append("- Rejection Rate: ").append(String.format("%.2f%%", rejectionRate)).append("\n");
        }

        // Add geographic threat data if available
        if (securityData.containsKey("geographicThreats")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> geoThreats = (List<Map<String, Object>>) securityData.get("geographicThreats");
            if (!geoThreats.isEmpty()) {
                prompt.append("- Geographic Threats Detected: ").append(geoThreats.size()).append(" countries\n");
                for (Map<String, Object> threat : geoThreats.subList(0, Math.min(3, geoThreats.size()))) {
                    prompt.append("  * ").append(threat.get("country"))
                            .append(": ").append(threat.get("uniqueIPs")).append(" IPs, ")
                            .append(threat.get("totalRejections")).append(" rejections\n");
                }
            }
        }

        // Add top attack vectors if available
        if (securityData.containsKey("topAttackVectors")) {
            @SuppressWarnings("unchecked")
            List<String> attackVectors = (List<String>) securityData.get("topAttackVectors");
            if (!attackVectors.isEmpty()) {
                prompt.append("- Top Attack Vectors: ").append(String.join(", ", attackVectors)).append("\n");
            }
        }

        prompt.append("\nTIME PERIOD: Last 24 hours\n");
        prompt.append("\nPlease provide a comprehensive executive summary focusing on:");
        prompt.append("\n1. Current security posture assessment");
        prompt.append("\n2. Critical threats requiring immediate attention");
        prompt.append("\n3. Business impact and operational risks");
        prompt.append("\n4. Specific actionable recommendations");
        prompt.append("\n5. Overall risk level and trend analysis");

        return prompt.toString();
    }

    private Map<String, Object> parseAIResponse(String aiResponse) {
        Map<String, Object> parsed = new HashMap<>();

        try {
            // Extract security posture score (1-10)
            if (aiResponse.contains("SECURITY POSTURE:")) {
                String postureSection = extractSection(aiResponse, "SECURITY POSTURE:");
                String scoreStr = extractNumber(postureSection);
                if (scoreStr != null) {
                    parsed.put("securityPostureScore", Integer.parseInt(scoreStr));
                }
                parsed.put("securityPostureAnalysis", postureSection);
            }

            // Extract key threats
            if (aiResponse.contains("KEY THREATS:")) {
                String threatsSection = extractSection(aiResponse, "KEY THREATS:");
                parsed.put("keyThreats", threatsSection);
            }

            // Extract business impact
            if (aiResponse.contains("BUSINESS IMPACT:")) {
                String impactSection = extractSection(aiResponse, "BUSINESS IMPACT:");
                parsed.put("businessImpact", impactSection);
            }

            // Extract immediate actions
            if (aiResponse.contains("IMMEDIATE ACTIONS:")) {
                String actionsSection = extractSection(aiResponse, "IMMEDIATE ACTIONS:");
                parsed.put("immediateActions", actionsSection);
            }

            // Extract risk level
            if (aiResponse.contains("RISK LEVEL:")) {
                String riskSection = extractSection(aiResponse, "RISK LEVEL:");
                parsed.put("riskLevel", extractRiskLevel(riskSection));
                parsed.put("riskAnalysis", riskSection);
            }

        } catch (Exception e) {
            log.warn("Error parsing AI response: {}", e.getMessage());
        }

        return parsed;
    }

    private String extractSection(String text, String sectionHeader) {
        int startIndex = text.indexOf(sectionHeader);
        if (startIndex == -1) return "";

        startIndex += sectionHeader.length();
        int endIndex = findNextSectionStart(text, startIndex);
        if (endIndex == -1) endIndex = text.length();

        return text.substring(startIndex, endIndex).trim();
    }

    private int findNextSectionStart(String text, int fromIndex) {
        String[] sections = {"SECURITY POSTURE:", "KEY THREATS:", "BUSINESS IMPACT:", "IMMEDIATE ACTIONS:", "RISK LEVEL:"};
        int earliest = -1;

        for (String section : sections) {
            int index = text.indexOf(section, fromIndex);
            if (index != -1 && (earliest == -1 || index < earliest)) {
                earliest = index;
            }
        }

        return earliest;
    }

    private String extractNumber(String text) {
        // Extract first number found (for security posture score)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b([1-9]|10)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractRiskLevel(String text) {
        String upperText = text.toUpperCase();
        if (upperText.contains("CRITICAL")) return "CRITICAL";
        if (upperText.contains("HIGH")) return "HIGH";
        if (upperText.contains("MEDIUM") || upperText.contains("MODERATE")) return "MEDIUM";
        if (upperText.contains("LOW")) return "LOW";
        return "UNKNOWN";
    }

    private Map<String, Object> generateFallbackSummary(Map<String, Object> securityData) {
        Map<String, Object> fallback = new HashMap<>();

        // Simple rule-based fallback when AI is unavailable
        Long totalEvents = (Long) securityData.getOrDefault("totalEvents", 0L);
        Long criticalEvents = (Long) securityData.getOrDefault("criticalEvents", 0L);
        Long totalRequests = (Long) securityData.getOrDefault("totalRequests", 0L);
        Long totalRejections = (Long) securityData.getOrDefault("totalRejections", 0L);

        if (totalEvents == 0) {
            fallback.put("summary", "No security events recorded in the analysis period.");
            fallback.put("riskLevel", "UNKNOWN");
            fallback.put("securityPostureScore", 5);
        } else {
            double criticalRatio = (double) criticalEvents / totalEvents;
            double rejectionRate = totalRequests > 0 ? (double) totalRejections / totalRequests : 0;

            StringBuilder summary = new StringBuilder();
            String riskLevel;
            int postureScore;

            if (criticalRatio > 0.1 || rejectionRate > 0.5) {
                summary.append("High number of critical security events detected (")
                        .append(String.format("%.1f%% critical, %.1f%% rejection rate", criticalRatio * 100, rejectionRate * 100))
                        .append("). Immediate review recommended.");
                riskLevel = "HIGH";
                postureScore = 3;
            } else if (criticalRatio > 0.05 || rejectionRate > 0.2) {
                summary.append("Moderate security activity with some critical events (")
                        .append(String.format("%.1f%% critical, %.1f%% rejection rate", criticalRatio * 100, rejectionRate * 100))
                        .append("). Continue monitoring.");
                riskLevel = "MEDIUM";
                postureScore = 6;
            } else {
                summary.append("Security posture appears stable with minimal critical events (")
                        .append(String.format("%.1f%% critical, %.1f%% rejection rate", criticalRatio * 100, rejectionRate * 100))
                        .append(").");
                riskLevel = "LOW";
                postureScore = 8;
            }

            fallback.put("summary", summary.toString());
            fallback.put("riskLevel", riskLevel);
            fallback.put("securityPostureScore", postureScore);
        }

        fallback.put("generatedBy", "Rule-based fallback analysis");
        fallback.put("confidence", "MEDIUM");

        return fallback;
    }
}