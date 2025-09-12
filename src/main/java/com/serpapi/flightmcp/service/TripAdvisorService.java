package com.serpapi.flightmcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serpapi.flightmcp.util.DebugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.io.ClassPathResource;

@Service
public class TripAdvisorService {
    private static final Logger logger = LoggerFactory.getLogger(TripAdvisorService.class);
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClientTokenService clientTokenService;
    
    @Value("${tripadvisor.serpapi.key:}")
    private String serpApiKey;
    
    // TripAdvisor uses the same base configuration as flights
    @Value("${serp.api.base.url:https://serpapi.com/search.json}")
    private String baseUrl;
    
    @Value("${serp.api.timeout.seconds:30}")
    private int timeoutSeconds;
    
    public TripAdvisorService(ClientTokenService clientTokenService) {
        this.clientTokenService = clientTokenService;
    }
    
    @PostConstruct
    private void init() {
        // Debug: Log initial API key value
        DebugUtil.debug("TripAdvisor Initial serpApiKey from @Value: '" + serpApiKey + "'");
        
        // Also check environment variable as fallback
        String envApiKey = System.getenv("TRIPADVISOR_SERPAPI_KEY");
        DebugUtil.debug("Environment variable TRIPADVISOR_SERPAPI_KEY: '" + envApiKey + "'");
        
        if (serpApiKey == null || serpApiKey.isEmpty()) {
            serpApiKey = envApiKey;
            if (serpApiKey == null) serpApiKey = "";
        }
        
        DebugUtil.debug("TripAdvisor Final serpApiKey: '" + serpApiKey + "'");
        DebugUtil.debug("TripAdvisor API Key configured: " + !serpApiKey.isEmpty());
        
        logger.info("TripAdvisorService initialized with SERP API config. API Key configured: {}", !serpApiKey.isEmpty());
        if (!serpApiKey.isEmpty()) {
            logger.info("TripAdvisor SERP API key found, will use real TripAdvisor data");
        } else {
            logger.info("No TripAdvisor SERP API key found, will use mock data");
        }
    }

    /**
     * Searches for hotels using TripAdvisor via SerpAPI or returns mock data if API key is not configured.
     * 
     * @param location The location to search for hotels
     * @param checkIn Check-in date in YYYY-MM-DD format
     * @param checkOut Check-out date in YYYY-MM-DD format
     * @param adults Number of adults
     * @return JSON string containing hotel search results
     * @throws Exception if the HTTP request fails
     */
    private String getApiKey(String clientId) {
        String clientApiKey = clientTokenService.getApiKey(clientId, "TRIPADVISOR_SERPAPI_KEY");
        return clientApiKey != null ? clientApiKey : serpApiKey;
    }
    
    public String searchHotels(String location, String checkIn, String checkOut, int adults, String clientId) throws Exception {
        String apiKeyToUse = getApiKey(clientId);
        DebugUtil.debug("TripAdvisor searchHotels called - serpApiKey.isEmpty(): " + serpApiKey.isEmpty());
        DebugUtil.debug("TripAdvisor serpApiKey length: " + (serpApiKey != null ? serpApiKey.length() : "null"));
        DebugUtil.debug("TripAdvisor Searching hotels in: " + location + " from " + checkIn + " to " + checkOut + " for " + adults + " adults");
        
        logger.info("Searching hotels in {} from {} to {} for {} adults", location, checkIn, checkOut, adults);
        logger.info("TripAdvisor API Key status: {}", serpApiKey.isEmpty() ? "EMPTY - using mock data" : "CONFIGURED - using SERP API");
        
        if (apiKeyToUse == null || apiKeyToUse.isEmpty()) {
            logger.error("No TripAdvisor SERP API key available for client {} and no system fallback", clientId);
            throw new Exception("TripAdvisor SERP API key not configured for client: " + clientId);
        }
        
        logger.debug("Using SERP API for TripAdvisor hotel search");
        String url = String.format(
            "%s?engine=tripadvisor&q=%s&check_in=%s&check_out=%s&adults=%d&api_key=%s",
            baseUrl,
            URLEncoder.encode(location, StandardCharsets.UTF_8),
            URLEncoder.encode(checkIn, StandardCharsets.UTF_8), 
            URLEncoder.encode(checkOut, StandardCharsets.UTF_8), 
            adults,
            apiKeyToUse
        );
        
        logger.debug("Making request to TripAdvisor SERP API: {}", url.replaceAll("api_key=[^&]*", "api_key=***"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "FlightMcpServer/1.0")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("TripAdvisor SERP API error: HTTP {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("TripAdvisor API request failed: HTTP " + response.statusCode());
        }
        
        logger.debug("TripAdvisor SERP API response received successfully");
        return response.body();
    }

    /**
     * Searches for restaurants using TripAdvisor via SerpAPI or returns mock data if API key is not configured.
     * 
     * @param location The location to search for restaurants
     * @param cuisine Optional cuisine type filter
     * @return JSON string containing restaurant search results
     * @throws Exception if the HTTP request fails
     */
    public String searchRestaurants(String location, String cuisine, String clientId) throws Exception {
        String apiKeyToUse = getApiKey(clientId);
        
        if (apiKeyToUse == null || apiKeyToUse.isEmpty()) {
            logger.error("No TripAdvisor SERP API key available for client {} and no system fallback", clientId);
            throw new Exception("TripAdvisor SERP API key not configured for client: " + clientId);
        }
        
        logger.debug("Using SERP API for TripAdvisor restaurant search");
        String queryParam = location;
        if (cuisine != null && !cuisine.isEmpty()) {
            queryParam += " " + cuisine + " restaurants";
        } else {
            queryParam += " restaurants";
        }
        
        String url = String.format(
            "%s?engine=tripadvisor&q=%s&api_key=%s",
            baseUrl,
            URLEncoder.encode(queryParam, StandardCharsets.UTF_8),
            apiKeyToUse
        );
        
        logger.debug("Making request to TripAdvisor SERP API: {}", url.replaceAll("api_key=[^&]*", "api_key=***"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "FlightMcpServer/1.0")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("TripAdvisor SERP API error: HTTP {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("TripAdvisor API request failed: HTTP " + response.statusCode());
        }
        
        logger.debug("TripAdvisor SERP API response received successfully");
        return response.body();
    }

    /**
     * Searches for attractions using TripAdvisor via SerpAPI or returns mock data if API key is not configured.
     * 
     * @param location The location to search for attractions
     * @param category Optional category filter (e.g., museums, parks, tours)
     * @return JSON string containing attraction search results
     * @throws Exception if the HTTP request fails
     */
    public String searchAttractions(String location, String category, String clientId) throws Exception {
        String apiKeyToUse = getApiKey(clientId);
        
        if (apiKeyToUse == null || apiKeyToUse.isEmpty()) {
            logger.error("No TripAdvisor SERP API key available for client {} and no system fallback", clientId);
            throw new Exception("TripAdvisor SERP API key not configured for client: " + clientId);
        }
        
        logger.debug("Using SERP API for TripAdvisor attraction search");
        String queryParam = location;
        if (category != null && !category.isEmpty()) {
            queryParam += " " + category;
        } else {
            queryParam += " attractions";
        }
        
        String url = String.format(
            "%s?engine=tripadvisor&q=%s&api_key=%s",
            baseUrl,
            URLEncoder.encode(queryParam, StandardCharsets.UTF_8),
            apiKeyToUse
        );
        
        logger.debug("Making request to TripAdvisor SERP API: {}", url.replaceAll("api_key=[^&]*", "api_key=***"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "FlightMcpServer/1.0")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("TripAdvisor SERP API error: HTTP {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("TripAdvisor API request failed: HTTP " + response.statusCode());
        }
        
        logger.debug("TripAdvisor SERP API response received successfully");
        return response.body();
    }

    /**
     * Returns mock hotel data when API key is not configured.
     * Reads template from mock-hotel-data.json file.
     */
    private String mockHotelData(String location, String checkIn, String checkOut, int adults) {
        logger.debug("Generating mock hotel data for {} from {} to {} for {} adults", location, checkIn, checkOut, adults);
        
        try {
            ClassPathResource resource = new ClassPathResource("mock-hotel-data.json");
            JsonNode mockData = objectMapper.readTree(resource.getInputStream());
            String template = mockData.get("template").asText();
            
            return String.format(template, 
                location, location, checkIn, checkOut, adults, // search_metadata
                location, location, location, // first hotel
                location, location, location, // second hotel 
                location, location, location); // third hotel
        } catch (IOException e) {
            logger.error("Failed to load mock hotel data template: {}", e.getMessage());
            // Fallback to minimal mock data
            return String.format("""
                {
                    "search_metadata": {"status": "Success", "query": "%s hotels", "location": "%s"},
                    "hotels": [{"name": "Sample Hotel %s", "rating": 4.0, "price": "$150/night"}],
                    "mock_data": true
                }
                """, location, location, location);
        }
    }

    /**
     * Returns mock restaurant data when API key is not configured.
     * Reads template from mock-restaurant-data.json file.
     */
    private String mockRestaurantData(String location, String cuisine) {
        logger.debug("Generating mock restaurant data for {} with cuisine: {}", location, cuisine);
        
        String cuisineType = (cuisine != null && !cuisine.isEmpty()) ? cuisine : "International";
        
        try {
            ClassPathResource resource = new ClassPathResource("mock-restaurant-data.json");
            JsonNode mockData = objectMapper.readTree(resource.getInputStream());
            String template = mockData.get("template").asText();
            
            return String.format(template, 
                location, cuisineType, location, // search_metadata
                cuisineType, cuisineType, location, cuisineType, location, // first restaurant
                cuisineType, cuisineType, location, cuisineType, location, // second restaurant
                location, location, location); // third restaurant
        } catch (IOException e) {
            logger.error("Failed to load mock restaurant data template: {}", e.getMessage());
            // Fallback to minimal mock data
            return String.format("""
                {
                    "search_metadata": {"status": "Success", "query": "%s restaurants", "location": "%s"},
                    "restaurants": [{"name": "Sample Restaurant %s", "rating": 4.0, "cuisine": "%s"}],
                    "mock_data": true
                }
                """, location, location, location, cuisineType);
        }
    }

    /**
     * Returns mock attraction data when API key is not configured.
     * Reads template from mock-attraction-data.json file.
     */
    private String mockAttractionData(String location, String category) {
        logger.debug("Generating mock attraction data for {} with category: {}", location, category);
        
        String categoryType = (category != null && !category.isEmpty()) ? category : "attractions";
        
        try {
            ClassPathResource resource = new ClassPathResource("mock-attraction-data.json");
            JsonNode mockData = objectMapper.readTree(resource.getInputStream());
            String template = mockData.get("template").asText();
            
            return String.format(template, 
                location, categoryType, location, // search_metadata
                location, location, location, // first attraction
                location, location, location, // second attraction
                location, location, location); // third attraction
        } catch (IOException e) {
            logger.error("Failed to load mock attraction data template: {}", e.getMessage());
            // Fallback to minimal mock data
            return String.format("""
                {
                    "search_metadata": {"status": "Success", "query": "%s attractions", "location": "%s"},
                    "attractions": [{"name": "Sample Attraction %s", "rating": 4.0, "category": "%s"}],
                    "mock_data": true
                }
                """, location, location, location, categoryType);
        }
    }
    
    // Legacy methods for backward compatibility
    public String searchHotels(String location, String checkIn, String checkOut, int adults) throws Exception {
        return searchHotels(location, checkIn, checkOut, adults, "system");
    }
    
    public String searchRestaurants(String location, String cuisine) throws Exception {
        return searchRestaurants(location, cuisine, "system");
    }
    
    public String searchAttractions(String location, String category) throws Exception {
        return searchAttractions(location, category, "system");
    }
}