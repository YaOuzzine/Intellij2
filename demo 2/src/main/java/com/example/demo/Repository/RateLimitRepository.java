package com.example.demo.Repository;

import com.example.demo.Entity.RateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface

RateLimitRepository extends JpaRepository<RateLimit, Long> {

    // You can define a custom query to find the rate limit by routeId
    RateLimit findByRouteId(Long routeId);
}
