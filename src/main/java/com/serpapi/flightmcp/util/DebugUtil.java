package com.serpapi.flightmcp.util;

/**
 * Utility class for conditional debug output.
 * Only outputs debug messages when not in MCP stdio mode to avoid interfering with the protocol.
 */
public class DebugUtil {
    
    private static final boolean isStdioMode = isStdioModeEnabled();
    
    private static boolean isStdioModeEnabled() {
        return "true".equals(System.getenv("MCP_STDIO")) || 
               "true".equals(System.getProperty("mcp.stdio"));
    }
    
    /**
     * Print debug message only if not in stdio mode
     */
    public static void debug(String message) {
        if (!isStdioMode) {
            System.err.println("DEBUG: " + message);
        }
    }
    
    /**
     * Check if stdio mode is enabled
     */
    public static boolean isStdioMode() {
        return isStdioMode;
    }
}