package com.serpapi.flightmcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serpapi.flightmcp.config.FlightMcpConfig;
import com.serpapi.flightmcp.util.DebugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class FlightService {
    private static final Logger logger = LoggerFactory.getLogger(FlightService.class);
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlightMcpConfig.SerpApiConfig apiConfig;
    private final FlightMcpConfig.MockDataConfig mockDataConfig;
    
    @Value("${flight.serpapi.key:}")
    private String serpApiKey;
    
    public FlightService(FlightMcpConfig.SerpApiConfig apiConfig, FlightMcpConfig.MockDataConfig mockDataConfig) {
        this.apiConfig = apiConfig;
        this.mockDataConfig = mockDataConfig;
    }
    
    @PostConstruct
    private void init() {
        // Debug: Log initial API key value
        DebugUtil.debug("Initial serpApiKey from @Value: '" + serpApiKey + "'");
        
        // Also check environment variable as fallback
        String envApiKey = System.getenv("FLIGHT_SERPAPI_KEY");
        DebugUtil.debug("Environment variable FLIGHT_SERPAPI_KEY: '" + envApiKey + "'");
        
        if (serpApiKey == null || serpApiKey.isEmpty()) {
            serpApiKey = envApiKey;
            if (serpApiKey == null) serpApiKey = "";
        }
        
        DebugUtil.debug("Final serpApiKey: '" + serpApiKey + "'");
        DebugUtil.debug("API Key configured: " + !serpApiKey.isEmpty());
        
        logger.info("FlightService initialized with SERP API config. API Key configured: {}", !serpApiKey.isEmpty());
        if (!serpApiKey.isEmpty()) {
            logger.info("SERP API key found, will use real flight data");
        } else {
            logger.info("No SERP API key found, will use mock data");
        }
    }

    /**
     * Searches for flight information using the SERP API or returns mock data if API key is not configured.
     * 
     * @param departure The departure city or airport code
     * @param arrival The arrival city or airport code  
     * @param date The departure date in YYYY-MM-DD format
     * @return JSON string containing flight search results
     * @throws Exception if the HTTP request fails
     */
    public String searchFlights(String departure, String arrival, String date) throws Exception {
        // Debug to stderr (won't interfere with JSON)
        DebugUtil.debug("searchFlights called - serpApiKey.isEmpty(): " + serpApiKey.isEmpty());
        DebugUtil.debug("serpApiKey length: " + (serpApiKey != null ? serpApiKey.length() : "null"));
        DebugUtil.debug("Searching: " + departure + " -> " + arrival + " on " + date);
        
        logger.info("Searching flights from {} to {} on {}", departure, arrival, date);
        logger.info("API Key status: {}", serpApiKey.isEmpty() ? "EMPTY - using mock data" : "CONFIGURED - using SERP API");
        
        if (serpApiKey.isEmpty()) {
            logger.warn("SERP API key not configured, using mock data");
            return mockFlightData(departure, arrival, date);
        }
        
        logger.debug("Using SERP API for flight search");
        String url = String.format(
            "%s?engine=%s&departure_id=%s&arrival_id=%s&outbound_date=%s&type=%s&api_key=%s",
            apiConfig.getBaseUrl(),
            apiConfig.getEngine(),
            URLEncoder.encode(departure, StandardCharsets.UTF_8), 
            URLEncoder.encode(arrival, StandardCharsets.UTF_8), 
            URLEncoder.encode(date, StandardCharsets.UTF_8), 
            apiConfig.getType(),
            serpApiKey
        );
        
        logger.debug("Making request to SERP API: {}", url.replaceAll("api_key=[^&]*", "api_key=***"));
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        
        DebugUtil.debug("About to send SERP API request...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        DebugUtil.debug("SERP API response status: " + response.statusCode());
        DebugUtil.debug("SERP API response body length: " + response.body().length());
        DebugUtil.debug("SERP API response body (first 200 chars): " + response.body().substring(0, Math.min(200, response.body().length())));
        
        logger.debug("SERP API response status: {}", response.statusCode());
        
        if (response.statusCode() == 200) {
            String responseBody = response.body();
            
            // Validate that response is valid JSON
            try {
                objectMapper.readTree(responseBody);
                DebugUtil.debug("SERP API JSON validation passed - using real data");
                logger.info("Successfully retrieved and validated flight data from SERP API");
                return responseBody;
            } catch (Exception e) {
                DebugUtil.debug("SERP API JSON validation failed: " + e.getMessage());
                logger.error("SERP API returned invalid JSON: {}", e.getMessage());
                logger.warn("Falling back to mock data due to invalid JSON response");
                return mockFlightData(departure, arrival, date);
            }
        }
        
        DebugUtil.debug("SERP API request failed - falling back to mock data");
        logger.warn("SERP API request failed with status {}, response body: {}", response.statusCode(), response.body());
        logger.warn("Falling back to mock data");
        return mockFlightData(departure, arrival, date);
    }
    
    /**
     * Generates mock flight data for testing purposes when SERP API is not available.
     * 
     * @param departure The departure city or airport code
     * @param arrival The arrival city or airport code
     * @param date The departure date
     * @return JSON string containing mock flight data
     */
    private String mockFlightData(String departure, String arrival, String date) {
        logger.debug("Generating mock flight data for {} to {} on {}", departure, arrival, date);
        
        String template = mockDataConfig.getFlightDataTemplate();
        String mockData = String.format(template,
            departure, arrival, date,  // search_parameters
            departure, departure, date,  // first flight departure
            arrival, arrival, date,      // first flight arrival  
            departure, departure, date,  // second flight departure
            arrival, arrival, date,      // second flight arrival
            departure, departure, date,  // third flight departure  
            arrival, arrival, date       // third flight arrival
        );
        
        logger.debug("Mock flight data generated successfully from template");
        return mockData;
    }
}