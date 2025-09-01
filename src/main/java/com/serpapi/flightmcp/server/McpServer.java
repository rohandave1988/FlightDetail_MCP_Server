package com.serpapi.flightmcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serpapi.flightmcp.service.FlightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP (Model Context Protocol) server implementation for flight search functionality.
 * Handles tool registration and execution for flight-related operations.
 */
@Component
public class McpServer {
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlightService flightService;

    public McpServer(FlightService flightService) {
        this.flightService = flightService;
        logger.info("McpServer initialized with FlightService");
    }

    /**
     * Handles incoming MCP requests and routes them to appropriate handlers.
     * 
     * @param request The MCP request containing method and parameters
     * @return Response map containing results or error information
     */
    public Map<String, Object> handleRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        boolean isNotification = id == null;
        logger.info("Handling MCP request with method: {} (notification: {})", method, isNotification);
        
        return switch (method) {
            case "initialize" -> {
                logger.debug("Handling initialize request");
                if (isNotification) {
                    yield null; // Don't respond to notifications
                }
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                            "tools", Map.of()
                        ),
                        "serverInfo", Map.of(
                            "name", "flight-mcp-server",
                            "version", "1.0.0"
                        )
                    )
                );
            }
            case "initialized" -> {
                logger.debug("Handling initialized notification");
                yield null; // initialized is always a notification, don't respond
            }
            case "tools/list" -> {
                logger.debug("Returning available tools list");
                if (isNotification) {
                    yield null; // Don't respond to notifications
                }
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "tools", List.of(
                            Map.of(
                                "name", "search_flights",
                                "description", "Search for flight information using departure and arrival locations",
                                "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                        "departure", Map.of("type", "string", "description", "Departure city or airport"),
                                        "arrival", Map.of("type", "string", "description", "Arrival city or airport"),
                                        "date", Map.of("type", "string", "description", "Departure date (YYYY-MM-DD)")
                                    ),
                                    "required", List.of("departure", "arrival", "date")
                                )
                            )
                        )
                    )
                );
            }
            case "tools/call" -> handleToolCall(request);
            default -> {
                logger.warn("Unknown method requested: {}", method);
                if (isNotification) {
                    yield null; // Don't respond to notifications
                }
                yield Map.of(
                    "jsonrpc", "2.0", 
                    "id", id,
                    "error", Map.of(
                        "code", -32601,
                        "message", "Method not found: " + method
                    )
                );
            }
        };
    }

    /**
     * Handles MCP tool call requests by executing the specified tool with provided arguments.
     * 
     * @param request The MCP request containing tool name and arguments
     * @return Response map containing tool execution results or error information
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolCall(Map<String, Object> request) {
        Object id = request.get("id");
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        logger.info("Executing tool call: {} with arguments: {}", name, arguments);

        if ("search_flights".equals(name)) {
            try {
                String departure = (String) arguments.get("departure");
                String arrival = (String) arguments.get("arrival");
                String date = (String) arguments.get("date");
                
                logger.debug("Calling FlightService.searchFlights with: {}, {}, {}", departure, arrival, date);
                String flightData = flightService.searchFlights(departure, arrival, date);
                
                logger.info("Successfully processed flight search request, returning raw flight data");
                return Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of(
                        "content", List.of(Map.of(
                            "type", "text",
                            "text", flightData
                        ))
                    )
                );
            } catch (Exception e) {
                logger.error("Failed to execute search_flights tool: {}", e.getMessage(), e);
                return Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "error", Map.of(
                        "code", -32000,
                        "message", "Failed to search flights: " + e.getMessage()
                    )
                );
            }
        }
        
        logger.warn("Unknown tool requested: {}", name);
        return Map.of(
            "jsonrpc", "2.0",
            "id", id,
            "error", Map.of(
                "code", -32601,
                "message", "Unknown tool: " + name
            )
        );
    }
}