package org.example.strategy;

import org.example.models.MarketDataPoint;

import java.math.BigDecimal;
import java.util.List;

public class MovingAverageStrategy implements Strategy {
    private static final int SHORT_PERIOD = 50;
    private static final int LONG_PERIOD = 200;
    private static final double RISK_PER_TRADE = 0.015; // 1.5% risk
    private static final double BUFFER_PERCENT = 0.005; // 0.5% buffer to filter minor crossovers
    private static final double MIN_VOLUME_THRESHOLD = 1_000_000; // Minimum volume for valid signal

    @Override
    public TradeSignal determineTradeSignal(List<MarketDataPoint> marketDataPoints) {
        if (marketDataPoints == null || marketDataPoints.size() < LONG_PERIOD) {
            return TradeSignal.HOLD;
        }

        // Calculate short and long-term exponential moving averages
        double shortTermEMA = calculateExponentialMovingAverage(marketDataPoints, SHORT_PERIOD);
        double longTermEMA = calculateExponentialMovingAverage(marketDataPoints, LONG_PERIOD);

        // Apply a buffer to avoid whipsaws
        double buffer = longTermEMA * BUFFER_PERCENT;

        // Validate volume levels
        double currentVolume = marketDataPoints.get(marketDataPoints.size() - 1).getVolume();
        if (currentVolume < MIN_VOLUME_THRESHOLD) {
            return TradeSignal.HOLD; // Ignore signal if volume is too low
        }

        // Golden Cross (Short EMA crosses above Long EMA with buffer)
        if (shortTermEMA > longTermEMA + buffer && isVolumeIncreasing(marketDataPoints)) {
            return TradeSignal.SELL;
        }
        // Death Cross (Short EMA crosses below Long EMA with buffer)
        else if (shortTermEMA < longTermEMA - buffer && isVolumeDecreasing(marketDataPoints)) {
            return TradeSignal.BUY;
        }

        return TradeSignal.HOLD;
    }

    @Override
    public int calculatePositionSize(BigDecimal totalCapital, BigDecimal currentPrice) {
        BigDecimal riskAmount = BigDecimal.valueOf(RISK_PER_TRADE).multiply(totalCapital);
        BigDecimal stopLossDistance = currentPrice.multiply(BigDecimal.valueOf(0.04)); // 4% stop loss
        return riskAmount.divide(stopLossDistance, BigDecimal.ROUND_DOWN).intValue();
    }

    /**
     * Calculates the Exponential Moving Average (EMA) for a given period.
     *
     * @param marketDataPoints List of MarketDataPoint objects
     * @param period           The period for EMA calculation
     * @return The calculated EMA
     */
    private double calculateExponentialMovingAverage(List<MarketDataPoint> marketDataPoints, int period) {
        int size = marketDataPoints.size();
        double smoothingFactor = 2.0 / (period + 1);
        double ema = marketDataPoints.get(0).getClose().doubleValue(); // Initialize EMA with the first data point

        for (int i = size-1; i < size; i++) {
            double closePrice = marketDataPoints.get(i).getClose().doubleValue();
            ema = (closePrice - ema) * smoothingFactor + ema; // EMA formula
        }

        return ema;
    }

    /**
     * Checks if the volume is increasing over the last few data points.
     *
     * @param marketDataPoints List of MarketDataPoint objects
     * @return true if volume is increasing, false otherwise
     */
    private boolean isVolumeIncreasing(List<MarketDataPoint> marketDataPoints) {
        int size = marketDataPoints.size();
        double prevVolume = marketDataPoints.get(size - 2).getVolume();
        double currentVolume = marketDataPoints.get(size - 1).getVolume();

        return currentVolume > prevVolume; // Volume is increasing
    }

    /**
     * Checks if the volume is decreasing over the last few data points.
     *
     * @param marketDataPoints List of MarketDataPoint objects
     * @return true if volume is decreasing, false otherwise
     */
    private boolean isVolumeDecreasing(List<MarketDataPoint> marketDataPoints) {
        int size = marketDataPoints.size();
        double prevVolume = marketDataPoints.get(size - 2).getVolume();
        double currentVolume = marketDataPoints.get(size - 1).getVolume();

        return currentVolume < prevVolume; // Volume is decreasing
    }
}
