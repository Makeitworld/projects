package org.example.datafetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApiConfig {
    private static ApiConfig instance;
    private final Properties properties;

    // Private constructor to prevent instantiation
    private ApiConfig() {
        properties = new Properties();
        try {
            // Load properties from application.properties
            InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties");
            if (input == null) {
                throw new IOException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Singleton instance method
    public static synchronized ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
        }
        return instance;
    }

    /**
     * Get API key for a specific service
     *
     * @param service The name of the service (e.g., "ALPHA_VANTAGE")
     * @return The API key for the specified service
     */
    public String getApiKey(String service) {
        return properties.getProperty(service + "_API_KEY", "");
    }

    /**
     * Get a configuration property
     *
     * @param key The property key
     * @return The property value
     */
    public String getProperty(String key) {
        return properties.getProperty(key, "");
    }
}