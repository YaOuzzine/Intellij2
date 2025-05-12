package com.example.gateway_admin.Services;

import com.example.gateway_admin.Entities.GatewayRoute;
import com.example.gateway_admin.Repositories.GatewayRouteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

@Service
public class DataSyncService {

    private static final Logger logger = Logger.getLogger(DataSyncService.class.getName());

    @Autowired
    private GatewayRouteRepository gatewayRouteRepository;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Synchronizes route data from admin schema to gateway schema.
     * Runs every 30 seconds by default.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void syncRoutesToGatewaySchema() {
        logger.info("Starting route synchronization to gateway schema...");

        try (Connection conn = dataSource.getConnection()) {
            // First, clear the gateway schema tables to prevent duplicates
            clearGatewayTables(conn);

            // Get all routes from admin schema
            List<GatewayRoute> routes = gatewayRouteRepository.findAll();

            // Insert each route into gateway schema
            for (GatewayRoute route : routes) {
                copyRouteToGatewaySchema(conn, route);
            }

            logger.info("Successfully synchronized " + routes.size() + " routes to gateway schema");
        } catch (SQLException e) {
            logger.severe("Error synchronizing data: " + e.getMessage());
            e.printStackTrace(); // Add this for more detailed error info
        }
    }

    private void clearGatewayTables(Connection conn) throws SQLException {
        // The order matters due to foreign key constraints
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM gateway.allowed_ips")) {
            int count = stmt.executeUpdate();
            logger.info("Cleared " + count + " rows from gateway.allowed_ips");
        } catch (SQLException e) {
            logger.severe("Error clearing gateway.allowed_ips: " + e.getMessage());
            throw e;
        }

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM gateway.rate_limit")) {
            int count = stmt.executeUpdate();
            logger.info("Cleared " + count + " rows from gateway.rate_limit");
        } catch (SQLException e) {
            logger.severe("Error clearing gateway.rate_limit: " + e.getMessage());
            throw e;
        }

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM gateway.gateway_routes")) {
            int count = stmt.executeUpdate();
            logger.info("Cleared " + count + " rows from gateway.gateway_routes");
        } catch (SQLException e) {
            logger.severe("Error clearing gateway.gateway_routes: " + e.getMessage());
            throw e;
        }
    }

    private void copyRouteToGatewaySchema(Connection conn, GatewayRoute route) throws SQLException {
        // Insert route
        String insertRouteSql =
                "INSERT INTO gateway.gateway_routes (id, uri, route_id, predicates, with_ip_filter, with_token, with_rate_limit) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(insertRouteSql)) {
            stmt.setLong(1, route.getId());
            stmt.setString(2, route.getUri());
            stmt.setString(3, route.getRouteId());
            stmt.setString(4, route.getPredicates());
            stmt.setBoolean(5, route.getWithIpFilter());
            stmt.setBoolean(6, route.getWithToken());
            stmt.setBoolean(7, route.getWithRateLimit());
            stmt.executeUpdate();
            logger.info("Synchronized route: " + route.getId() + " - " + route.getPredicates());
        } catch (SQLException e) {
            logger.severe("Error inserting route " + route.getId() + ": " + e.getMessage());
            throw e; // Rethrow to ensure we don't silently ignore
        }

        // Copy rate limit if exists
        if (route.getRateLimit() != null) {
            String insertRateLimitSql =
                    "INSERT INTO gateway.rate_limit (id, route_id, max_requests, time_window_ms) " +
                            "VALUES (?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(insertRateLimitSql)) {
                stmt.setLong(1, route.getRateLimit().getId());
                stmt.setLong(2, route.getId());
                stmt.setInt(3, route.getRateLimit().getMaxRequests());
                stmt.setInt(4, route.getRateLimit().getTimeWindowMs());
                stmt.executeUpdate();
                logger.info("Synchronized rate limit for route: " + route.getId());
            } catch (SQLException e) {
                logger.severe("Error inserting rate limit for route " + route.getId() + ": " + e.getMessage());
                // Continue with other operations
            }
        }

        // Copy allowed IPs if any
        if (route.getAllowedIps() != null && !route.getAllowedIps().isEmpty()) {
            String insertIpSql =
                    "INSERT INTO gateway.allowed_ips (id, gateway_route_id, ip) " +
                            "VALUES (?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(insertIpSql)) {
                for (var ip : route.getAllowedIps()) {
                    stmt.setLong(1, ip.getId());
                    stmt.setLong(2, route.getId());
                    stmt.setString(3, ip.getIp());
                    stmt.executeUpdate();
                    logger.info("Synchronized IP: " + ip.getIp() + " for route: " + route.getId());
                }
            } catch (SQLException e) {
                logger.severe("Error inserting IP addresses for route " + route.getId() + ": " + e.getMessage());
                // Continue with other operations
            }
        } else {
            logger.info("No IPs to synchronize for route: " + route.getId());
        }
    }
}