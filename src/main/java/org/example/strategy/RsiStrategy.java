package org.example.strategy;

import org.example.models.MarketDataPoint;

import java.math.BigDecimal;
import java.util.List;

public class RsiStrategy implements Strategy {
    // Standard RSI period and thresholds
    private static final int RSI_PERIOD = 14;
    private static final double OVERBOUGHT_THRESHOLD = 70;
    private static final double OVERSOLD_THRESHOLD = 30;

    @Override
    public TradeSignal determineTradeSignal(List<MarketDataPoint> marketDataPoints) {
        // Ensure we have enough data points
        if (marketDataPoints == null || marketDataPoints.size() < RSI_PERIOD) {
            return TradeSignal.HOLD;
        }

        // Calculate RSI
        double rsi = calculateRSI(marketDataPoints);

        // Simple binary signal generation
        if (rsi > OVERBOUGHT_THRESHOLD) {
            return TradeSignal.SELL;
        } else if (rsi < OVERSOLD_THRESHOLD) {
            return TradeSignal.BUY;
        }

        return TradeSignal.HOLD;
    }

    @Override
    public int calculatePositionSize(BigDecimal totalCapital, BigDecimal currentPrice) {
        // Fixed percentage risk approach
        BigDecimal riskPercentage = BigDecimal.valueOf(0.01); // 1% risk per trade
        BigDecimal riskAmount = totalCapital.multiply(riskPercentage);

        // Simple stop loss at 3% from current price
        BigDecimal stopLossDistance = currentPrice.multiply(BigDecimal.valueOf(0.03));

        // Calculate position size
        return riskAmount.divide(stopLossDistance, BigDecimal.ROUND_DOWN).intValue();
    }

    private double calculateRSI(List<MarketDataPoint> marketDataPoints) {
        // Take last 14 data points
        int endIndex = marketDataPoints.size() - 1;
        List<MarketDataPoint> relevantData = marketDataPoints.subList(
                Math.max(0, endIndex - RSI_PERIOD + 1),
                endIndex + 1
        );

        double totalGain = 0;
        double totalLoss = 0;

        // Calculate gains and losses
        for (int i = 1; i < relevantData.size(); i++) {
            BigDecimal priceDiff = relevantData.get(i).getClose()
                    .subtract(relevantData.get(i - 1).getClose());

            if (priceDiff.compareTo(BigDecimal.ZERO) >= 0) {
                totalGain += priceDiff.doubleValue();
            } else {
                totalLoss += Math.abs(priceDiff.doubleValue());
            }
        }

        // Avoid division by zero
        double avgGain = totalGain / RSI_PERIOD;
        double avgLoss = totalLoss / RSI_PERIOD;

        // Prevent division by zero
        if (avgLoss == 0) return 100;

        // Calculate Relative Strength
        double rs = avgGain / avgLoss;

        // Calculate RSI
        return 100 - (100 / (1 + rs));
    }
}