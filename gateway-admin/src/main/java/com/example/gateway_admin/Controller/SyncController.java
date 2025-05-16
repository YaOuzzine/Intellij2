package com.example.gateway_admin.Controller;

import com.example.gateway_admin.Services.DataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller to manually trigger data synchronization
 */
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);
    private final DataSyncService dataSyncService;

    @Autowired
    public SyncController(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    /**
     * Endpoint to manually trigger a sync
     * Removed any authentication requirements to match other endpoints
     */
    @PostMapping("/routes")
    public ResponseEntity<Map<String, Object>> syncRoutes() {
        logger.info("Manual synchronization requested");

        try {
            // Call the sync method
            dataSyncService.syncRoutesToGatewaySchema();

            // Prepare success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("message", "Routes synchronized successfully");

            logger.info("Manual synchronization completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the detailed error
            logger.error("Error during manual synchronization", e);

            // Prepare detailed error response
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}