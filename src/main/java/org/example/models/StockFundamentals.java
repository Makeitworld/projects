package org.example.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_fundamental") // Maps this class to the table 'stock_fundamentals'
public class StockFundamentals {

    @Id
    @Column(name = "ticker", nullable = false, unique = true) // Primary key column
    private String ticker;

    // Financial Ratios (using Double wrappers to handle null values)
    @Column(name = "pe_ratio")
    private Double peRatio; // Price to Earnings Ratio

    @Column(name = "pb_ratio")
    private Double pbRatio; // Price to Book Ratio

    @Column(name = "return_on_equity")
    private Double returnOnEquity; // Return on Equity

    @Column(name = "dividend_yield")
    private Double dividendYield; // Dividend Yield

    // Constructor
    public StockFundamentals(String ticker) {
        this.ticker = ticker;
    }

    // Detailed constructor
    public StockFundamentals(String ticker, Double peRatio, Double pbRatio,
                             Double returnOnEquity, Double dividendYield) {
        this.ticker = ticker;
        this.peRatio = peRatio;
        this.pbRatio = pbRatio;
        this.returnOnEquity = returnOnEquity;
        this.dividendYield = dividendYield;
    }

    // Default constructor for Hibernate
    public StockFundamentals() {
        // Default constructor for Hibernate
    }

    // Getters and Setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Double getPeRatio() {
        return peRatio != null ? peRatio : 0.0; // Return 0 if null
    }

    public void setPeRatio(Double peRatio) {
        this.peRatio = peRatio;
    }

    public Double getPbRatio() {
        return pbRatio != null ? pbRatio : 0.0; // Return 0 if null
    }

    public void setPbRatio(Double pbRatio) {
        this.pbRatio = pbRatio;
    }

    public Double getReturnOnEquity() {
        return returnOnEquity != null ? returnOnEquity : 0.0; // Return 0 if null
    }

    public void setReturnOnEquity(Double returnOnEquity) {
        this.returnOnEquity = returnOnEquity;
    }

    // Getter for dividendYield with null handling
    public Double getDividendYield() {
        return dividendYield != null ? dividendYield : 0.0; // Return 0 if null
    }

    public void setDividendYield(Double dividendYield) {
        this.dividendYield = dividendYield;
    }

    @Override
    public String toString() {
        return "StockFundamentals {" +
                "ticker='" + ticker + '\'' +
                ", peRatio=" + (peRatio != null ? peRatio : 0.0) +
                ", pbRatio=" + (pbRatio != null ? pbRatio : 0.0) +
                ", returnOnEquity=" + (returnOnEquity != null ? returnOnEquity : 0.0) +
                ", dividendYield=" + (dividendYield != null ? dividendYield : 0.0) +
                '}';
    }
}
