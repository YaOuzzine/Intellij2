package com.example.demo.Db;

import com.example.demo.Entity.GatewayRoute;
import com.example.demo.Repository.GatewayRouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GatewayRouteService {

    @Autowired
    private GatewayRouteRepository gatewayRouteRepository;

    @Transactional(readOnly = true)
    public List<GatewayRoute> getAllRoutesWithFilters() {
        // Use the repository method that eagerly fetches AllowedIps and RateLimit.
        List<GatewayRoute> routes = gatewayRouteRepository.findAllWithAllowedIpsAndRateLimit();

        // Force eager loading (if needed) by accessing the allowedIps list.
        routes.forEach(route -> route.getAllowedIps().size());

        return routes;
    }
}