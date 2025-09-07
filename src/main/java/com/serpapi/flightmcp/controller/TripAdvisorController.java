package com.serpapi.flightmcp.controller;

import com.serpapi.flightmcp.service.TripAdvisorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for TripAdvisor search operations.
 * Handles HTTP requests for hotel, restaurant, and attraction data via SerpAPI TripAdvisor integration.
 */
@RestController
@RequestMapping("/api/tripadvisor")
public class TripAdvisorController {
    
    private static final Logger logger = LoggerFactory.getLogger(TripAdvisorController.class);
    
    private final TripAdvisorService tripAdvisorService;
    
    public TripAdvisorController(TripAdvisorService tripAdvisorService) {
        this.tripAdvisorService = tripAdvisorService;
        logger.info("TripAdvisorController initialized");
    }
    
    /**
     * Search for hotels in a specific location.
     * 
     * @param location Location to search for hotels
     * @param checkIn Check-in date in YYYY-MM-DD format
     * @param checkOut Check-out date in YYYY-MM-DD format
     * @param adults Number of adults (default: 2)
     * @return Hotel search results
     */
    @GetMapping("/hotels/search")
    public ResponseEntity<?> searchHotels(
            @RequestParam String location,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(defaultValue = "2") int adults) {
        
        logger.info("Hotel search request: {} from {} to {} for {} adults", location, checkIn, checkOut, adults);
        
        try {
            String hotelData = tripAdvisorService.searchHotels(location, checkIn, checkOut, adults);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", hotelData,
                "query", Map.of(
                    "location", location,
                    "checkIn", checkIn,
                    "checkOut", checkOut,
                    "adults", adults
                ),
                "type", "hotels"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing hotel search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search hotels: " + e.getMessage(),
                    "type", "hotels"
                ));
        }
    }
    
    /**
     * Search for restaurants in a specific location.
     * 
     * @param location Location to search for restaurants
     * @param cuisine Optional cuisine type filter
     * @return Restaurant search results
     */
    @GetMapping("/restaurants/search")
    public ResponseEntity<?> searchRestaurants(
            @RequestParam String location,
            @RequestParam(required = false) String cuisine) {
        
        logger.info("Restaurant search request: {} with cuisine: {}", location, cuisine);
        
        try {
            String restaurantData = tripAdvisorService.searchRestaurants(location, cuisine);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", restaurantData,
                "query", Map.of(
                    "location", location,
                    "cuisine", cuisine != null ? cuisine : "all"
                ),
                "type", "restaurants"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing restaurant search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search restaurants: " + e.getMessage(),
                    "type", "restaurants"
                ));
        }
    }
    
    /**
     * Search for attractions in a specific location.
     * 
     * @param location Location to search for attractions
     * @param category Optional category filter (e.g., museums, parks, tours)
     * @return Attraction search results
     */
    @GetMapping("/attractions/search")
    public ResponseEntity<?> searchAttractions(
            @RequestParam String location,
            @RequestParam(required = false) String category) {
        
        logger.info("Attraction search request: {} with category: {}", location, category);
        
        try {
            String attractionData = tripAdvisorService.searchAttractions(location, category);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", attractionData,
                "query", Map.of(
                    "location", location,
                    "category", category != null ? category : "all"
                ),
                "type", "attractions"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing attraction search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search attractions: " + e.getMessage(),
                    "type", "attractions"
                ));
        }
    }
    
    /**
     * Combined search endpoint that searches all TripAdvisor categories.
     * 
     * @param location Location to search
     * @param checkIn Optional check-in date for hotels
     * @param checkOut Optional check-out date for hotels
     * @param adults Optional number of adults for hotels (default: 2)
     * @param cuisine Optional cuisine filter for restaurants
     * @param category Optional category filter for attractions
     * @return Combined search results from all categories
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchAll(
            @RequestParam String location,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String category) {
        
        logger.info("Combined TripAdvisor search request for location: {}", location);
        
        try {
            Map<String, Object> results = Map.of(
                "location", location,
                "hotels", searchHotelsInternal(location, checkIn, checkOut, adults),
                "restaurants", searchRestaurantsInternal(location, cuisine),
                "attractions", searchAttractionsInternal(location, category)
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", results,
                "query", Map.of(
                    "location", location,
                    "checkIn", checkIn,
                    "checkOut", checkOut,
                    "adults", adults,
                    "cuisine", cuisine,
                    "category", category
                ),
                "type", "combined"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing combined search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to perform combined search: " + e.getMessage(),
                    "type", "combined"
                ));
        }
    }
    
    /**
     * Health check endpoint for the TripAdvisor service.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "tripadvisor-mcp-server",
            "timestamp", System.currentTimeMillis(),
            "endpoints", Map.of(
                "hotels", "/api/tripadvisor/hotels/search",
                "restaurants", "/api/tripadvisor/restaurants/search",
                "attractions", "/api/tripadvisor/attractions/search",
                "combined", "/api/tripadvisor/search"
            )
        ));
    }
    
    // Internal methods for combined search
    private String searchHotelsInternal(String location, String checkIn, String checkOut, int adults) {
        try {
            if (checkIn != null && checkOut != null) {
                return tripAdvisorService.searchHotels(location, checkIn, checkOut, adults);
            } else {
                // Use default dates if not provided
                String defaultCheckIn = "2025-12-01";
                String defaultCheckOut = "2025-12-03";
                return tripAdvisorService.searchHotels(location, defaultCheckIn, defaultCheckOut, adults);
            }
        } catch (Exception e) {
            logger.warn("Hotel search failed in combined search: {}", e.getMessage());
            return "{\"error\": \"Hotel search failed\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
    
    private String searchRestaurantsInternal(String location, String cuisine) {
        try {
            return tripAdvisorService.searchRestaurants(location, cuisine);
        } catch (Exception e) {
            logger.warn("Restaurant search failed in combined search: {}", e.getMessage());
            return "{\"error\": \"Restaurant search failed\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
    
    private String searchAttractionsInternal(String location, String category) {
        try {
            return tripAdvisorService.searchAttractions(location, category);
        } catch (Exception e) {
            logger.warn("Attraction search failed in combined search: {}", e.getMessage());
            return "{\"error\": \"Attraction search failed\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}