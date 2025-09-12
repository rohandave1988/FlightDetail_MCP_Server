# Client Token Management

Secure API key management that keeps sensitive credentials server-side instead of in Claude Desktop config files.

## Setup

### 1. Configure Server Tokens
Edit `src/main/resources/client-tokens.json`:
```json
{
  "clients": [
    {
      "clientId": "claude-desktop",
      "clientToken": "claude-desktop-token-12345",
      "name": "Claude Desktop Client", 
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

### 2. Configure Claude Desktop
Update `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "travel-search": {
      "command": "java",
      "args": ["-jar", "/path/to/flight-mcp-server-1.0.0.jar", "--stdio"],
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

## Security Benefits

✅ **API keys never exposed in Claude Desktop config**  
✅ **Centralized credential management**  
✅ **Per-client permissions and API keys**  
✅ **Easy credential rotation without client changes**  

## Management

**Add Client**: Edit `client-tokens.json`, restart server  
**Revoke Access**: Set `active: false` or remove entry  
**Permissions**: `flights`, `tripadvisor`, `youtube`  

## Error Codes
- `-32001`: Unauthorized (invalid credentials)  
- `-32002`: Forbidden (insufficient permissions)