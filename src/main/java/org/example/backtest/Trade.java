package org.example.backtest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Trade(String ticker, org.example.backtest.Trade.TradeType type, int quantity, BigDecimal price,
                    LocalDateTime timestamp) {
    public enum TradeType {
        BUY, SELL
    }

}
