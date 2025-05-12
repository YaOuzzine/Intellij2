// src/main/java/com/example/gateway_admin/Entities/GatewayRoute.java
package com.example.gateway_admin.Entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a gateway route configuration.
 */
@Getter
@Entity
@Table(name = "gateway_routes", schema = "admin")
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
    private Boolean withRateLimit;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "rate_limit_id")
    private RateLimit rateLimit;

    @OneToMany(mappedBy = "gatewayRoute", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<AllowedIps> allowedIps = new ArrayList<>();

    public GatewayRoute() {}

    public void setId(Long id) {
        this.id = id;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public void setPredicates(String predicates) {
        this.predicates = predicates;
    }

    public void setWithIpFilter(Boolean withIpFilter) {
        this.withIpFilter = withIpFilter;
    }

    public void setWithToken(Boolean withToken) {
        this.withToken = withToken;
    }

    public void setWithRateLimit(Boolean withRateLimit) {
        this.withRateLimit = withRateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public void setAllowedIps(List<AllowedIps> allowedIps) {
        this.allowedIps = allowedIps;
    }
}
