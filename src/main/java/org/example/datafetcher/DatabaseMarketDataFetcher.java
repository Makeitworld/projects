package org.example.datafetcher;

import org.example.models.MarketDataPoint;
import org.example.repository.MarketDataRepository;

import java.time.LocalDateTime;
import java.util.List;

public class DatabaseMarketDataFetcher {
    private final MarketDataRepository repository;

    public DatabaseMarketDataFetcher(MarketDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Fetches market data from the database.
     *
     * @param symbol         The symbol for the market data (e.g., "AAPL").

     * @return A list of MarketDataPoint objects.
     */
    public List<MarketDataPoint> fetchMarketData(String symbol) {
        // Currently fetches all by ticker. Enhance filtering by timestamp range if needed.
        return repository.getByTicker(symbol);
    }

    /**
     * Saves or updates market data into the database.
     *
     * @param data A list of MarketDataPoint objects to save.
     */
    public void saveOrUpdateMarketData(List<MarketDataPoint> data) {
        for (MarketDataPoint point : data) {
            repository.saveOrUpdate(point);
        }
    }
}
