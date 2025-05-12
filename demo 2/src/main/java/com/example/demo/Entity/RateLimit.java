package com.example.demo.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rate_limit", schema = "gateway")
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This field references the 'id' of the GatewayRoute table
    private Long routeId;

    private Integer maxRequests;
    private Integer timeWindowMs;

    public RateLimit() {}

    public RateLimit(Integer maxRequests, Integer timeWindowMs) {
        this.maxRequests = maxRequests;
        this.timeWindowMs = timeWindowMs;
    }

    public Long getId() {
        return id;
    }

    // Add getters/setters for routeId
    public Long getRouteId() {
        return routeId;
    }
    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Integer getMaxRequests() {
        return maxRequests;
    }
    public void setMaxRequests(Integer maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Integer getTimeWindowMs() {
        return timeWindowMs;
    }
    public void setTimeWindowMs(Integer timeWindowMs) {
        this.timeWindowMs = timeWindowMs;
    }
}