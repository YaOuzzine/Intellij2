// src/main/java/com/example/gateway_admin/Entities/RateLimit.java
package com.example.gateway_admin.Entities;

import jakarta.persistence.*;

@Entity
@Table(name = "rate_limit", schema = "admin")
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long routeId;
    private Integer maxRequests;
    private Integer timeWindowMs;

    public RateLimit() {}

    public RateLimit(Integer maxRequests, Integer timeWindowMs) {
        this.maxRequests = maxRequests;
        this.timeWindowMs = timeWindowMs;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

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
