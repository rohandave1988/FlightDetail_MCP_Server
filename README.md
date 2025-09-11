

https://github.com/user-attachments/assets/f8ef7c7c-642a-4e46-b075-e1a17740cee2

# Flight & Travel MCP Server

A Spring Boot MCP (Model Context Protocol) server for comprehensive travel and content search capabilities. Integrates with SERP API for real flight data, TripAdvisor travel information, and YouTube video search, providing global search through Claude Desktop integration.

## Features

- **MCP Protocol Compliant** - Full stdio-based MCP implementation for Claude Desktop
- **Real Flight Data** - SERP API integration with Google Flights data  
- **TripAdvisor Integration** - Hotels, restaurants, and attractions search via SERP API
- **YouTube Video Search** - YouTube video search with advanced filtering options
- **Global Search Support** - Supports worldwide flight, travel, and video searches
- **Fallback Support** - Mock data when SERP API unavailable
- **Claude Desktop Ready** - Optimized for seamless Claude Desktop integration
- **Multi-Communication** - Supports both stdio (MCP) and HTTP endpoints

## Prerequisites

- Java 21+
- SERP API key (optional, falls back to mock data)
- Claude Desktop (for MCP integration)

## Setup

### 1. Build the Application
```bash
./gradlew build -x test
```

### 2. Configure SERP API (Optional)
Set your SERP API keys via environment variables:

**For Flight Search:**
```bash
export FLIGHT_SERPAPI_KEY=your_serpapi_key_here
```

**For TripAdvisor Search:**
```bash
export TRIPADVISOR_SERPAPI_KEY=your_serpapi_key_here
```

**For YouTube Search:**
```bash
export YOUTUBE_SERPAPI_KEY=your_serpapi_key_here
```

The application automatically picks up the API keys from these environment variables. If not set, the server will use mock data for each respective service.

### 3. Claude Desktop Integration
Add to your Claude Desktop configuration (`~/Library/Application Support/Claude/claude_desktop_config.json`):
```json
{
  "mcpServers": {
    "flight-search": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/flight-mcp-server-1.0.0.jar",
        "--stdio"
      ],
      "env": {
        "MCP_STDIO": "true",
        "SPRING_MAIN_WEB_APPLICATION_TYPE": "none",
        "FLIGHT_SERPAPI_KEY": "your_flight_serpapi_key_here",
        "TRIPADVISOR_SERPAPI_KEY": "your_tripadvisor_serpapi_key_here",
        "YOUTUBE_SERPAPI_KEY": "your_youtube_serpapi_key_here"
      }
    }
  }
}
```

### 4. Standalone Server (Optional)
```bash
java -jar build/libs/flight-mcp-server-1.0.0.jar
```

## Usage

### Claude Desktop
Once configured, you can ask Claude Desktop for comprehensive travel searches:

**Flight Searches:**
- "Search for flights from New York to London on December 25th"  
- "Find flights from Mumbai to Delhi tomorrow"
- "Show me flight options from Tokyo to Singapore on March 15th"

**Hotel Searches:**
- "Find hotels in Paris for December 25-27 for 2 adults"
- "Search for accommodation in Tokyo from January 1-5 for 1 adult"
- "Look for hotels in New York City for next weekend"

**Restaurant Searches:**
- "Find French restaurants in Paris"
- "Search for Italian restaurants in Rome"
- "Look for vegetarian restaurants in San Francisco"

**Attraction Searches:**
- "Find museums in London"
- "Search for parks and outdoor activities in Denver"
- "Look for tours and attractions in Barcelona"

**YouTube Video Searches:**
- "Find Python tutorial videos from this week"
- "Search for cooking videos uploaded today sorted by view count"
- "Look for short videos about machine learning"
- "Find recent travel vlogs about Japan"

**Combined Travel Planning:**
- "Plan a trip to Tokyo - find flights from New York on March 1st, hotels for March 1-5, and top attractions"

### HTTP API (Standalone Mode)
When running as standalone server:

**List Tools:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc": "2.0", "id": 1, "method": "tools/list"}'
```

**Search Flights:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "search_flights",
      "arguments": {
        "departure": "New York",
        "arrival": "London",
        "date": "2025-12-25"
      }
    }
  }'
```

**Health Check:**
```bash
curl http://localhost:8080/api/flights/health
```

### TripAdvisor API Endpoints

**Search Hotels:**
```bash
curl "http://localhost:8080/api/tripadvisor/hotels/search?location=New York&checkIn=2025-12-25&checkOut=2025-12-27&adults=2"
```

**Search Restaurants:**
```bash
curl "http://localhost:8080/api/tripadvisor/restaurants/search?location=Paris&cuisine=French"
```

**Search Attractions:**
```bash
curl "http://localhost:8080/api/tripadvisor/attractions/search?location=London&category=museums"
```

**Combined Search (All categories):**
```bash
curl "http://localhost:8080/api/tripadvisor/search?location=Tokyo&checkIn=2025-12-25&checkOut=2025-12-27&adults=2&cuisine=Japanese&category=temples"
```

**TripAdvisor Health Check:**
```bash
curl http://localhost:8080/api/tripadvisor/health
```

### YouTube API Endpoints

**Search Videos:**
```bash
curl "http://localhost:8080/api/youtube/search?query=Python tutorials&duration=medium&maxResults=10"
```

**Search Recent Videos:**
```bash
curl "http://localhost:8080/api/youtube/recent?query=travel vlogs&maxResults=5"
```

**Search Popular Videos:**
```bash
curl "http://localhost:8080/api/youtube/popular?query=cooking&duration=short&maxResults=15"
```

**Simple Video Search:**
```bash
curl "http://localhost:8080/api/youtube/videos/search?query=machine learning"
```

**YouTube Health Check:**
```bash
curl http://localhost:8080/api/youtube/health
```

## Configuration

### Application Settings
The `src/main/resources/application.yaml` automatically reads from environment variables:
```yaml
# Flight SERP API Configuration - reads from FLIGHT_SERPAPI_KEY env var
flight:
  serpapi:
    key: ${FLIGHT_SERPAPI_KEY:}

# TripAdvisor SERP API Configuration - reads from TRIPADVISOR_SERPAPI_KEY env var
tripadvisor:
  serpapi:
    key: ${TRIPADVISOR_SERPAPI_KEY:}

# YouTube SERP API Configuration - reads from YOUTUBE_SERPAPI_KEY env var
youtube:
  serpapi:
    key: ${YOUTUBE_SERPAPI_KEY:}

# Server Configuration
server:
  port: 8080
```

**Environment Variable Setup:**
```bash
# Set the Flight API key
export FLIGHT_SERPAPI_KEY=your_serpapi_key_here

# Set the TripAdvisor API key
export TRIPADVISOR_SERPAPI_KEY=your_serpapi_key_here

# Set the YouTube API key
export YOUTUBE_SERPAPI_KEY=your_serpapi_key_here

# Verify they're set
echo $FLIGHT_SERPAPI_KEY
echo $TRIPADVISOR_SERPAPI_KEY
echo $YOUTUBE_SERPAPI_KEY
```

### SERP API Settings  
Edit `src/main/resources/api-config.properties`:
```properties
serp.api.base.url=https://serpapi.com/search.json
serp.api.engine=google_flights
serp.api.type=2
serp.api.timeout.seconds=30
serp.api.hl=en
serp.api.gl=us
```

## Project Structure

```
src/main/java/com/serpapi/flightmcp/
├── FlightMcpApplication.java         # Main Spring Boot application
├── config/
│   └── FlightMcpConfig.java          # Configuration management
├── controller/
│   └── FlightController.java         # REST API endpoints
├── server/
│   └── McpServer.java                # MCP protocol handler
└── service/
    └── FlightService.java            # SERP API integration
```

## Architecture Overview

```
┌─────────────────┐    stdio/MCP       ┌─────────────────┐
│ Claude Desktop  │ ◄─────────────────► │   Spring Boot   │
│                 │   Protocol          │   MCP Server    │
└─────────────────┘                     └─────────────────┘
                                                  │
                                        ┌─────────────────┐
                                        │   SERP API      │
                                        │ (Google Flights)│
                                        └─────────────────┘
```

### Communication Modes
- **stdio Mode**: For Claude Desktop integration via MCP protocol  
- **HTTP Mode**: For standalone operation and custom integrations
- **Dual Mode**: Server supports both simultaneously

## Available Tools

The MCP server provides comprehensive travel search tools:

### search_flights
**Description**: Search for flight information using departure and arrival locations  
**Parameters**:
- `departure` (string): Departure city or airport code
- `arrival` (string): Arrival city or airport code  
- `date` (string): Departure date in YYYY-MM-DD format

### search_hotels
**Description**: Search for hotels in a specific location using TripAdvisor data  
**Parameters**:
- `location` (string): Location to search for hotels
- `checkIn` (string): Check-in date (YYYY-MM-DD)
- `checkOut` (string): Check-out date (YYYY-MM-DD)
- `adults` (integer): Number of adults (default: 2)

### search_restaurants
**Description**: Search for restaurants in a specific location using TripAdvisor data  
**Parameters**:
- `location` (string): Location to search for restaurants
- `cuisine` (string, optional): Cuisine type filter

### search_attractions
**Description**: Search for attractions and activities in a specific location using TripAdvisor data  
**Parameters**:
- `location` (string): Location to search for attractions
- `category` (string, optional): Category filter (e.g., museums, parks, tours)

### search_youtube_videos
**Description**: Search for YouTube videos using SerpAPI YouTube search  
**Parameters**:
- `query` (string): Search query for YouTube videos
- `duration` (string, optional): Duration filter (short, medium, long)
- `uploadDate` (string, optional): Upload date filter (hour, today, week, month, year)
- `sortBy` (string, optional): Sort order (relevance, upload_date, view_count, rating)
- `maxResults` (integer, optional): Maximum number of results (default: 20, max: 50)

**Example Usage in Claude Desktop**:
- "Find flights from Mumbai to Delhi on 2025-12-15"
- "Search for hotels in Paris for December 25-27 for 2 adults" 
- "Find Italian restaurants in Rome"
- "Look for museums and cultural attractions in London"
- "Search for Python tutorial videos uploaded this week"
- "Find popular cooking videos under 10 minutes long"
- "Plan a complete trip to Tokyo with flights, hotels, and activities"

## Troubleshooting

### Common Issues

**Build Issues:**
```bash
# Clean build
./gradlew clean build -x test
```

**Claude Desktop Connection:**
- Ensure JAR path in `claude_desktop_config.json` is correct
- Restart Claude Desktop after configuration changes
- Check stderr output for debug information

**API Key Issues:**
- Verify SERP API key is set in `application.yaml` or environment variable
- Without API key, server falls back to mock data (still functional)
- Get API key from: https://serpapi.com/

**Port Conflicts:**
```bash
# Kill process on port 8080  
lsof -ti:8080 | xargs kill -9
```

### Debug Output
When running with Claude Desktop, check stderr for:
```
DEBUG: searchFlights called - serpApiKey.isEmpty(): false
DEBUG: About to send SERP API request...
DEBUG: SERP API response status: 200
```

## Integration with Other LLMs

This MCP server can integrate with various LLM platforms:

- **OpenAI API**: Use custom MCP client with function calling
- **Local LLMs**: Ollama, LM Studio via HTTP endpoints
- **Other Platforms**: Any system supporting stdio or HTTP communication

**Example HTTP Integration:**
```python
import requests

response = requests.post('http://localhost:8080/mcp', json={
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call", 
    "params": {
        "name": "search_flights",
        "arguments": {
            "departure": "NYC",
            "arrival": "LAX", 
            "date": "2025-12-25"
        }
    }
})
```

## Files Structure

```
flight-mcp-server/
├── build.gradle                           # Build configuration
├── src/main/
│   ├── java/com/serpapi/flightmcp/
│   │   ├── FlightMcpApplication.java       # Main application
│   │   ├── config/FlightMcpConfig.java     # Configuration
│   │   ├── controller/FlightController.java # HTTP endpoints
│   │   ├── server/McpServer.java           # MCP protocol handler
│   │   └── service/FlightService.java      # SERP API integration
│   └── resources/
│       ├── application.yaml                # Main configuration
│       ├── application-stdio.properties    # Stdio mode config
│       ├── api-config.properties          # SERP API settings
│       └── logback-stdio.xml              # Logging for stdio mode
├── .gitignore                             # Git ignore (protects API keys)
└── README.md                              # This documentation
```

## Contributing

1. Fork the repository
2. Create a feature branch  
3. Test with both Claude Desktop and standalone modes
4. Submit a pull request

