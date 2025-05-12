package com.example.gateway_admin.Repositories;

import com.example.gateway_admin.Entities.RateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimit, Long> {
}
