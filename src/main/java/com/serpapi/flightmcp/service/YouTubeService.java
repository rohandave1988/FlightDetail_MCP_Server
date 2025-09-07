package com.serpapi.flightmcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serpapi.flightmcp.util.DebugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class YouTubeService {
    private static final Logger logger = LoggerFactory.getLogger(YouTubeService.class);
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${youtube.serpapi.key:}")
    private String serpApiKey;
    
    // YouTube uses the same base configuration as other services
    @Value("${serp.api.base.url:https://serpapi.com/search.json}")
    private String baseUrl;
    
    @Value("${serp.api.timeout.seconds:30}")
    private int timeoutSeconds;
    
    @PostConstruct
    private void init() {
        // Debug: Log initial API key value
        DebugUtil.debug("YouTube Initial serpApiKey from @Value: '" + serpApiKey + "'");
        
        // Also check environment variable as fallback
        String envApiKey = System.getenv("YOUTUBE_SERPAPI_KEY");
        DebugUtil.debug("Environment variable YOUTUBE_SERPAPI_KEY: '" + envApiKey + "'");
        
        if (serpApiKey == null || serpApiKey.isEmpty()) {
            serpApiKey = envApiKey;
            if (serpApiKey == null) serpApiKey = "";
        }
        
        DebugUtil.debug("YouTube Final serpApiKey: '" + serpApiKey + "'");
        DebugUtil.debug("YouTube API Key configured: " + !serpApiKey.isEmpty());
        
        logger.info("YouTubeService initialized with SERP API config. API Key configured: {}", !serpApiKey.isEmpty());
        if (!serpApiKey.isEmpty()) {
            logger.info("YouTube SERP API key found, will use real YouTube data");
        } else {
            logger.info("No YouTube SERP API key found, will use mock data");
        }
    }

    /**
     * Searches for YouTube videos using SerpAPI or returns mock data if API key is not configured.
     * 
     * @param query The search query for YouTube videos
     * @param duration Optional duration filter (short, medium, long)
     * @param uploadDate Optional upload date filter (hour, today, week, month, year)
     * @param sortBy Optional sort order (relevance, upload_date, view_count, rating)
     * @param maxResults Maximum number of results to return (default: 20)
     * @return JSON string containing YouTube search results
     * @throws Exception if the HTTP request fails
     */
    public String searchVideos(String query, String duration, String uploadDate, String sortBy, Integer maxResults) throws Exception {
        DebugUtil.debug("YouTube searchVideos called - serpApiKey.isEmpty(): " + serpApiKey.isEmpty());
        DebugUtil.debug("YouTube Searching for: " + query + " duration: " + duration + " uploadDate: " + uploadDate + " sortBy: " + sortBy);
        
        logger.info("Searching YouTube videos for query: {} with filters - duration: {}, uploadDate: {}, sortBy: {}, maxResults: {}", 
                   query, duration, uploadDate, sortBy, maxResults);
        logger.info("YouTube API Key status: {}", serpApiKey.isEmpty() ? "EMPTY - using mock data" : "CONFIGURED - using SERP API");
        
        if (serpApiKey.isEmpty()) {
            logger.warn("YouTube SERP API key not configured, using mock data");
            return mockVideoData(query, duration, uploadDate, sortBy, maxResults);
        }
        
        logger.debug("Using SERP API for YouTube video search");
        
        // Build URL with optional parameters
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(String.format("%s?engine=youtube&search_query=%s&api_key=%s", 
                          baseUrl, URLEncoder.encode(query, StandardCharsets.UTF_8), serpApiKey));
        
        if (duration != null && !duration.isEmpty()) {
            urlBuilder.append("&sp=").append(getDurationFilter(duration));
        }
        if (uploadDate != null && !uploadDate.isEmpty()) {
            urlBuilder.append("&sp=").append(getUploadDateFilter(uploadDate));
        }
        if (sortBy != null && !sortBy.isEmpty()) {
            urlBuilder.append("&sp=").append(getSortByFilter(sortBy));
        }
        if (maxResults != null && maxResults > 0) {
            urlBuilder.append("&num=").append(Math.min(maxResults, 50)); // Limit to 50 max
        }
        
        String url = urlBuilder.toString();
        logger.debug("Making request to YouTube SERP API: {}", url.replaceAll("api_key=[^&]*", "api_key=***"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .header("User-Agent", "FlightMcpServer/1.0")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            logger.error("YouTube SERP API error: HTTP {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("YouTube API request failed: HTTP " + response.statusCode());
        }
        
        logger.debug("YouTube SERP API response received successfully");
        return response.body();
    }

    /**
     * Searches for YouTube videos with simple query (convenience method).
     */
    public String searchVideos(String query) throws Exception {
        return searchVideos(query, null, null, null, 20);
    }

    /**
     * Returns mock YouTube video data when API key is not configured.
     * Reads template from mock-youtube-data.json file.
     */
    private String mockVideoData(String query, String duration, String uploadDate, String sortBy, Integer maxResults) {
        logger.debug("Generating mock YouTube data for query: {} with filters", query);
        
        int resultCount = (maxResults != null && maxResults > 0) ? Math.min(maxResults, 20) : 20;
        
        try {
            ClassPathResource resource = new ClassPathResource("mock-youtube-data.json");
            JsonNode mockData = objectMapper.readTree(resource.getInputStream());
            String template = mockData.get("template").asText();
            
            return String.format(template, 
                query, // search_metadata.query
                query, query, // first video
                query, query, // second video
                query, query, // third video
                resultCount); // total_results
        } catch (IOException e) {
            logger.error("Failed to load mock YouTube data template: {}", e.getMessage());
            // Fallback to minimal mock data
            return String.format("""
                {
                    "search_metadata": {"status": "Success", "query": "%s"},
                    "video_results": [{"title": "Sample Video about %s", "views": "1.2M views", "duration": "5:30"}],
                    "mock_data": true
                }
                """, query, query);
        }
    }

    /**
     * Maps duration filter to YouTube search parameter.
     */
    private String getDurationFilter(String duration) {
        return switch (duration.toLowerCase()) {
            case "short" -> "EgIYAQ"; // Under 4 minutes
            case "medium" -> "EgIYAw"; // 4-20 minutes
            case "long" -> "EgIYAg"; // Over 20 minutes
            default -> "";
        };
    }

    /**
     * Maps upload date filter to YouTube search parameter.
     */
    private String getUploadDateFilter(String uploadDate) {
        return switch (uploadDate.toLowerCase()) {
            case "hour" -> "EgIIAQ"; // Last hour
            case "today" -> "EgIIAg"; // Today
            case "week" -> "EgIIAw"; // This week
            case "month" -> "EgIIBA"; // This month
            case "year" -> "EgIIBQ"; // This year
            default -> "";
        };
    }

    /**
     * Maps sort by filter to YouTube search parameter.
     */
    private String getSortByFilter(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "relevance" -> "CAASAhAB"; // Relevance (default)
            case "upload_date" -> "CAI"; // Upload date
            case "view_count" -> "CAM"; // View count
            case "rating" -> "CAE"; // Rating
            default -> "";
        };
    }
}