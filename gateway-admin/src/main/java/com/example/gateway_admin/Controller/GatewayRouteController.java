// src/main/java/com/example/gateway_admin/Controller/GatewayRouteController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Entities.RateLimit;
import com.example.gateway_admin.Services.GatewayRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/gateway-routes")
public class GatewayRouteController {

    private final GatewayRouteService gatewayRouteService;

    public GatewayRouteController(GatewayRouteService gatewayRouteService) {
        this.gatewayRouteService = gatewayRouteService;
    }

    @GetMapping
    public List<GatewayRoute> getAllRoutes() {
        return gatewayRouteService.getAllRoutes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GatewayRoute> getRouteById(@PathVariable Long id) {
        GatewayRoute route = gatewayRouteService.getRouteById(id);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(route);
    }

    @PostMapping
    public GatewayRoute createRoute(@RequestBody GatewayRoute route) {
        return gatewayRouteService.createRoute(route);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GatewayRoute> updateRoute(@PathVariable Long id, @RequestBody GatewayRoute updatedRoute) {
        GatewayRoute existingRoute = gatewayRouteService.getRouteById(id);
        if (existingRoute == null) {
            return ResponseEntity.notFound().build();
        }

        // Merge top-level fields
        existingRoute.setUri(updatedRoute.getUri());
        existingRoute.setRouteId(updatedRoute.getRouteId());
        existingRoute.setPredicates(updatedRoute.getPredicates());
        existingRoute.setWithIpFilter(updatedRoute.getWithIpFilter());
        existingRoute.setWithToken(updatedRoute.getWithToken());
        existingRoute.setWithRateLimit(updatedRoute.getWithRateLimit());

        // Merge the nested RateLimit if present
        if (updatedRoute.getRateLimit() != null) {
            if (existingRoute.getRateLimit() == null) {
                existingRoute.setRateLimit(new RateLimit());
            }
            existingRoute.getRateLimit().setMaxRequests(updatedRoute.getRateLimit().getMaxRequests());
            existingRoute.getRateLimit().setTimeWindowMs(updatedRoute.getRateLimit().getTimeWindowMs());
            existingRoute.getRateLimit().setRouteId(updatedRoute.getRateLimit().getRouteId());
        }

        GatewayRoute savedRoute = gatewayRouteService.updateRoute(id, existingRoute);
        return ResponseEntity.ok(savedRoute);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GatewayRoute> patchRoute(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        GatewayRoute existingRoute = gatewayRouteService.getRouteById(id);
        if (existingRoute == null) {
            return ResponseEntity.notFound().build();
        }

        // partial update logic
        if (updates.containsKey("routeId")) {
            existingRoute.setRouteId((String) updates.get("routeId"));
        }
        if (updates.containsKey("uri")) {
            existingRoute.setUri((String) updates.get("uri"));
        }
        if (updates.containsKey("predicates")) {
            existingRoute.setPredicates((String) updates.get("predicates"));
        }
        if (updates.containsKey("withIpFilter")) {
            existingRoute.setWithIpFilter((Boolean) updates.get("withIpFilter"));
        }
        if (updates.containsKey("withToken")) {
            existingRoute.setWithToken((Boolean) updates.get("withToken"));
        }
        if (updates.containsKey("withRateLimit")) {
            existingRoute.setWithRateLimit((Boolean) updates.get("withRateLimit"));
        }

        if (updates.containsKey("rateLimit")) {
            // Merge partial rate limit fields
            Map<String, Object> rl = (Map<String, Object>) updates.get("rateLimit");
            if (existingRoute.getRateLimit() == null) {
                existingRoute.setRateLimit(new RateLimit());
            }
            if (rl.containsKey("maxRequests")) {
                existingRoute.getRateLimit().setMaxRequests((Integer) rl.get("maxRequests"));
            }
            if (rl.containsKey("timeWindowMs")) {
                existingRoute.getRateLimit().setTimeWindowMs((Integer) rl.get("timeWindowMs"));
            }
            if (rl.containsKey("routeId")) {
                existingRoute.getRateLimit().setRouteId(Long.valueOf(rl.get("routeId").toString()));
            }
        }

        GatewayRoute updatedRoute = gatewayRouteService.updateRoute(id, existingRoute);
        return ResponseEntity.ok(updatedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        GatewayRoute existingRoute = gatewayRouteService.getRouteById(id);
        if (existingRoute == null) {
            return ResponseEntity.notFound().build();
        }
        gatewayRouteService.deleteRoute(id);
        return ResponseEntity.ok().build();
    }
}



/*
// src/main/java/com/example/gateway_admin/Controller/GatewayRouteController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Services.GatewayRouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/gateway-routes")
public class GatewayRouteController {

    private final GatewayRouteService gatewayRouteService;

    public GatewayRouteController(GatewayRouteService gatewayRouteService) {
        this.gatewayRouteService = gatewayRouteService;
    }

    @GetMapping
    public List<GatewayRoute> getAllRoutes() {
        return gatewayRouteService.getAllRoutes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GatewayRoute> getRouteById(@PathVariable Long id) {
        GatewayRoute route = gatewayRouteService.getRouteById(id);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(route);
    }

    @PostMapping
    public GatewayRoute createRoute(@RequestBody GatewayRoute route) {
        return gatewayRouteService.createRoute(route);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GatewayRoute> updateRoute(@PathVariable Long id, @RequestBody GatewayRoute updatedRoute) {
        GatewayRoute existingRoute = gatewayRouteService.getRouteById(id);
        if (existingRoute == null) {
            return ResponseEntity.notFound().build();
        }
        updatedRoute.setId(id);
        GatewayRoute savedRoute = gatewayRouteService.updateRoute(id, updatedRoute);
        return ResponseEntity.ok(savedRoute);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GatewayRoute> patchRoute(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        GatewayRoute existingRoute = gatewayRouteService.getRouteById(id);
        if (existingRoute == null) {
            return ResponseEntity.notFound().build();
        }
        if (updates.containsKey("routeId")) {
            existingRoute.setRouteId((String) updates.get("routeId"));
        }
        if (updates.containsKey("uri")) {
            existingRoute.setUri((String) updates.get("uri"));
        }
        if (updates.containsKey("predicates")) {
            existingRoute.setPredicates((String) updates.get("predicates"));
        }
        if (updates.containsKey("withIpFilter")) {
            existingRoute.setWithIpFilter((Boolean) updates.get("withIpFilter"));
        }
        if (updates.containsKey("withToken")) {
            existingRoute.setWithToken((Boolean) updates.get("withToken"));
        }
        if (updates.containsKey("withRateLimit")) {
            existingRoute.setWithRateLimit((Boolean) updates.get("withRateLimit"));
        }
        GatewayRoute updatedRoute = gatewayRouteService.updateRoute(id, existingRoute);
        return ResponseEntity.ok(updatedRoute);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        GatewayRoute existingRoute = gatewayRouteService.getRouteById(id);
        if (existingRoute == null) {
            return ResponseEntity.notFound().build();
        }
        gatewayRouteService.deleteRoute(id);
        return ResponseEntity.ok().build();
    }
}
*/
