package com.serpapi.flightmcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientTokenService {
    private static final Logger logger = LoggerFactory.getLogger(ClientTokenService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<ClientInfo> clients = new ArrayList<>();
    
    @PostConstruct
    public void loadClientTokens() {
        try {
            ClassPathResource resource = new ClassPathResource("client-tokens.json");
            if (!resource.exists()) {
                // Try loading from root directory
                java.io.File rootFile = new java.io.File("client-tokens.json");
                if (rootFile.exists()) {
                    JsonNode rootNode = objectMapper.readTree(rootFile);
                    parseClients(rootNode);
                } else {
                    logger.warn("client-tokens.json not found in classpath or root directory, no clients loaded");
                    return;
                }
            } else {
                try (InputStream inputStream = resource.getInputStream()) {
                    JsonNode rootNode = objectMapper.readTree(inputStream);
                    parseClients(rootNode);
                }
            }
            
            logger.info("Loaded {} client tokens", clients.size());
        } catch (IOException e) {
            logger.error("Failed to load client tokens: {}", e.getMessage());
        }
    }
    
    private void parseClients(JsonNode rootNode) {
        JsonNode clientsArray = rootNode.get("clients");
        if (clientsArray != null && clientsArray.isArray()) {
            for (JsonNode clientNode : clientsArray) {
                ClientInfo clientInfo = new ClientInfo();
                clientInfo.clientId = clientNode.get("clientId").asText();
                clientInfo.clientToken = clientNode.get("clientToken").asText();
                clientInfo.name = clientNode.get("name").asText();
                clientInfo.description = clientNode.has("description") ? clientNode.get("description").asText() : "";
                clientInfo.active = clientNode.has("active") ? clientNode.get("active").asBoolean() : true;
                
                // Parse permissions
                JsonNode permissionsNode = clientNode.get("permissions");
                if (permissionsNode != null && permissionsNode.isArray()) {
                    for (JsonNode permission : permissionsNode) {
                        clientInfo.permissions.add(permission.asText());
                    }
                }
                
                // Parse API keys
                JsonNode apiKeysNode = clientNode.get("apiKeys");
                if (apiKeysNode != null && apiKeysNode.isObject()) {
                    apiKeysNode.fields().forEachRemaining(entry -> {
                        clientInfo.apiKeys.put(entry.getKey(), entry.getValue().asText());
                    });
                }
                
                clients.add(clientInfo);
            }
        }
    }
    
    public boolean validateClientToken(String clientId, String clientToken) {
        if (clientId == null || clientToken == null) {
            logger.debug("Client validation failed: clientId or clientToken is null");
            return false;
        }
        
        ClientInfo client = findClient(clientId);
        if (client == null) {
            logger.debug("Client validation failed: clientId '{}' not found", clientId);
            return false;
        }
        
        if (!client.active) {
            logger.debug("Client validation failed: client '{}' is inactive", clientId);
            return false;
        }
        
        boolean isValid = clientToken.equals(client.clientToken);
        logger.debug("Client validation for '{}': {}", clientId, isValid ? "SUCCESS" : "FAILED");
        return isValid;
    }
    
    public ClientInfo getClientInfo(String clientId) {
        return findClient(clientId);
    }
    
    public boolean hasPermission(String clientId, String permission) {
        ClientInfo client = findClient(clientId);
        if (client == null || !client.active) {
            return false;
        }
        return client.permissions.contains(permission);
    }
    
    private ClientInfo findClient(String clientId) {
        return clients.stream()
                .filter(client -> clientId.equals(client.clientId))
                .findFirst()
                .orElse(null);
    }
    
    public List<ClientInfo> getAllClients() {
        return new ArrayList<>(clients);
    }
    
    /**
     * Gets the API key for a specific client and key name
     * 
     * @param clientId The client ID
     * @param keyName The API key name (e.g., "FLIGHT_SERPAPI_KEY")
     * @return The API key value, or null if not found
     */
    public String getApiKey(String clientId, String keyName) {
        ClientInfo client = findClient(clientId);
        if (client == null || !client.active) {
            return null;
        }
        return client.apiKeys.get(keyName);
    }
    
    public static class ClientInfo {
        public String clientId;
        public String clientToken;
        public String name;
        public String description;
        public boolean active;
        public List<String> permissions = new ArrayList<>();
        public Map<String, String> apiKeys = new HashMap<>();
        
        @Override
        public String toString() {
            return "ClientInfo{" +
                    "clientId='" + clientId + '\'' +
                    ", name='" + name + '\'' +
                    ", active=" + active +
                    ", permissions=" + permissions +
                    ", apiKeys=" + apiKeys.keySet() + // Don't log actual keys for security
                    '}';
        }
    }
}