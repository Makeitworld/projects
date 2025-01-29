package org.example.datafetcher;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SectorMapCreator {
    public static Map<String, String> createSectorMap() {
        Map<String, String> sectorMap = new HashMap<>();

        try (InputStream inputStream = SectorMapCreator.class.getResourceAsStream("/sp500_tickers.csv");
             InputStreamReader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();

            for (String[] row : rows) {
                if (row.length >= 2) {
                    String ticker = row[0];
                    String sector = row[1];
                    sectorMap.put(ticker, sector);
                }
            }

        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        return sectorMap;
    }
}