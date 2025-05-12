package com.example.gateway_admin.Repositories;

import com.example.gateway_admin.Entities.GatewayRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GatewayRouteRepository extends JpaRepository<GatewayRoute, Long> {

    GatewayRoute findByPredicates(String predicates);

    // Checks if a GatewayRoute with the given predicate already exists
    boolean existsByPredicates(String predicates);

    @Query("SELECT DISTINCT r FROM GatewayRoute r " +
            "LEFT JOIN FETCH r.allowedIps " +
            "LEFT JOIN FETCH r.rateLimit")
    List<GatewayRoute> findAllWithAllowedIpsAndRateLimit();
}
