package com.example.demo.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "allowed_ips", schema = "gateway")
public class AllowedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    @ManyToOne
    @JoinColumn(name = "gateway_route_id")
    private GatewayRoute gatewayRoute;

    // Getters & Setters
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
}
