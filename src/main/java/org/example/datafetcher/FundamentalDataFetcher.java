package org.example.datafetcher;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.example.models.StockFundamentals;
import org.example.repository.StockFundamentalsRepository;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class FundamentalDataFetcher {
    private static final String API_URL = "https://www.alphavantage.co/query";
    private final StockFundamentalsRepository repository;

    public FundamentalDataFetcher(StockFundamentalsRepository repository) {
        this.repository = repository;
    }

    /**
     * Fetches fundamental data for a given stock symbol from the API and saves it to the database.
     *
     * @param symbol  The stock symbol (e.g., "AAPL").
     * @param apiKey  The Alpha Vantage API key.
     * @throws IOException If an error occurs during the HTTP request.
     */
    public void fetchAndSaveFundamentalData(String symbol, String apiKey) throws IOException {
        String overviewUrl = String.format("%s?function=OVERVIEW&symbol=%s&apikey=%s",
                API_URL, symbol, apiKey);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(overviewUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                        StringBuilder responseBody = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBody.append(line);
                        }
                        System.out.println(responseBody.toString());

                        // Parse JSON and create StockFundamentals object
                        StockFundamentals fundamentals = parseFundamentalData(symbol, responseBody.toString());


                        // Save to database
                        repository.saveOrUpdate(fundamentals);
                        System.out.println("Data fetched from API and saved to database for symbol: " + symbol);
                    }
                } else {
                    throw new IOException("No response received from the API");
                }
            }
        }
    }

    /**
     * Fetches fundamental data for a given stock symbol from the database.
     *
     * @param symbol The stock symbol (e.g., "AAPL").
     * @return The StockFundamentals object or null if not found.
     */
    public StockFundamentals getFundamentalDataFromDatabase(String symbol) {
        StockFundamentals fundamentals = repository.findByTicker(symbol);
        if (fundamentals != null) {
            System.out.println("Data retrieved from database for symbol: " + symbol);
        } else {
            System.out.println("No data found in the database for symbol: " + symbol);
        }
        return fundamentals;
    }

    public List<StockFundamentals> getAllFundamentalDataFromDatabase() {
        List<StockFundamentals> allFundamentals = repository.findAll();

        if (allFundamentals != null && !allFundamentals.isEmpty()) {
            System.out.println("Entire fundamental data retrieved from the database for all stocks.");
        } else {
            System.out.println("No fundamental data found in the database.");
        }

        return allFundamentals;
    }

    /**
     * Parses the fundamental data JSON into a StockFundamentals object.
     *
     * @param symbol       The stock symbol.
     * @param jsonResponse The JSON response from the API.
     * @return StockFundamentals object.
     */
    private StockFundamentals parseFundamentalData(String symbol, String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            return new StockFundamentals(
                    symbol,
                    parseDoubleOrDefault(jsonObject, "PERatio"),
                    parseDoubleOrDefault(jsonObject, "PriceToBookRatio"),
                    parseDoubleOrDefault(jsonObject, "ReturnOnEquityTTM"),
                    parseDoubleOrDefault(jsonObject, "DividendYield")
            );
        } catch (Exception e) {
            // Return a default object if parsing fails
            return new StockFundamentals(symbol);
        }
    }

    /**
     * Helper method to parse double values safely from JSON.
     *
     * @param jsonObject The JSON object.
     * @return Parsed double value or 0.0 if parsing fails.
     */


    private double parseDoubleOrDefault(JSONObject jsonObject, String key) {
        try {
            String value = jsonObject.optString(key, "0.0");
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
