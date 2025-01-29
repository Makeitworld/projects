package org.example;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.example.backtest.BacktestResult;
import org.example.backtest.Backtester;
import org.example.datafetcher.ApiMarketDataFetcher;
import org.example.datafetcher.DatabaseMarketDataFetcher;
import org.example.datafetcher.FundamentalDataFetcher;
import org.example.datafetcher.StockSelector;
import org.example.models.MarketDataPoint;
import org.example.models.StockFundamentals;
import org.example.repository.MarketDataRepository;
import org.example.repository.StockFundamentalsRepository;
import org.example.strategy.MovingAverageStrategy;
import org.example.strategy.RsiStrategy;
import org.example.utils.AppLogger;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final AppLogger logger = new AppLogger(Main.class);

    // Configuration constants
    private static final String API_KEY = "UH3MZPUZMQ0W7S4";
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000");

    public static void main(String[] args) {
        try {
            // Initialize dependencies
            MarketDataRepository marketDataRepository = new MarketDataRepository();
            DatabaseMarketDataFetcher databaseFetcher = new DatabaseMarketDataFetcher(marketDataRepository);

            // Fetch and filter stock fundamentals
            List<StockFundamentals> filteredStocks = fetchAndFilterStocks();

            // Prepare market data for backtesting
            Map<String, List<MarketDataPoint>> marketDataMap = prepareMarketData(filteredStocks, databaseFetcher);
            MovingAverageStrategy strategy=new MovingAverageStrategy();
            BigDecimal initialCapital = new BigDecimal("100000");
            Backtester b=new Backtester(initialCapital, strategy,marketDataMap);

            // Run backtest
            BacktestResult backtestResult = b.runBacktest();

            // Display results
            displayBacktestResults(backtestResult);

        } catch (Exception e) {
            logger.error("Error in main application flow", e);
        }
    }
    public static void plotDailyReturns(List<BigDecimal> dailyReturns) {
        // Create an XYSeries to hold the data
        XYSeries series = new XYSeries("Daily Returns");

        // Populate the series with the daily returns and time points (e.g., day 1, day 2, ...)
        for (int i = 0; i < dailyReturns.size(); i++) {
            series.add(i + 1, dailyReturns.get(i).doubleValue()); // i+1 for days starting from 1
        }

        // Create a dataset from the series
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        // Create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Daily Returns Over Time",    // Title
                "Time (Days)",                // X-Axis Label (Time in days)
                "Daily Return",               // Y-Axis Label (Return value)
                dataset,                      // Dataset
                PlotOrientation.VERTICAL,     // Plot orientation
                true,                         // Include legend
                true,                         // Tooltips
                false                         // URLs
        );

        // Create a panel for the chart and display it in a JFrame
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        JFrame frame = new JFrame("Daily Return Plot");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }

    private static List<StockFundamentals> fetchAndFilterStocks() {
        StockFundamentalsRepository repository = new StockFundamentalsRepository();
        FundamentalDataFetcher dataFetcher = new FundamentalDataFetcher(repository);
        StockSelector stockSelector = new StockSelector();

        // Fetch all fundamental data
        List<StockFundamentals> allFundamentals = dataFetcher.getAllFundamentalDataFromDatabase();

        // Filter stocks based on sector averages
        List<StockFundamentals> filteredStocks = stockSelector.filterBasedOnSectorAverages(allFundamentals);

        // Log filtered stocks
        logger.info("Filtered stocks count: " + filteredStocks.size());
        printFilteredTickers(filteredStocks);

        return filteredStocks;
    }

    private static Map<String, List<MarketDataPoint>> prepareMarketData(
            List<StockFundamentals> filteredStocks,
            DatabaseMarketDataFetcher databaseFetcher) {

        Map<String, List<MarketDataPoint>> marketDataMap = new HashMap<>();

        for (StockFundamentals stock : filteredStocks) {
            String ticker = stock.getTicker();
            List<MarketDataPoint> dataPoints = databaseFetcher.fetchMarketData(ticker);
            marketDataMap.put(ticker, dataPoints);
        }

        return marketDataMap;
    }

    private static BacktestResult runBacktest(Map<String, List<MarketDataPoint>> marketDataMap) {
        RsiStrategy strategy = new RsiStrategy();
        Backtester backtester = new Backtester(INITIAL_CAPITAL, strategy, marketDataMap);

        return backtester.runBacktest();
    }

    private static void displayBacktestResults(BacktestResult result) {
        System.out.println("Backtest Results:");
        System.out.println("Initial Capital: " + INITIAL_CAPITAL);
        System.out.println("Final Portfolio Value: " + result.finalCapital());
        System.out.println("Total Return: " + result.totalReturn() + "%");
        System.out.println("Sharpe Ratio: " + result.calculateSharpeRatio());
        System.out.println("Sortino Ratio: " + result.calculateSortinoRatio());
        System.out.println("Maximum Drawdown: " + result.calculateDownsideDeviation());
    }

    private static void printFilteredTickers(List<StockFundamentals> filteredStocks) {
        filteredStocks.forEach(stock -> System.out.println(stock.getTicker()));
    }

    // Optional: Method to fetch and save market data from API (currently commented out)
    private void fetchMarketDataFromApi() {
        try {
            // Initialize market data fetchers
            String apiKey = "UH3MZPUZMQ0W7S4";
            MarketDataRepository market = new MarketDataRepository();
            DatabaseMarketDataFetcher databaseFetcher = new DatabaseMarketDataFetcher(market);
            ApiMarketDataFetcher apiFetcher = new ApiMarketDataFetcher();


            // Define parameters for fetching market data
            String symbol = "BIIB";
            String interval = "Daily";
            LocalDateTime startTimestamp = LocalDateTime.of(2021, 10, 1, 0, 0);
            LocalDateTime endTimestamp = LocalDateTime.of(2024, 10, 1, 0, 0);

            // Fetch market data from API
            List<MarketDataPoint> marketData = apiFetcher.fetchMarketData(symbol, interval, apiKey, startTimestamp, endTimestamp);
            databaseFetcher.saveOrUpdateMarketData(marketData);

            // Print out the fetched data statistics
            System.out.println("Total data points fetched: " + marketData.size());
            }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}