# Flight MCP Server

A Spring Boot MCP (Model Context Protocol) server for flight search capabilities. Integrates with SERP API for real flight data and provides global flight search through Claude Desktop integration.

## Features

- **MCP Protocol Compliant** - Full stdio-based MCP implementation for Claude Desktop
- **Real Flight Data** - SERP API integration with Google Flights data  
- **Global Flight Search** - Supports flight searches worldwide
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
Set your SERP API key via environment variable:
```bash
export FLIGHT_SERPAPI_KEY=your_serpapi_key_here
```

The application automatically picks up the API key from the `FLIGHT_SERPAPI_KEY` environment variable. If not set, the server will use mock data.

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
        "FLIGHT_SERPAPI_KEY": "your_serpapi_key_here"
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
Once configured, simply ask Claude Desktop to search for flights:
- "Search for flights from New York to London on December 25th"  
- "Find flights from Mumbai to Delhi tomorrow"
- "Show me flight options from Tokyo to Singapore on March 15th"

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

## Configuration

### Application Settings
The `src/main/resources/application.yaml` automatically reads from environment variables:
```yaml
# SERP API Configuration - reads from FLIGHT_SERPAPI_KEY env var
flight:
  serpapi:
    key: ${FLIGHT_SERPAPI_KEY:}

# Server Configuration
server:
  port: 8080
```

**Environment Variable Setup:**
```bash
# Set the API key
export FLIGHT_SERPAPI_KEY=your_serpapi_key_here

# Verify it's set
echo $FLIGHT_SERPAPI_KEY
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

The MCP server provides one main tool:

### search_flights
**Description**: Search for flight information using departure and arrival locations  
**Parameters**:
- `departure` (string): Departure city or airport code
- `arrival` (string): Arrival city or airport code  
- `date` (string): Departure date in YYYY-MM-DD format

**Example Usage in Claude Desktop**:
- "Find flights from Mumbai to Delhi on 2025-12-15"
- "Search flights JFK to LAX tomorrow"  
- "Show me options from London to Paris on March 1st"

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

## License  

MIT License - see LICENSE file for details