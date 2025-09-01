package com.serpapi.flightmcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serpapi.flightmcp.server.McpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Main Spring Boot application class for the Flight MCP Server.
 * Provides MCP protocol communication endpoint.
 */
@SpringBootApplication
@RestController
public class FlightMcpApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(FlightMcpApplication.class);
    
    private final McpServer mcpServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FlightMcpApplication(McpServer mcpServer) {
        this.mcpServer = mcpServer;
        logger.info("Flight MCP Application initialized");
    }

    /**
     * Application entry point.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Check if running in stdio mode (for Claude Desktop)
        boolean stdioMode = System.getProperty("mcp.stdio", "false").equals("true") || 
                           System.getenv("MCP_STDIO") != null ||
                           args.length > 0 && args[0].equals("--stdio");
        
        if (stdioMode) {
            // Set stdio profile early to suppress logs
            System.setProperty("spring.profiles.active", "stdio");
        } else {
            logger.info("Starting Flight MCP Server...");
        }
        
        SpringApplication app = new SpringApplication(FlightMcpApplication.class);
        if (stdioMode) {
            app.setBannerMode(Banner.Mode.OFF);
        }
        app.run(args);
    }

    /**
     * Handles MCP protocol requests via HTTP POST.
     * 
     * @param request The MCP request body containing method and parameters
     * @return Response map with results or error information
     */
    @PostMapping("/mcp")
    public Map<String, Object> handleMcpRequest(@RequestBody Map<String, Object> request) {
        logger.info("Received MCP request: {}", request.get("method"));
        Map<String, Object> response = mcpServer.handleRequest(request);
        logger.debug("Sending MCP response: {}", response.containsKey("error") ? "ERROR" : "SUCCESS");
        return response;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if running in stdio mode
        boolean stdioMode = System.getProperty("mcp.stdio", "false").equals("true") || 
                           System.getenv("MCP_STDIO") != null ||
                           (args.length > 0 && args[0].equals("--stdio"));
        
        if (stdioMode) {
            runStdioMode();
        }
    }

    private void runStdioMode() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> request = objectMapper.readValue(line, Map.class);
                    Map<String, Object> response = mcpServer.handleRequest(request);
                    if (response != null) {
                        System.out.println(objectMapper.writeValueAsString(response));
                        System.out.flush();
                    }
                } catch (Exception e) {
                    // Log to stderr instead of stdout to avoid interfering with MCP protocol
                    System.err.println("Error processing stdio request: " + e.getMessage());
                    // Try to extract ID from the original line for error response
                    Object errorId = null;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> request = objectMapper.readValue(line, Map.class);
                        errorId = request.get("id");
                    } catch (Exception ignored) {
                        // If we can't parse the request, we can't include an ID
                    }
                    
                    if (errorId != null) {
                        Map<String, Object> errorResponse = Map.of(
                            "jsonrpc", "2.0",
                            "id", errorId,
                            "error", Map.of(
                                "code", -32000,
                                "message", "Internal error: " + e.getMessage()
                            )
                        );
                        System.out.println(objectMapper.writeValueAsString(errorResponse));
                        System.out.flush();
                    }
                    // If no ID, don't send error response (was a notification)
                }
            }
        }
    }

}