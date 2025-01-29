package org.example.backtest;

import org.example.models.MarketDataPoint;
import org.example.models.Portfolio;
import org.example.strategy.Strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class Backtester {
    private final BigDecimal initialCapital;
    private final Strategy strategy;
    private final Map<String, List<MarketDataPoint>> marketData;
    public final List<BigDecimal> strategyReturns;
    public final List<BigDecimal> marketReturns;
    private final List<String> missedBuyTrades;
    private final List<String> missedSellTrades;
    private final List<String> lossTrades;

    // Track daily capital for drawdown calculation
    private final List<BigDecimal> dailyCapital;

    // Target date for splitting in-sample and out-sample data
    private static final String SPLIT_DATE = "2023-10-01";

    public Backtester(BigDecimal initialCapital, Strategy strategy, Map<String, List<MarketDataPoint>> marketData) {
        this.initialCapital = initialCapital;
        this.strategy = strategy;
        this.marketData = marketData;
        this.strategyReturns = new ArrayList<>();
        this.marketReturns = new ArrayList<>();
        this.missedBuyTrades = new ArrayList<>();
        this.missedSellTrades = new ArrayList<>();
        this.lossTrades = new ArrayList<>();
        this.dailyCapital = new ArrayList<>();
    }

    /**
     * Performs a standard backtest across all available data
     *
     * @return BacktestResult containing performance metrics
     */

    public BacktestResult runBacktest() {
        return runBacktestForData(marketData, initialCapital);
    }

    /**
     * Performs an in-sample backtest using data before the split date
     *
     * @return BacktestResult for in-sample data
     */
    public BacktestResult runInSampleBacktest() {
        Map<String, List<MarketDataPoint>> insampleData = new HashMap<>();
        for (String ticker : marketData.keySet()) {
            List<MarketDataPoint> data = marketData.get(ticker);
            System.out.println(data.size());
            int splitIndex = data.size()/2 - 1;
            List<MarketDataPoint> insample = data.subList(0, splitIndex);
            insampleData.put(ticker, insample);
        }
        return runBacktestForData(insampleData, initialCapital);
    }

    /**
     * Performs an out-sample backtest using data after the split date
     * Starts with the capital remaining from the in-sample backtest
     *
     * @return BacktestResult for out-sample data
     */
    public BacktestResult runOutSampleBacktest() {
        BacktestResult inSampleResult = runInSampleBacktest();
        BigDecimal remainingCapital = inSampleResult.finalCapital();

        // Prepare out-sample data
        Map<String, List<MarketDataPoint>> outsampleData = new HashMap<>();
        for (String ticker : marketData.keySet()) {
            List<MarketDataPoint> data = marketData.get(ticker);

            int splitIndex = data.size()/2 - 1;
            List<MarketDataPoint> outsample = data.subList(splitIndex, data.size());
            outsampleData.put(ticker, outsample);
        }

        // Run out-sample backtest with remaining capital
        return runBacktestForData(outsampleData, remainingCapital);
    }

    /**
     * Finds the index of the closest data point to a given date
     *
     * @param data List of market data points
     * @param targetDateStr Target date as a string
     * @return Closest index to the target date
     */
    private int findClosestIndexToDate(List<MarketDataPoint> data, String targetDateStr) {
        LocalDateTime targetDate = LocalDateTime.parse(targetDateStr + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        int closestIndex = -1; // Initialize to -1 to indicate no valid index found
        for (int i = 0; i < data.size(); i++) {
            LocalDateTime currentTimestamp = data.get(i).getTimestamp();

            // Check if current timestamp exceeds the target date
            if (currentTimestamp.isAfter(targetDate)) {
                break; // Stop processing if we exceed the target date
            }

            // Update closestIndex since currentTimestamp is less than or equal to targetDate
            closestIndex = i;
        }
        System.out.println(closestIndex);

        return 501; // Return -1 if no valid index was found
    }

    /**
     * Runs backtest for a specific subset of market data
     *
     * @param marketDataSubset Subset of market data to backtest
     * @param startingCapital Capital to start the backtest with
     * @return BacktestResult containing performance metrics
     */
    private BacktestResult runBacktestForData(Map<String, List<MarketDataPoint>> marketDataSubset, BigDecimal startingCapital) {
        // Clear previous returns and tracking lists
        strategyReturns.clear();
        marketReturns.clear();
        missedBuyTrades.clear();
        missedSellTrades.clear();
        lossTrades.clear();
        dailyCapital.clear();

        Portfolio portfolio = new Portfolio(startingCapital);
        List<Trade> trades = new ArrayList<>();

        int maxTimePeriods = marketDataSubset.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        for (int timeIndex = 1; timeIndex < maxTimePeriods; timeIndex++) {
            BigDecimal dailyReturn = BigDecimal.ZERO;
            BigDecimal marketReturn = BigDecimal.ZERO;

            int validTickers = 0;
            int squareOff = 0;

            for (String ticker : marketDataSubset.keySet()) {
                List<MarketDataPoint> tickerData = marketDataSubset.get(ticker);
                if (timeIndex >= tickerData.size()) {
                    continue;
                }

                // Market return calculation
                MarketDataPoint currentData = tickerData.get(timeIndex);
                MarketDataPoint previousData = tickerData.get(timeIndex - 1);

                BigDecimal currentPrice = currentData.getClose();
                BigDecimal previousPrice = previousData.getClose();

                if (previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                    marketReturn = marketReturn.add(
                            currentPrice.subtract(previousPrice)
                                    .divide(previousPrice, MathContext.DECIMAL128)
                    );
                    validTickers++;
                }

                // Strategy signal and trade processing
                List<MarketDataPoint> historicalData = tickerData.subList(0, timeIndex);
                Strategy.TradeSignal signal = strategy.determineTradeSignal(historicalData);

                switch (signal) {
                    case BUY:
                        int sharesToBuy = strategy.calculatePositionSize(portfolio.getTotalCapital(), currentPrice);
                        if (sharesToBuy > 0) {
                            portfolio.addPosition(ticker, BigDecimal.valueOf(sharesToBuy), currentPrice);
                            trades.add(new Trade(ticker, Trade.TradeType.BUY, sharesToBuy, currentPrice, currentData.getTimestamp()));
                        } else {
                            missedBuyTrades.add(ticker + " (Time: " + currentData.getTimestamp() + ")");
                        }
                        break;

                    case SELL:
                        Portfolio.PortfolioPosition position = portfolio.getPositions().get(ticker);
                        if (position != null && position.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                            squareOff++;
                            BigDecimal averagePrice = position.getAveragePrice();
                            int sharesToSell = position.getQuantity().intValue();

                            portfolio.removePosition(ticker, position.getQuantity(), currentPrice);
                            trades.add(new Trade(ticker, Trade.TradeType.SELL, sharesToSell, currentPrice, currentData.getTimestamp()));

                            BigDecimal profit = currentPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(sharesToSell));
                            dailyReturn = dailyReturn.add(profit.divide(averagePrice, MathContext.DECIMAL128));

                            if (profit.compareTo(BigDecimal.ZERO) < 0) {
                                lossTrades.add(ticker + " (Time: " + currentData.getTimestamp() + ") - Loss: " + profit);
                            }
                        } else {
                            missedSellTrades.add(ticker + " (Time: " + currentData.getTimestamp() + ")");
                        }
                        break;

                    default:
                        break;
                }
            }

            // Normalize market return
            if (validTickers > 0) {
                marketReturn = marketReturn.divide(BigDecimal.valueOf(validTickers), MathContext.DECIMAL128);
            }

            // Store returns
            if (squareOff > 0) {
                strategyReturns.add(dailyReturn.divide(BigDecimal.valueOf(squareOff), MathContext.DECIMAL128));
            } else {
                strategyReturns.add(BigDecimal.ZERO);
            }

            marketReturns.add(marketReturn);
            dailyCapital.add(portfolio.getTotalCapital());
        }

        // Perform regression analysis
        calculateRegression(strategyReturns, marketReturns);

        // Calculate final performance
        return calculatePerformance(portfolio, trades, getFinalPrices());
    }

    /**
     * Calculates regression analysis between strategy and market returns
     *
     * @param strategyReturns List of strategy returns
     * @param marketReturns List of market returns
     */
    private void calculateRegression(List<BigDecimal> strategyReturns, List<BigDecimal> marketReturns) {
        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < Math.min(strategyReturns.size(), marketReturns.size()); i++) {
            double market = marketReturns.get(i).doubleValue();
            double strategy = strategyReturns.get(i).doubleValue();
            regression.addData(market, strategy);
        }

        System.out.println("Regression Analysis:");
        System.out.println("Beta (Slope): " + regression.getSlope());
        System.out.println("Alpha (Intercept): " + regression.getIntercept());
        System.out.println("R-squared: " + regression.getRSquare());
    }

    /**
     * Calculates final performance metrics
     *
     * @param finalPortfolio Final portfolio state
     * @param trades List of trades executed
     * @param finalPrices Final prices of assets
     * @return BacktestResult containing performance metrics
     */
    private BacktestResult calculatePerformance(Portfolio finalPortfolio, List<Trade> trades, Map<String, BigDecimal> finalPrices) {
        BigDecimal finalValue = finalPortfolio.getTotalCapital();
        List<Trade> closingTrades = new ArrayList<>();

        for (Map.Entry<String, Portfolio.PortfolioPosition> entry : finalPortfolio.getPositions().entrySet()) {
            String ticker = entry.getKey();
            Portfolio.PortfolioPosition position = entry.getValue();

            if (finalPrices.containsKey(ticker)) {
                BigDecimal currentPrice = finalPrices.get(ticker);
                BigDecimal positionValue = position.getQuantity().multiply(currentPrice);
                finalValue = finalValue.add(positionValue);

                // Create a closing trade to log the action
                closingTrades.add(new Trade(ticker, Trade.TradeType.SELL, position.getQuantity().intValue(), currentPrice, trades.getLast().timestamp()));
            }
        }

        // Append closing trades to the overall trades list
        trades.addAll(closingTrades);

        BigDecimal totalReturn = finalValue.subtract(initialCapital)
                .divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        double risk = marketReturns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(marketReturns.size()), 4, RoundingMode.HALF_UP).doubleValue();

        return new BacktestResult(initialCapital, finalValue, totalReturn.doubleValue(), trades, strategyReturns,risk ,
                missedBuyTrades, missedSellTrades, lossTrades, dailyCapital);
    }

    /**
     * Retrieves final prices for all tickers
     *
     * @return Map of tickers to their final prices
     */
    private Map<String, BigDecimal> getFinalPrices() {
        Map<String, BigDecimal> finalPrices = new HashMap<>();
        LocalDateTime targetDate = LocalDateTime.of(2024, 10, 1, 16, 0);

        for (String ticker : marketData.keySet()) {
            List<MarketDataPoint> tickerData = marketData.get(ticker);

            // Find the closest data point to the target date
            MarketDataPoint closestDataPoint = findClosestDataPoint(tickerData, targetDate);

            if (closestDataPoint != null) {
                finalPrices.put(ticker, closestDataPoint.getClose());
            } else {
                // Fallback to the last available data point if no exact match
                if (!tickerData.isEmpty()) {
                    finalPrices.put(ticker, tickerData.get(tickerData.size() - 1).getClose());
                } else {
                    // If no data available, use a placeholder
                    finalPrices.put(ticker, new BigDecimal("100"));
                }
            }
        }

        return finalPrices;
    }

    /**
     * Finds the closest market data point to the target date
     *
     * @param dataPoints List of market data points
     * @param targetDate Target date to find closest point
     * @return Closest MarketDataPoint or null if no suitable point found
     */
    private MarketDataPoint findClosestDataPoint(List<MarketDataPoint> dataPoints, LocalDateTime targetDate) {
        return dataPoints.stream()
                .filter(point -> point.getTimestamp().toLocalDate().equals(targetDate.toLocalDate()))
                .findFirst()
                .orElseGet(() -> {
                    // If no exact match, find the closest point
                    return dataPoints.stream()
                            .min(Comparator.comparingLong(point ->
                                    Math.abs(ChronoUnit.DAYS.between(point.getTimestamp().toLocalDate(), targetDate.toLocalDate()))
                            ))
                            .orElse(null);
                });
    }
}