package org.example.datafetcher;

import org.example.models.StockFundamentals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StockSelector {
    private final Map<String, String> sectorMap;

    public StockSelector() {
        SectorMapCreator sectorMapCreator = new SectorMapCreator();
        sectorMap = SectorMapCreator.createSectorMap();
    }

    /**
     * Filters stocks based on average sector performance and return on equity (ROE).
     *
     * @param fundamentalsList List of StockFundamentals
     * @return Filtered list of StockFundamentals
     */
    public List<StockFundamentals> filterBasedOnSectorAverages(List<StockFundamentals> fundamentalsList) {
        // Step 1: Compute sector averages
        Map<String, Map<String, Double>> sectorAverages = fundamentalsList.stream()
                .collect(Collectors.groupingBy(
                        stock -> {
                            String sector = sectorMap.get(stock.getTicker());
                            return sector != null ? sector : "Unknown";
                        },
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                stocks -> {
                                    // Calculate averages for each sector
                                    double avgDividendYield = stocks.stream()
                                            .mapToDouble(StockFundamentals::getDividendYield)
                                            .average().orElse(0.0);

                                    double avgPeRatio = stocks.stream()
                                            .mapToDouble(StockFundamentals::getPeRatio)
                                            .filter(ratio -> ratio > 0) // Filter out negative or zero PE ratios
                                            .average().orElse(0.0);

                                    double avgPbRatio = stocks.stream()
                                            .mapToDouble(StockFundamentals::getPbRatio)
                                            .filter(ratio -> ratio > 0) // Filter out negative or zero PB ratios
                                            .average().orElse(0.0);

                                    Map<String, Double> averages = new HashMap<>();
                                    averages.put("dividend_yield", avgDividendYield);
                                    averages.put("pe_ratio", avgPeRatio);
                                    averages.put("pb_ratio", avgPbRatio);
                                    return averages;
                                }
                        )
                ));

        // Step 2: Compute overall average ROE
        double avgROE = fundamentalsList.stream()
                .mapToDouble(StockFundamentals::getReturnOnEquity)
                .filter(roe -> roe > 0) // Filter out negative or zero ROE
                .average().orElse(0.0);

        // Step 3: Filter stocks based on criteria
        return fundamentalsList.stream()
                .filter(stock -> {
                    String sector = sectorMap.get(stock.getTicker());
                    if (sector == null) {
                        return false; // Skip if sector data is missing
                    }

                    Map<String, Double> sectorAvg = sectorAverages.get(sector);
                    if (sectorAvg == null) {
                        return false; // Skip if sector data is missing
                    }

                    int criteriaMet = 0;
                    if (stock.getDividendYield() > sectorAvg.getOrDefault("dividend_yield", 0.0)) criteriaMet++;
                    if (stock.getPeRatio() < sectorAvg.getOrDefault("pe_ratio", 0.0)) criteriaMet++;
                    if (stock.getPbRatio() < sectorAvg.getOrDefault("pb_ratio", 0.0)) criteriaMet++;
                    if(stock.getReturnOnEquity() > avgROE) criteriaMet++;


                    // At least 2 of the 4 criteria should be met
                    return criteriaMet >= 2;
                })
                .collect(Collectors.toList());
    }
}