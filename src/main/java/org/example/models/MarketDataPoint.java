package org.example.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "market_data_points")
public class MarketDataPoint {

    @Id
    private String id; // Unique identifier (e.g., ticker + timestamp concatenation)

    @Column(name = "ticker", nullable = false)
    private String ticker;

    private LocalDateTime timestamp;

    private BigDecimal open;  // Financial data fields use BigDecimal
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private long volume;

    // Constructor
    public MarketDataPoint(String ticker, LocalDateTime timestamp, BigDecimal open,
                           BigDecimal high, BigDecimal low, BigDecimal close, long volume) {
        this.id = ticker + "_" + timestamp.toString(); // Generating a unique ID
        this.ticker = ticker;
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    // Default constructor for JPA
    public MarketDataPoint() {
    }

    // Getters
    public String getId() { return id; }
    public String getTicker() { return ticker; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public long getVolume() { return volume; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public void setLow(BigDecimal low) { this.low = low; }
    public void setClose(BigDecimal close) { this.close = close; }
    public void setVolume(long volume) { this.volume = volume; }

    @Override
    public String toString() {
        return "MarketDataPoint{" +
                "id='" + id + '\'' +
                ", ticker='" + ticker + '\'' +
                ", timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                '}';
    }
}
