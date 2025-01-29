package org.example.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record BacktestResult(
        BigDecimal initialCapital,
        BigDecimal finalCapital,
        double totalReturn,
        List<Trade> trades,
        List<BigDecimal> dailyReturns,
        double riskFreeRate,
        List<String> missedSellTrades,    // Added missed sell trades
        List<String> negativeProfitTrades,  // Added negative profit trades
        List<String> lossTrades, List<BigDecimal> dailyCapital) { // Track daily capital for drawdown calculation

    // Calculate Sharpe Ratio
    public double calculateSharpeRatio() {
        BigDecimal meanReturn = dailyReturns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), 4, RoundingMode.HALF_UP);

        BigDecimal variance = dailyReturns.stream()
                .map(r -> r.subtract(meanReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), 4, RoundingMode.HALF_UP);

        BigDecimal standardDeviation = new BigDecimal(Math.sqrt(variance.doubleValue()));

        // Sharpe Ratio formula: (Mean Return - Risk-Free Rate) / Standard Deviation
        return Math.sqrt(252) * meanReturn.subtract(BigDecimal.valueOf(riskFreeRate))
                .divide(standardDeviation, RoundingMode.HALF_UP).doubleValue();
    }

    // Calculate Sortino Ratio
    public double calculateSortinoRatio() {
        BigDecimal downsideDeviation = calculateDownsideDeviation();

        if (downsideDeviation.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        BigDecimal meanReturn = dailyReturns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyReturns.size()), 4, RoundingMode.HALF_UP);

        // Sortino Ratio formula: (Mean Return - Risk-Free Rate) / Downside Deviation
        return meanReturn.subtract(BigDecimal.valueOf(riskFreeRate)).divide(downsideDeviation, RoundingMode.HALF_UP).doubleValue();
    }

    // Calculate downside deviation (standard deviation of negative returns)
    public BigDecimal calculateDownsideDeviation() {
        BigDecimal downsideReturns = BigDecimal.ZERO;
        int downsideCount = 0;

        for (BigDecimal dailyReturn : dailyReturns) {
            if (dailyReturn.compareTo(BigDecimal.ZERO) < 0) {
                downsideReturns = downsideReturns.add(dailyReturn.pow(2));
                downsideCount++;
            }
        }

        if (downsideCount == 0) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(Math.sqrt(downsideReturns.doubleValue() / downsideCount));
    }

    // New method to calculate the number of missed sell trades
    public int getMissedSellTradesCount() {
        return missedSellTrades.size();
    }

    // New method to calculate the number of trades with negative profit
    public int getNegativeProfitTradesCount() {
        return negativeProfitTrades.size();
    }

    // New method to calculate the Maximum Drawdown
    public double calculateMaxDrawdown() {
        BigDecimal maxCapital = initialCapital;
        BigDecimal peakCapital = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (BigDecimal capital : dailyCapital) {
            // Track the peak capital
            if (capital.compareTo(peakCapital) > 0) {
                peakCapital = capital;
            }

            // Calculate drawdown from the peak
            BigDecimal drawdown = peakCapital.subtract(capital).divide(peakCapital, 4, RoundingMode.HALF_UP);

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        // Return max drawdown as a percentage
        return maxDrawdown.doubleValue() * 100;
    }

    @Override
    public String toString() {
        return "BacktestResult{" +
                "initialCapital=" + initialCapital +
                ", finalCapital=" + finalCapital +
                ", totalReturn=" + totalReturn +
                "%, trades=" + trades.size() +
                ", missedSellTrades=" + missedSellTrades.size() +
                ", negativeProfitTrades=" + negativeProfitTrades.size() +
                ", MaxDrawdown=" + calculateMaxDrawdown() + "%" +
                ", SharpeRatio=" + calculateSharpeRatio() +
                ", SortinoRatio=" + calculateSortinoRatio() +
                '}';
    }
}
