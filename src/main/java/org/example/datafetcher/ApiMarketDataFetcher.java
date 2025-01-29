package org.example.datafetcher;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.example.models.MarketDataPoint;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ApiMarketDataFetcher {
    private static final Logger LOGGER = Logger.getLogger(ApiMarketDataFetcher.class.getName());
    private static final String API_URL = "https://www.alphavantage.co/query";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Fetches market data for the given symbol and interval from Alpha Vantage API.
     *
     * @param symbol    The stock symbol (e.g., "AAPL" for Apple Inc.)
     * @param interval  The interval (e.g., "5min", "15min", "daily")
     * @param apiKey    The Alpha Vantage API key
     * @return List of MarketDataPoint objects
     * @throws IOException If an error occurs during the HTTP request
     */
    public static List<MarketDataPoint> fetchMarketData(
            String symbol,
            String interval,
            String apiKey,
            LocalDateTime startTimestamp,
            LocalDateTime endTimestamp
    ) throws IOException {
        String function = getApiFunction(interval);
        String url = String.format("%s?function=%s&symbol=%s&interval=%s&outputsize=full&apikey=%s",
                API_URL, function, symbol, interval, apiKey);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                        StringBuilder responseBody = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBody.append(line);
                        }
                        // Pass timestamps to parseMarketData
                        System.out.println(responseBody.toString());
                        return parseMarketData(symbol, responseBody.toString(), interval, startTimestamp, endTimestamp);
                    }
                } else {
                    throw new IOException("No response received from the Alpha Vantage API");
                }
            }
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Error parsing market data", e);
            throw new IOException("Error parsing market data", e);
        }
    }

    /**
     * Determines the appropriate API function based on interval
     *
     * @param interval The time interval
     * @return Corresponding Alpha Vantage API function
     */
    private static String getApiFunction(String interval) {
        return switch (interval.toLowerCase()) {
            case "daily" -> "TIME_SERIES_DAILY";
            case "weekly" -> "TIME_SERIES_WEEKLY";
            case "monthly" -> "TIME_SERIES_MONTHLY";
            default -> "TIME_SERIES_INTRADAY";
        };
    }

    /**
     * Parses market data JSON response into a list of MarketDataPoint objects
     *
     * @param symbol    The stock symbol
     * @param jsonResponse The JSON response from the API
     * @param interval  The time interval
     * @return List of MarketDataPoint objects
     */
    private static List<MarketDataPoint> parseMarketData(
            String symbol,
            String jsonResponse,
            String interval,
            LocalDateTime startTimestamp,
            LocalDateTime endTimestamp
    ) {
        List<MarketDataPoint> dataPoints = new ArrayList<>();
        JSONObject timeSeries = getJsonObject(jsonResponse, interval);

        // Parse each timestamp's data
        for (String timestamp : timeSeries.keySet()) {
            JSONObject dataPoint = timeSeries.getJSONObject(timestamp);

            try {
                // First try to parse with the full format "yyyy-MM-dd HH:mm:ss"
                LocalDateTime parsedTimestamp = parseTimestamp(timestamp);

                // Filter by start and end timestamps
                if ((startTimestamp == null || !parsedTimestamp.isBefore(startTimestamp)) &&
                        (endTimestamp == null || !parsedTimestamp.isAfter(endTimestamp))) {

                    BigDecimal open = BigDecimal.valueOf(dataPoint.optDouble("1. open", 0.0));
                    BigDecimal high = BigDecimal.valueOf(dataPoint.optDouble("2. high", 0.0));
                    BigDecimal low = BigDecimal.valueOf(dataPoint.optDouble("3. low", 0.0));
                    BigDecimal close = BigDecimal.valueOf(dataPoint.optDouble("4. close", 0.0));
                    long volume = dataPoint.optLong("5. volume", 0);

                    dataPoints.add(new MarketDataPoint(symbol, parsedTimestamp, open, high, low, close, volume));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error parsing data for timestamp: " + timestamp, e);
            }
        }

        return dataPoints;
    }

    /**
     * Tries to parse a timestamp with two different formats:
     * 1. yyyy-MM-dd HH:mm:ss
     * 2. yyyy-MM-dd
     *
     * @param timestamp The timestamp to parse
     * @return The parsed LocalDateTime object
     */
    private static LocalDateTime parseTimestamp(String timestamp) {
        try {
            // Try parsing with full format (with time component)
            return LocalDateTime.parse(timestamp + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            // If parsing fails, handle the error and return null
            LOGGER.log(Level.WARNING, "Error parsing timestamp: " + timestamp, e);
            return null;  // Return null if parsing fails
        }
    }




    private static JSONObject getJsonObject(String jsonResponse, String interval) {
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // Determine the correct time series key based on the interval
        String timeSeriesKey = switch (interval.toLowerCase()) {
            case "daily" -> "Time Series (Daily)";
            case "weekly" -> "Weekly Time Series";
            case "monthly" -> "Monthly Time Series";
            default -> "Time Series (" + interval + ")";
        };

        JSONObject timeSeries = jsonObject.optJSONObject(timeSeriesKey);
        if (timeSeries == null) {
            throw new JSONException("Time series key not found in the API response: " + timeSeriesKey);
        }
        return timeSeries;
    }

    /**
     * Rate limiter to prevent exceeding API call limits
     *
     * @param milliseconds Delay between API calls
     */
    public static void apiCallDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("API call delay interrupted");
        }
    }
}
