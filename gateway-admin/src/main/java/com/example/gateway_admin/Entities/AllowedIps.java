package com.example.gateway_admin.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Represents an allowed IP associated with a gateway route.
 */
@Entity
@Table(name = "allowed_ips", schema = "admin")
public class AllowedIps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gateway_route_id", nullable = false)
    @JsonBackReference
    private GatewayRoute gatewayRoute;

    // Standard getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public GatewayRoute getGatewayRoute() {
        return gatewayRoute;
    }

    public void setGatewayRoute(GatewayRoute gatewayRoute) {
        this.gatewayRoute = gatewayRoute;
    }

    /**
     * Computed property to expose the gateway route id.
     * The property will be serialized as "gatewayRouteId" in the JSON output.
     */
    @Transient
    @JsonProperty("gatewayRouteId")
    public Long getGatewayRouteId() {
        return gatewayRoute != null ? gatewayRoute.getId() : null;
    }
}
