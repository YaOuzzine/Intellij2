package com.example.demo.Entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gateway_routes", schema = "gateway")
public class GatewayRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uri;
    private String routeId;

    @Column(unique = true)
    private String predicates;

    private Boolean withIpFilter;
    private Boolean withToken;
    private Boolean withRateLimit; // New field

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "rate_limit_id")
    private RateLimit rateLimit;

    @OneToMany(mappedBy = "gatewayRoute", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AllowedIp> allowedIps = new ArrayList<>();

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRouteId() {
        return routeId;
    }
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getPredicates() {
        return predicates;
    }
    public void setPredicates(String predicates) {
        this.predicates = predicates;
    }

    public Boolean getWithIpFilter() {
        return withIpFilter;
    }
    public void setWithIpFilter(Boolean withIpFilter) {
        this.withIpFilter = withIpFilter;
    }

    public Boolean getWithToken() {
        return withToken;
    }
    public void setWithToken(Boolean withToken) {
        this.withToken = withToken;
    }

    public Boolean getWithRateLimit() {
        return withRateLimit;
    }
    public void setWithRateLimit(Boolean withRateLimit) {
        this.withRateLimit = withRateLimit;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public List<AllowedIp> getAllowedIps() {
        return allowedIps;
    }
    public void setAllowedIps(List<AllowedIp> allowedIps) {
        this.allowedIps = allowedIps;
    }
}
