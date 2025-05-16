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
     * This can be called manually or runs automatically every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void syncRoutesToGatewaySchema() {
        logger.info("Starting route synchronization to gateway schema...");

        try (Connection conn = dataSource.getConnection()) {
            // Add this line for debugging
            logDatabaseDebugInfo(conn);

            // Rest of your method remains the same
            clearGatewayTables(conn);

            // Get all routes from admin schema
            List<GatewayRoute> routes = gatewayRouteRepository.findAll();
            logger.info("Found " + routes.size() + " routes in admin schema to synchronize");

            // First, insert all rate limits
            for (GatewayRoute route : routes) {
                if (route.getRateLimit() != null) {
                    logger.info("Processing rate limit for route " + route.getId() +
                            " with max requests: " + route.getRateLimit().getMaxRequests() +
                            " and time window: " + route.getRateLimit().getTimeWindowMs() + "ms");
                    insertRateLimit(conn, route);
                }
            }

            // Now insert routes
            for (GatewayRoute route : routes) {
                logger.info("Processing route " + route.getId() + ": " + route.getPredicates() +
                        " -> " + route.getUri());
                insertRoute(conn, route);

                if (route.getAllowedIps() != null && !route.getAllowedIps().isEmpty()) {
                    logger.info("Route " + route.getId() + " has " + route.getAllowedIps().size() +
                            " allowed IPs");
                    insertAllowedIps(conn, route);
                }
            }

            // Add final debug info after sync
            logDatabaseDebugInfo(conn);

            logger.info("Successfully synchronized " + routes.size() + " routes to gateway schema");
        } catch (SQLException e) {
            logger.severe("Error synchronizing data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to synchronize routes: " + e.getMessage(), e);
        }
    }
    /**
     * Debug method to diagnose connection and database issues
     * This will log detailed information about the connection and database state
     */
    private void logDatabaseDebugInfo(Connection conn) {
        try {
            logger.info("--- Database Debug Information ---");

            // Log connection information
            logger.info("Connection valid: " + !conn.isClosed());
            logger.info("Connection auto-commit: " + conn.getAutoCommit());

            // Log database schemas
            try (java.sql.Statement stmt = conn.createStatement()) {
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT schema_name FROM information_schema.schemata")) {
                    logger.info("Available schemas:");
                    while (rs.next()) {
                        logger.info(" - " + rs.getString(1));
                    }
                }
            }

            // Check if the gateway schema exists and has the required tables
            try (java.sql.Statement stmt = conn.createStatement()) {
                try (java.sql.ResultSet rs = stmt.executeQuery(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'gateway'")) {
                    logger.info("Tables in gateway schema:");
                    while (rs.next()) {
                        logger.info(" - " + rs.getString(1));
                    }
                }
            }

            // Check the admin schema tables
            try (java.sql.Statement stmt = conn.createStatement()) {
                try (java.sql.ResultSet rs = stmt.executeQuery(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'admin'")) {
                    logger.info("Tables in admin schema:");
                    while (rs.next()) {
                        logger.info(" - " + rs.getString(1));
                    }
                }
            }

            // Count gateway routes in both schemas
            try (java.sql.Statement stmt = conn.createStatement()) {
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admin.gateway_routes")) {
                    if (rs.next()) {
                        logger.info("Number of routes in admin schema: " + rs.getInt(1));
                    }
                }
            }

            try (java.sql.Statement stmt = conn.createStatement()) {
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM gateway.gateway_routes")) {
                    if (rs.next()) {
                        logger.info("Number of routes in gateway schema: " + rs.getInt(1));
                    }
                }
            }

            logger.info("--- End of Database Debug Information ---");
        } catch (SQLException e) {
            logger.severe("Error getting debug information: " + e.getMessage());
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

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM gateway.gateway_routes")) {
            int count = stmt.executeUpdate();
            logger.info("Cleared " + count + " rows from gateway.gateway_routes");
        } catch (SQLException e) {
            logger.severe("Error clearing gateway.gateway_routes: " + e.getMessage());
            throw e;
        }

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM gateway.rate_limit")) {
            int count = stmt.executeUpdate();
            logger.info("Cleared " + count + " rows from gateway.rate_limit");
        } catch (SQLException e) {
            logger.severe("Error clearing gateway.rate_limit: " + e.getMessage());
            throw e;
        }
    }

    private void insertRateLimit(Connection conn, GatewayRoute route) throws SQLException {
        if (route.getRateLimit() == null) {
            return;
        }

        String insertRateLimitSql =
                "INSERT INTO gateway.rate_limit (id, route_id, max_requests, time_window_ms) " +
                        "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(insertRateLimitSql)) {
            stmt.setLong(1, route.getRateLimit().getId());
            stmt.setLong(2, route.getId());
            stmt.setInt(3, route.getRateLimit().getMaxRequests());
            stmt.setInt(4, route.getRateLimit().getTimeWindowMs());
            stmt.executeUpdate();
            logger.info("Inserted rate limit with ID: " + route.getRateLimit().getId() +
                    " for route: " + route.getId());
        } catch (SQLException e) {
            logger.severe("Error inserting rate limit for route " + route.getId() + ": " + e.getMessage());
            throw e; // Re-throw to abort the whole sync process
        }
    }

    private void insertRoute(Connection conn, GatewayRoute route) throws SQLException {
        String insertRouteSql =
                "INSERT INTO gateway.gateway_routes (id, uri, route_id, predicates, with_ip_filter, with_token, with_rate_limit, rate_limit_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(insertRouteSql)) {
            stmt.setLong(1, route.getId());
            stmt.setString(2, route.getUri());
            stmt.setString(3, route.getRouteId());
            stmt.setString(4, route.getPredicates());
            stmt.setBoolean(5, route.getWithIpFilter());
            stmt.setBoolean(6, route.getWithToken());
            stmt.setBoolean(7, route.getWithRateLimit());

            // Set rate_limit_id to the actual rate limit ID or null
            if (route.getRateLimit() != null) {
                stmt.setLong(8, route.getRateLimit().getId());
            } else {
                stmt.setNull(8, java.sql.Types.BIGINT);
            }

            stmt.executeUpdate();
            logger.info("Inserted route: " + route.getId() + " - " + route.getPredicates());
        } catch (SQLException e) {
            logger.severe("Error inserting route " + route.getId() + ": " + e.getMessage());
            throw e; // Rethrow to ensure we don't silently ignore
        }
    }

    private void insertAllowedIps(Connection conn, GatewayRoute route) throws SQLException {
        if (route.getAllowedIps() == null || route.getAllowedIps().isEmpty()) {
            return;
        }

        String insertIpSql =
                "INSERT INTO gateway.allowed_ips (id, gateway_route_id, ip) " +
                        "VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(insertIpSql)) {
            for (var ip : route.getAllowedIps()) {
                stmt.setLong(1, ip.getId());
                stmt.setLong(2, route.getId());
                stmt.setString(3, ip.getIp());
                stmt.executeUpdate();
                logger.info("Inserted IP: " + ip.getIp() + " for route: " + route.getId());
            }
        } catch (SQLException e) {
            logger.severe("Error inserting IP addresses for route " + route.getId() + ": " + e.getMessage());
            throw e; // Re-throw to abort the whole sync process
        }
    }
}