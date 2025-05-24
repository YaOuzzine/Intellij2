package com.example.demo.Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "security_events", schema = "gateway")
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // 'REQUEST', 'REJECTION', 'ATTACK_ATTEMPT', 'RATE_LIMIT_HIT'

    @Column(name = "route_id", length = 255)
    private String routeId;

    @Column(name = "client_ip", length = 45) // Support IPv6
    private String clientIp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "rejection_reason", length = 100)
    private String rejectionReason;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "geo_location")
    @JdbcTypeCode(SqlTypes.JSON)
    private String geoLocation; // Store as JSON string

    @Column(name = "threat_level", length = 20)
    private String threatLevel = "LOW"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata; // Store additional data as JSON

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_size")
    private Long requestSize;

    @Column(name = "headers")
    @JdbcTypeCode(SqlTypes.JSON)
    private String headers; // Store relevant headers as JSON

    // Constructors
    public SecurityEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public SecurityEvent(String eventType, String routeId, String clientIp) {
        this();
        this.eventType = eventType;
        this.routeId = routeId;
        this.clientIp = clientIp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(String geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getThreatLevel() {
        return threatLevel;
    }

    public void setThreatLevel(String threatLevel) {
        this.threatLevel = threatLevel;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Long getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(Long requestSize) {
        this.requestSize = requestSize;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }
}