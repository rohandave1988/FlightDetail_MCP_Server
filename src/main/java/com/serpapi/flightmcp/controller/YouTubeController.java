package com.serpapi.flightmcp.controller;

import com.serpapi.flightmcp.service.YouTubeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for YouTube search operations.
 * Handles HTTP requests for YouTube video data via SerpAPI YouTube integration.
 */
@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeController.class);
    
    private final YouTubeService youtubeService;
    
    public YouTubeController(YouTubeService youtubeService) {
        this.youtubeService = youtubeService;
        logger.info("YouTubeController initialized");
    }
    
    /**
     * Search for YouTube videos.
     * 
     * @param query Search query for videos
     * @param duration Optional duration filter (short, medium, long)
     * @param uploadDate Optional upload date filter (hour, today, week, month, year)
     * @param sortBy Optional sort order (relevance, upload_date, view_count, rating)
     * @param maxResults Maximum number of results to return (default: 20, max: 50)
     * @return YouTube search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(
            @RequestParam String query,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String uploadDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "20") Integer maxResults) {
        
        logger.info("YouTube search request: '{}' with filters - duration: {}, uploadDate: {}, sortBy: {}, maxResults: {}", 
                   query, duration, uploadDate, sortBy, maxResults);
        
        try {
            String videoData = youtubeService.searchVideos(query, duration, uploadDate, sortBy, maxResults);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", videoData,
                "query", Map.of(
                    "search_query", query,
                    "duration", duration != null ? duration : "any",
                    "upload_date", uploadDate != null ? uploadDate : "any",
                    "sort_by", sortBy != null ? sortBy : "relevance",
                    "max_results", maxResults
                ),
                "type", "youtube_videos"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing YouTube search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search YouTube videos: " + e.getMessage(),
                    "type", "youtube_videos"
                ));
        }
    }
    
    /**
     * Simple YouTube video search with just a query.
     * 
     * @param query Search query for videos
     * @return YouTube search results
     */
    @GetMapping("/videos/search")
    public ResponseEntity<?> searchVideosSimple(@RequestParam String query) {
        
        logger.info("Simple YouTube search request: '{}'", query);
        
        try {
            String videoData = youtubeService.searchVideos(query);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", videoData,
                "query", Map.of(
                    "search_query", query,
                    "type", "simple_search"
                ),
                "type", "youtube_videos"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing simple YouTube search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search YouTube videos: " + e.getMessage(),
                    "type", "youtube_videos"
                ));
        }
    }
    
    /**
     * Search for recent YouTube videos (uploaded today).
     * 
     * @param query Search query for videos
     * @param maxResults Maximum number of results (default: 10)
     * @return Recent YouTube search results
     */
    @GetMapping("/recent")
    public ResponseEntity<?> searchRecentVideos(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") Integer maxResults) {
        
        logger.info("Recent YouTube search request: '{}' with maxResults: {}", query, maxResults);
        
        try {
            String videoData = youtubeService.searchVideos(query, null, "today", "upload_date", maxResults);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", videoData,
                "query", Map.of(
                    "search_query", query,
                    "upload_date", "today",
                    "sort_by", "upload_date",
                    "max_results", maxResults
                ),
                "type", "youtube_recent"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing recent YouTube search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search recent YouTube videos: " + e.getMessage(),
                    "type", "youtube_recent"
                ));
        }
    }
    
    /**
     * Search for popular YouTube videos (sorted by view count).
     * 
     * @param query Search query for videos
     * @param duration Optional duration filter (short, medium, long)
     * @param maxResults Maximum number of results (default: 15)
     * @return Popular YouTube search results
     */
    @GetMapping("/popular")
    public ResponseEntity<?> searchPopularVideos(
            @RequestParam String query,
            @RequestParam(required = false) String duration,
            @RequestParam(defaultValue = "15") Integer maxResults) {
        
        logger.info("Popular YouTube search request: '{}' with duration: {}, maxResults: {}", query, duration, maxResults);
        
        try {
            String videoData = youtubeService.searchVideos(query, duration, null, "view_count", maxResults);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", videoData,
                "query", Map.of(
                    "search_query", query,
                    "duration", duration != null ? duration : "any",
                    "sort_by", "view_count",
                    "max_results", maxResults
                ),
                "type", "youtube_popular"
            ));
            
        } catch (Exception e) {
            logger.error("Error processing popular YouTube search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to search popular YouTube videos: " + e.getMessage(),
                    "type", "youtube_popular"
                ));
        }
    }
    
    /**
     * Health check endpoint for the YouTube service.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "youtube-mcp-server",
            "timestamp", System.currentTimeMillis(),
            "endpoints", Map.of(
                "search", "/api/youtube/search",
                "videos", "/api/youtube/videos/search", 
                "recent", "/api/youtube/recent",
                "popular", "/api/youtube/popular"
            ),
            "filters", Map.of(
                "duration", new String[]{"short", "medium", "long"},
                "uploadDate", new String[]{"hour", "today", "week", "month", "year"},
                "sortBy", new String[]{"relevance", "upload_date", "view_count", "rating"}
            )
        ));
    }
}