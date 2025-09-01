package com.serpapi.flightmcp.controller;

import com.serpapi.flightmcp.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for flight search operations.
 * Handles HTTP requests for flight data and MCP protocol endpoints.
 */
@RestController
@RequestMapping("/api/flights")
public class FlightController {
    
    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);
    
    private final FlightService flightService;
    
    public FlightController(FlightService flightService) {
        this.flightService = flightService;
        logger.info("FlightController initialized");
    }
    
    /**
     * Search for flights between departure and arrival locations.
     * 
     * @param departure Departure airport code or city
     * @param arrival Arrival airport code or city
     * @param date Flight date in YYYY-MM-DD format
     * @return Formatted flight search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchFlights(
            @RequestParam String departure,
            @RequestParam String arrival,
            @RequestParam String date) {
        
        logger.info("Flight search request: {} -> {} on {}", departure, arrival, date);
        
        try {
            // Get raw flight data
            String flightData = flightService.searchFlights(departure, arrival, date);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", flightData,
                "query", Map.of(
                    "departure", departure,
                    "arrival", arrival,
                    "date", date
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error processing flight search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search flights: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Health check endpoint for the flight service.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "flight-mcp-server",
            "timestamp", System.currentTimeMillis()
        ));
    }
}