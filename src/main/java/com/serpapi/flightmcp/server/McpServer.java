package com.serpapi.flightmcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serpapi.flightmcp.service.ClientTokenService;
import com.serpapi.flightmcp.service.FlightService;
import com.serpapi.flightmcp.service.TripAdvisorService;
import com.serpapi.flightmcp.service.YouTubeService;
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
    private final TripAdvisorService tripAdvisorService;
    private final YouTubeService youtubeService;
    private final ClientTokenService clientTokenService;

    public McpServer(FlightService flightService, TripAdvisorService tripAdvisorService, YouTubeService youtubeService, ClientTokenService clientTokenService) {
        this.flightService = flightService;
        this.tripAdvisorService = tripAdvisorService;
        this.youtubeService = youtubeService;
        this.clientTokenService = clientTokenService;
        logger.info("McpServer initialized with FlightService, TripAdvisorService, YouTubeService, and ClientTokenService");
    }

    /**
     * Validates client token from environment variables
     * 
     * @return Map with validation result and client info
     */
    private Map<String, Object> validateClientToken() {
        String clientId = System.getenv("MCP_CLIENT_ID");
        String clientToken = System.getenv("MCP_CLIENT_TOKEN");
        
        if (clientId == null || clientToken == null) {
            logger.warn("Client validation failed: MCP_CLIENT_ID or MCP_CLIENT_TOKEN environment variables not set");
            return Map.of("valid", false, "error", "Client credentials not provided");
        }
        
        boolean isValid = clientTokenService.validateClientToken(clientId, clientToken);
        if (!isValid) {
            logger.warn("Client validation failed for clientId: {}", clientId);
            return Map.of("valid", false, "error", "Invalid client credentials");
        }
        
        ClientTokenService.ClientInfo clientInfo = clientTokenService.getClientInfo(clientId);
        logger.info("Client validated successfully: {} ({})", clientInfo.name, clientId);
        return Map.of("valid", true, "clientInfo", clientInfo);
    }
    
    /**
     * Gets the required permission for a tool
     * 
     * @param toolName The name of the tool
     * @return The required permission, or null if no specific permission is needed
     */
    private String getRequiredPermission(String toolName) {
        return switch (toolName) {
            case "search_flights" -> "flights";
            case "search_hotels", "search_restaurants", "search_attractions" -> "tripadvisor";
            case "search_youtube_videos" -> "youtube";
            default -> null;
        };
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
                            "name", "travel-mcp-server",
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
                            ),
                            Map.of(
                                "name", "search_hotels",
                                "description", "Search for hotels in a specific location using TripAdvisor data",
                                "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                        "location", Map.of("type", "string", "description", "Location to search for hotels"),
                                        "checkIn", Map.of("type", "string", "description", "Check-in date (YYYY-MM-DD)"),
                                        "checkOut", Map.of("type", "string", "description", "Check-out date (YYYY-MM-DD)"),
                                        "adults", Map.of("type", "integer", "description", "Number of adults", "default", 2)
                                    ),
                                    "required", List.of("location", "checkIn", "checkOut")
                                )
                            ),
                            Map.of(
                                "name", "search_restaurants",
                                "description", "Search for restaurants in a specific location using TripAdvisor data",
                                "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                        "location", Map.of("type", "string", "description", "Location to search for restaurants"),
                                        "cuisine", Map.of("type", "string", "description", "Optional cuisine type filter")
                                    ),
                                    "required", List.of("location")
                                )
                            ),
                            Map.of(
                                "name", "search_attractions",
                                "description", "Search for attractions and activities in a specific location using TripAdvisor data",
                                "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                        "location", Map.of("type", "string", "description", "Location to search for attractions"),
                                        "category", Map.of("type", "string", "description", "Optional category filter (e.g., museums, parks, tours)")
                                    ),
                                    "required", List.of("location")
                                )
                            ),
                            Map.of(
                                "name", "search_youtube_videos",
                                "description", "Search for YouTube videos using SerpAPI YouTube search",
                                "inputSchema", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                        "query", Map.of("type", "string", "description", "Search query for YouTube videos"),
                                        "duration", Map.of("type", "string", "description", "Optional duration filter (short, medium, long)"),
                                        "uploadDate", Map.of("type", "string", "description", "Optional upload date filter (hour, today, week, month, year)"),
                                        "sortBy", Map.of("type", "string", "description", "Optional sort order (relevance, upload_date, view_count, rating)"),
                                        "maxResults", Map.of("type", "integer", "description", "Maximum number of results (default: 20, max: 50)", "default", 20)
                                    ),
                                    "required", List.of("query")
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
        
        // Validate client token
        Map<String, Object> validation = validateClientToken();
        if (!(Boolean) validation.get("valid")) {
            logger.error("Tool call rejected due to invalid client credentials");
            return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "error", Map.of(
                    "code", -32001,
                    "message", "Unauthorized: " + validation.get("error")
                )
            );
        }
        
        ClientTokenService.ClientInfo clientInfo = (ClientTokenService.ClientInfo) validation.get("clientInfo");
        
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        // Check if client has permission for this tool
        String requiredPermission = getRequiredPermission(name);
        if (requiredPermission != null && !clientInfo.permissions.contains(requiredPermission)) {
            logger.warn("Client {} does not have permission for tool: {}", clientInfo.clientId, name);
            return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "error", Map.of(
                    "code", -32002,
                    "message", "Forbidden: Client does not have permission to use tool: " + name
                )
            );
        }
        
        logger.info("Executing tool call: {} with arguments: {} for client: {}", name, arguments, clientInfo.name);

        return switch (name) {
            case "search_flights" -> {
                try {
                    String departure = (String) arguments.get("departure");
                    String arrival = (String) arguments.get("arrival");
                    String date = (String) arguments.get("date");
                    
                    logger.debug("Calling FlightService.searchFlights with: {}, {}, {} for client: {}", departure, arrival, date, clientInfo.clientId);
                    String flightData = flightService.searchFlights(departure, arrival, date, clientInfo.clientId);
                    
                    logger.info("Successfully processed flight search request, returning raw flight data");
                    yield Map.of(
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
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "error", Map.of(
                            "code", -32000,
                            "message", "Failed to search flights: " + e.getMessage()
                        )
                    );
                }
            }
            case "search_hotels" -> {
                try {
                    String location = (String) arguments.get("location");
                    String checkIn = (String) arguments.get("checkIn");
                    String checkOut = (String) arguments.get("checkOut");
                    Integer adults = arguments.get("adults") != null ? (Integer) arguments.get("adults") : 2;
                    
                    logger.debug("Calling TripAdvisorService.searchHotels with: {}, {}, {}, {} for client: {}", location, checkIn, checkOut, adults, clientInfo.clientId);
                    String hotelData = tripAdvisorService.searchHotels(location, checkIn, checkOut, adults, clientInfo.clientId);
                    
                    logger.info("Successfully processed hotel search request, returning hotel data");
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "result", Map.of(
                            "content", List.of(Map.of(
                                "type", "text",
                                "text", hotelData
                            ))
                        )
                    );
                } catch (Exception e) {
                    logger.error("Failed to execute search_hotels tool: {}", e.getMessage(), e);
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "error", Map.of(
                            "code", -32000,
                            "message", "Failed to search hotels: " + e.getMessage()
                        )
                    );
                }
            }
            case "search_restaurants" -> {
                try {
                    String location = (String) arguments.get("location");
                    String cuisine = (String) arguments.get("cuisine");
                    
                    logger.debug("Calling TripAdvisorService.searchRestaurants with: {}, {} for client: {}", location, cuisine, clientInfo.clientId);
                    String restaurantData = tripAdvisorService.searchRestaurants(location, cuisine, clientInfo.clientId);
                    
                    logger.info("Successfully processed restaurant search request, returning restaurant data");
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "result", Map.of(
                            "content", List.of(Map.of(
                                "type", "text",
                                "text", restaurantData
                            ))
                        )
                    );
                } catch (Exception e) {
                    logger.error("Failed to execute search_restaurants tool: {}", e.getMessage(), e);
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "error", Map.of(
                            "code", -32000,
                            "message", "Failed to search restaurants: " + e.getMessage()
                        )
                    );
                }
            }
            case "search_attractions" -> {
                try {
                    String location = (String) arguments.get("location");
                    String category = (String) arguments.get("category");
                    
                    logger.debug("Calling TripAdvisorService.searchAttractions with: {}, {} for client: {}", location, category, clientInfo.clientId);
                    String attractionData = tripAdvisorService.searchAttractions(location, category, clientInfo.clientId);
                    
                    logger.info("Successfully processed attraction search request, returning attraction data");
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "result", Map.of(
                            "content", List.of(Map.of(
                                "type", "text",
                                "text", attractionData
                            ))
                        )
                    );
                } catch (Exception e) {
                    logger.error("Failed to execute search_attractions tool: {}", e.getMessage(), e);
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "error", Map.of(
                            "code", -32000,
                            "message", "Failed to search attractions: " + e.getMessage()
                        )
                    );
                }
            }
            case "search_youtube_videos" -> {
                try {
                    String query = (String) arguments.get("query");
                    String duration = (String) arguments.get("duration");
                    String uploadDate = (String) arguments.get("uploadDate");
                    String sortBy = (String) arguments.get("sortBy");
                    Integer maxResults = arguments.get("maxResults") != null ? (Integer) arguments.get("maxResults") : 20;
                    
                    logger.debug("Calling YouTubeService.searchVideos with: {}, {}, {}, {}, {} for client: {}", query, duration, uploadDate, sortBy, maxResults, clientInfo.clientId);
                    String videoData = youtubeService.searchVideos(query, duration, uploadDate, sortBy, maxResults, clientInfo.clientId);
                    
                    logger.info("Successfully processed YouTube search request, returning video data");
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "result", Map.of(
                            "content", List.of(Map.of(
                                "type", "text",
                                "text", videoData
                            ))
                        )
                    );
                } catch (Exception e) {
                    logger.error("Failed to execute search_youtube_videos tool: {}", e.getMessage(), e);
                    yield Map.of(
                        "jsonrpc", "2.0",
                        "id", id,
                        "error", Map.of(
                            "code", -32000,
                            "message", "Failed to search YouTube videos: " + e.getMessage()
                        )
                    );
                }
            }
            default -> {
                logger.warn("Unknown tool requested: {}", name);
                yield Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "error", Map.of(
                        "code", -32601,
                        "message", "Unknown tool: " + name
                    )
                );
            }
        };
    }
}