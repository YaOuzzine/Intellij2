// src/main/java/com/example/gateway_admin/Controller/IpAddressController.java
package com.example.gateway_admin.Controller;

import com.example.gateway_admin.Entities.AllowedIps;
import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Repositories.AllowedIpRepository;
import com.example.gateway_admin.Repositories.GatewayRouteRepository;
import com.example.gateway_admin.Services.DataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/ip-addresses")
public class IpAddressController {

    private final AllowedIpRepository allowedIpRepository;
    private final GatewayRouteRepository gatewayRouteRepository;

    @Autowired
    private DataSyncService dataSyncService;

    @PersistenceContext
    private EntityManager entityManager;

    public IpAddressController(AllowedIpRepository allowedIpRepository, GatewayRouteRepository gatewayRouteRepository) {
        this.allowedIpRepository = allowedIpRepository;
        this.gatewayRouteRepository = gatewayRouteRepository;
    }

    // GET: All IP addresses with enhanced route details
    @GetMapping
    public List<Map<String, Object>> getAllIpAddresses() {
        List<AllowedIps> ips = allowedIpRepository.findAll();

        return ips.stream().map(ip -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", ip.getId());
            result.put("ip", ip.getIp());
            result.put("gatewayRouteId", ip.getGatewayRouteId());

            // Add enhanced route details if available
            if (ip.getGatewayRoute() != null) {
                result.put("predicate", ip.getGatewayRoute().getPredicates());
                result.put("routeUri", ip.getGatewayRoute().getUri());
                result.put("routeId", ip.getGatewayRoute().getRouteId());
                result.put("withIpFilter", ip.getGatewayRoute().getWithIpFilter());
            }

            return result;
        }).collect(Collectors.toList());
    }

    // GET: Retrieve routes for dropdown selection
    @GetMapping("/routes")
    public List<Map<String, Object>> getRoutesForSelection() {
        List<GatewayRoute> routes = gatewayRouteRepository.findAll();

        return routes.stream().map(route -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", route.getId());
            result.put("predicate", route.getPredicates());
            result.put("routeId", route.getRouteId());
            result.put("uri", route.getUri());
            result.put("withIpFilter", route.getWithIpFilter());
            result.put("ipCount", route.getAllowedIps() != null ? route.getAllowedIps().size() : 0);
            return result;
        }).collect(Collectors.toList());
    }

    // GET: Specific IP address by id
    @GetMapping("/{id}")
    public ResponseEntity<?> getIpAddressById(@PathVariable Long id) {
        return allowedIpRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: Create a new IP address and assign it to a GatewayRoute
    @PostMapping
    @Transactional
    public ResponseEntity<?> createIpAddress(@RequestBody AllowedIps ipAddress) {
        try {
            // Log the incoming payload for debugging
            System.out.println("Add Request Received: ip = " + ipAddress.getIp() +
                    ", gatewayRoute.id = " + (ipAddress.getGatewayRoute() != null ? ipAddress.getGatewayRoute().getId() : "null"));

            // Validate that a gateway route id is provided
            if (ipAddress.getGatewayRoute() == null || ipAddress.getGatewayRoute().getId() == null) {
                return ResponseEntity.badRequest().body("Gateway route id is missing in the request payload.");
            }

            // Check if IP is valid
            if (!isValidIpAddress(ipAddress.getIp())) {
                return ResponseEntity.badRequest().body("Invalid IP address format. Please use IPv4 format (e.g., 192.168.1.1)");
            }

            // Look up the gateway route by its id
            GatewayRoute route = gatewayRouteRepository.findById(ipAddress.getGatewayRoute().getId())
                    .orElseThrow(() -> new RuntimeException("Gateway route not found with id "
                            + ipAddress.getGatewayRoute().getId()));

            // Check if this IP already exists for this route
            boolean ipExists = route.getAllowedIps().stream()
                    .anyMatch(ip -> ip.getIp().equals(ipAddress.getIp()));

            if (ipExists) {
                return ResponseEntity.badRequest().body("This IP is already assigned to this route");
            }

            // Set withIpFilter to true if it's adding an IP
            if (!Boolean.TRUE.equals(route.getWithIpFilter())) {
                route.setWithIpFilter(true);
                gatewayRouteRepository.save(route);
            }

            // Associate the new IP address with the fetched GatewayRoute
            ipAddress.setGatewayRoute(route);

            // Add the IP address to the GatewayRoute's collection
            route.getAllowedIps().add(ipAddress);

            // Save the new AllowedIps record and log the result
            AllowedIps savedIp = allowedIpRepository.save(ipAddress);
            System.out.println("IP Address successfully added with id: " + savedIp.getId());

            // Trigger manual sync for immediate effect
            dataSyncService.syncRoutesToGatewaySchema();

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedIp.getId());
            response.put("ip", savedIp.getIp());
            response.put("gatewayRouteId", savedIp.getGatewayRouteId());
            response.put("predicate", route.getPredicates());
            response.put("routeUri", route.getUri());
            response.put("routeId", route.getRouteId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add IP address: " + e.getMessage());
        }
    }

    // PUT: Update an existing IP address (and optionally change its GatewayRoute)
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateIpAddress(@PathVariable Long id, @RequestBody AllowedIps updatedIp) {
        try {
            AllowedIps existingIp = allowedIpRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Allowed IP not found with id " + id));

            // Validate the new IP
            if (!isValidIpAddress(updatedIp.getIp())) {
                return ResponseEntity.badRequest().body("Invalid IP address format");
            }

            // Update IP address
            existingIp.setIp(updatedIp.getIp());

            // Update gateway route if needed
            if (updatedIp.getGatewayRoute() != null && updatedIp.getGatewayRoute().getId() != null) {
                GatewayRoute newRoute = gatewayRouteRepository.findById(updatedIp.getGatewayRoute().getId())
                        .orElseThrow(() -> new RuntimeException("Gateway route not found with id " + updatedIp.getGatewayRoute().getId()));
                GatewayRoute oldRoute = existingIp.getGatewayRoute();

                if (!oldRoute.getId().equals(newRoute.getId())) {
                    oldRoute.getAllowedIps().remove(existingIp);
                    gatewayRouteRepository.save(oldRoute);
                    newRoute.getAllowedIps().add(existingIp);
                    existingIp.setGatewayRoute(newRoute);
                    gatewayRouteRepository.save(newRoute);
                }
            }

            AllowedIps savedIp = allowedIpRepository.save(existingIp);

            // Trigger manual sync
            dataSyncService.syncRoutesToGatewaySchema();

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedIp.getId());
            response.put("ip", savedIp.getIp());
            response.put("gatewayRouteId", savedIp.getGatewayRouteId());
            response.put("predicate", savedIp.getGatewayRoute().getPredicates());
            response.put("routeUri", savedIp.getGatewayRoute().getUri());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update IP address: " + e.getMessage());
        }
    }

    // DELETE: Remove an IP address from both the database and its associated GatewayRoute
    @DeleteMapping("/{ipId}/gateway/{gatewayId}")
    @Transactional
    public ResponseEntity<?> deleteIpAddress(@PathVariable Long ipId, @PathVariable Long gatewayId) {
        try {
            AllowedIps ipToDelete = allowedIpRepository.findById(ipId)
                    .orElseThrow(() -> new RuntimeException("Allowed IP not found with id " + ipId));

            if (!ipToDelete.getGatewayRoute().getId().equals(gatewayId)) {
                throw new RuntimeException("IP address with id " + ipId +
                        " does not belong to gateway route with id " + gatewayId);
            }

            GatewayRoute route = ipToDelete.getGatewayRoute();
            route.getAllowedIps().remove(ipToDelete);

            // If this was the last IP, disable IP filtering on the route
            if (route.getAllowedIps().isEmpty()) {
                route.setWithIpFilter(false);
            }

            gatewayRouteRepository.save(route);
            allowedIpRepository.delete(ipToDelete);

            // Trigger manual sync
            dataSyncService.syncRoutesToGatewaySchema();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete IP address: " + e.getMessage());
        }
    }

    // DELETE: Remove all IPs for a route
    @DeleteMapping("/route/{routeId}")
    @Transactional
    public ResponseEntity<?> deleteAllIpsForRoute(@PathVariable Long routeId) {
        try {
            GatewayRoute route = gatewayRouteRepository.findById(routeId)
                    .orElseThrow(() -> new RuntimeException("Gateway route not found with id " + routeId));

            if (route.getAllowedIps() != null && !route.getAllowedIps().isEmpty()) {
                allowedIpRepository.deleteAll(route.getAllowedIps());
                route.getAllowedIps().clear();
                route.setWithIpFilter(false);
                gatewayRouteRepository.save(route);

                // Trigger manual sync
                dataSyncService.syncRoutesToGatewaySchema();
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete IP addresses: " + e.getMessage());
        }
    }

    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Simple regex for IPv4 validation
        String ipv4Pattern = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
        if (!ip.matches(ipv4Pattern)) {
            return false;
        }

        // Check each octet
        String[] octets = ip.split("\\.");
        for (String octet : octets) {
            int value = Integer.parseInt(octet);
            if (value < 0 || value > 255) {
                return false;
            }
        }

        return true;
    }
}