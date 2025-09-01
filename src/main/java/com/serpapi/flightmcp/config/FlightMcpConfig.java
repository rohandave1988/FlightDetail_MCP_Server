package com.serpapi.flightmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Configuration class for Flight MCP application settings.
 * Loads properties from external configuration files.
 */
@Configuration
@PropertySource({"classpath:api-config.properties", "classpath:mock-flight-data.properties"})
public class FlightMcpConfig {

    private final Environment environment;

    public FlightMcpConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @ConfigurationProperties(prefix = "serp.api")
    public SerpApiConfig serpApiConfig() {
        SerpApiConfig config = new SerpApiConfig();
        config.setBaseUrl(environment.getProperty("serp.api.base.url", "https://serpapi.com/search.json"));
        config.setEngine(environment.getProperty("serp.api.engine", "google_flights"));
        config.setType(environment.getProperty("serp.api.type", "2"));
        config.setTimeoutSeconds(Integer.parseInt(environment.getProperty("serp.api.timeout.seconds", "30")));
        config.setHl(environment.getProperty("serp.api.hl", "en"));
        config.setGl(environment.getProperty("serp.api.gl", "in"));
        return config;
    }

    @Bean
    public MockDataConfig mockDataConfig() {
        MockDataConfig config = new MockDataConfig();
        config.setFlightDataTemplate(environment.getProperty("mock.flight.data", "{}"));
        return config;
    }


    /**
     * Configuration properties for SERP API settings.
     */
    public static class SerpApiConfig {
        private String baseUrl;
        private String engine;
        private String type;
        private int timeoutSeconds;
        private String hl; // Language (en = English)
        private String gl; // Country (in = India)

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public String getHl() { return hl; }
        public void setHl(String hl) { this.hl = hl; }

        public String getGl() { return gl; }
        public void setGl(String gl) { this.gl = gl; }
    }

    /**
     * Configuration properties for mock flight data.
     */
    public static class MockDataConfig {
        private String flightDataTemplate;

        public String getFlightDataTemplate() { return flightDataTemplate; }
        public void setFlightDataTemplate(String flightDataTemplate) { this.flightDataTemplate = flightDataTemplate; }
    }

}