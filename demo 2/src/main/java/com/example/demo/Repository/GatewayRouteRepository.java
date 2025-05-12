package com.example.demo.Repository;

import com.example.demo.Entity.GatewayRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GatewayRouteRepository extends JpaRepository<GatewayRoute, Long> {

    @Query("SELECT DISTINCT r FROM GatewayRoute r " +
            "LEFT JOIN FETCH r.allowedIps " +
            "LEFT JOIN FETCH r.rateLimit")
    List<GatewayRoute> findAllWithAllowedIpsAndRateLimit();

    GatewayRoute findByPredicates(String predicates);
}
