package org.example.strategy;

import org.example.models.MarketDataPoint;

import java.math.BigDecimal;
import java.util.List;

public interface Strategy {
    /**
     * Determines whether to buy, sell, or hold a stock
     *
     * @param marketDataPoints List of historical market data points
     * @return TradeSignal indicating recommended action
     */
    TradeSignal determineTradeSignal(List<MarketDataPoint> marketDataPoints);

    /**
     * Calculates the position size based on strategy rules
     *
     * @param totalCapital Total available capital
     * @param currentPrice Current stock price
     * @return Recommended number of shares to trade
     */
    int calculatePositionSize(BigDecimal totalCapital, BigDecimal currentPrice);

    enum TradeSignal {
        BUY,    // Strong buy signal
        SELL,   // Strong sell signal
        HOLD    // No action recommended
    }
}