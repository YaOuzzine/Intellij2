/*
package com.example.gateway_admin.Services;

import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Repositories.GatewayRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class GatewayRouteService {

    private final GatewayRouteRepository gatewayRouteRepository;

    public GatewayRouteService(GatewayRouteRepository gatewayRouteRepository) {
        this.gatewayRouteRepository = gatewayRouteRepository;
    }

    @Transactional(readOnly = true)
    public List<GatewayRoute> getAllRoutes() {
        return gatewayRouteRepository.findAll();
    }

    @Transactional
    public GatewayRoute createRoute(GatewayRoute route) {
        return gatewayRouteRepository.save(route);
    }

    @Transactional
    public GatewayRoute updateRoute(Long id, GatewayRoute updatedRoute) {
        // Set the id so that JPA performs an update instead of an insert
        updatedRoute.setId(id);
        return gatewayRouteRepository.save(updatedRoute);
    }

    @Transactional
    public void deleteRoute(Long id) {
        gatewayRouteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public GatewayRoute getRouteById(Long id) {
        return gatewayRouteRepository.findById(id).orElse(null);
    }
}
*/
// src/main/java/com/example/gateway_admin/Services/GatewayRouteService.java
package com.example.gateway_admin.Services;

import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Repositories.GatewayRouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GatewayRouteService {

    private final GatewayRouteRepository gatewayRouteRepository;

    public GatewayRouteService(GatewayRouteRepository gatewayRouteRepository) {
        this.gatewayRouteRepository = gatewayRouteRepository;
    }

    public List<GatewayRoute> getAllRoutes() {
        return gatewayRouteRepository.findAll();
    }

    public GatewayRoute getRouteById(Long id) {
        return gatewayRouteRepository.findById(id).orElse(null);
    }

    public GatewayRoute createRoute(GatewayRoute route) {
        // If route has a RateLimit, it will be saved automatically due to cascade = ALL
        return gatewayRouteRepository.save(route);
    }

    public GatewayRoute updateRoute(Long id, GatewayRoute mergedRoute) {
        // mergedRoute is the route that has had fields "merged" in the controller
        return gatewayRouteRepository.save(mergedRoute);
    }

    public void deleteRoute(Long id) {
        gatewayRouteRepository.deleteById(id);
    }
}
