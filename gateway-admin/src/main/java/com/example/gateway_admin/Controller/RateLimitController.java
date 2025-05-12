// src/main/java/com/example/gateway_admin/Controller/RateLimitController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.Entities.RateLimit;
import com.example.gateway_admin.Repositories.RateLimitRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

    private final RateLimitRepository rateLimitRepository;

    public RateLimitController(RateLimitRepository rateLimitRepository) {
        this.rateLimitRepository = rateLimitRepository;
    }

    // GET all rate limits
    @GetMapping
    public List<RateLimit> getAllRateLimits() {
        return rateLimitRepository.findAll();
    }

    // GET a specific rate limit by id
    @GetMapping("/{id}")
    public RateLimit getRateLimitById(@PathVariable Long id) {
        return rateLimitRepository.findById(id).orElse(null);
    }

    // POST to create a new rate limit
    @PostMapping
    public RateLimit createRateLimit(@RequestBody RateLimit rateLimit) {
        return rateLimitRepository.save(rateLimit);
    }

    // PUT to update an existing rate limit (full update)
    @PutMapping("/{id}")
    public RateLimit updateRateLimit(@PathVariable Long id, @RequestBody RateLimit updatedRateLimit) {
        RateLimit existing = rateLimitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RateLimit not found with id " + id));
        // Update only the fields that should change
        existing.setMaxRequests(updatedRateLimit.getMaxRequests());
        existing.setTimeWindowMs(updatedRateLimit.getTimeWindowMs());
        return rateLimitRepository.save(existing);
    }

    // DELETE a rate limit by id
    @DeleteMapping("/{id}")
    public void deleteRateLimit(@PathVariable Long id) {
        rateLimitRepository.deleteById(id);
    }
}
