# Flight & Travel MCP Server

Spring Boot MCP server for travel search - flights, hotels, restaurants, attractions, and YouTube videos via SERP API.

## Features

✅ **Secure Client Token Management** - API keys stored server-side, never exposed in config files  
✅ **Flight Search** - Google Flights via SERP API  
✅ **Travel Search** - TripAdvisor hotels, restaurants, attractions  
✅ **YouTube Search** - Video search with filters  
✅ **Claude Desktop Ready** - MCP protocol integration  

## Quick Setup

### 1. Build
```bash
./gradlew build -x test
```

### 2. Configure API Keys
Edit `src/main/resources/client-tokens.json`:
```json
{
  "clients": [
    {
      "clientId": "claude-desktop",
      "clientToken": "claude-desktop-token-12345",
      "permissions": ["flights", "tripadvisor", "youtube"],
      "active": true,
      "apiKeys": {
        "FLIGHT_SERPAPI_KEY": "your_flight_serpapi_key_here",
        "TRIPADVISOR_SERPAPI_KEY": "your_tripadvisor_serpapi_key_here",
        "YOUTUBE_SERPAPI_KEY": "your_youtube_serpapi_key_here"
      }
    }
  ]
}
```

### 3. Claude Desktop Config
Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "travel-search": {
      "command": "java",
      "args": ["-jar", "/path/to/FlightDetail_MCP_Server-1.0.0.jar", "--stdio"],
      "env": {
        "MCP_STDIO": "true",
        "SPRING_MAIN_WEB_APPLICATION_TYPE": "none",
        "MCP_CLIENT_ID": "claude-desktop",
        "MCP_CLIENT_TOKEN": "claude-desktop-token-12345"
      }
    }
  }
}
```

## Usage

Ask Claude Desktop:
- "Find flights from New York to London on December 25th"
- "Search hotels in Paris for 2 adults, December 25-27"
- "Find Italian restaurants in Rome"  
- "Look for museums in London"
- "Search Python tutorial videos from this week"

## Security Benefits

✅ **API keys never exposed in Claude Desktop config**  
✅ **Centralized credential management**  
✅ **Per-client permissions and API keys**  
✅ **Easy credential rotation without client changes**

## Available Tools

- **search_flights** - Flight search with departure, arrival, date
- **search_hotels** - Hotel search with location, dates, adults
- **search_restaurants** - Restaurant search with location, cuisine
- **search_attractions** - Attraction search with location, category  
- **search_youtube_videos** - Video search with query, filters, limits

## Management

**Add Client**: Edit `client-tokens.json`, restart server  
**Revoke Access**: Set `active: false` or remove client entry  
**Update API Keys**: Edit `apiKeys` section in client config

