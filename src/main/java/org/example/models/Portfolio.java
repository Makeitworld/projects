package org.example.models;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Portfolio {
    private BigDecimal totalCapital;
    private final Map<String, PortfolioPosition> positions;

    public Portfolio(BigDecimal initialCapital) {
        this.totalCapital = initialCapital;
        this.positions = new HashMap<>();
    }

    public BigDecimal getTotalCapital() {
        return totalCapital;
    }

    public Map<String, PortfolioPosition> getPositions() {
        return positions;
    }

    // Add position to portfolio
    public void addPosition(String ticker, BigDecimal quantity, BigDecimal price) {
        PortfolioPosition position = positions.getOrDefault(ticker, new PortfolioPosition());

        // Calculate the new average price based on the old position and new position
        BigDecimal newAveragePrice = position.calculateNewAveragePrice(quantity, price);

        // Update position and total capital
        position.addQuantity(quantity);
        position.setAveragePrice(newAveragePrice);
        totalCapital = totalCapital.subtract(price.multiply(quantity));
        positions.put(ticker, position);
    }

    // Remove position from portfolio
    public void removePosition(String ticker, BigDecimal quantity, BigDecimal price) {
        PortfolioPosition position = positions.get(ticker);
        if (position != null) {
            BigDecimal availableQuantity = position.getQuantity();
            if (availableQuantity.compareTo(quantity) >= 0) {
                // Enough quantity available, remove the specified quantity
                position.removeQuantity(quantity);
                totalCapital = totalCapital.add(price.multiply(quantity));
            } else {
                // Not enough quantity, remove all available quantity
                totalCapital = totalCapital.add(price.multiply(availableQuantity));
                position.removeQuantity(availableQuantity); // Set quantity to zero
            }

            // If quantity becomes zero, remove the position from the portfolio
            if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                positions.remove(ticker);
            }
        } else {
            // No position found, nothing to remove
            System.out.println("No position found for " + ticker);
        }
    }

    // PortfolioPosition now uses BigDecimal for quantity and price
    public static class PortfolioPosition {
        private BigDecimal quantity;
        private BigDecimal averagePrice;

        public PortfolioPosition() {
            this.quantity = BigDecimal.ZERO; // Initialize with zero quantity
            this.averagePrice = BigDecimal.ZERO; // Initialize with zero average price
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public BigDecimal getAveragePrice() {
            return averagePrice;
        }

        // Set average price
        public void setAveragePrice(BigDecimal averagePrice) {
            this.averagePrice = averagePrice;
        }

        // Add quantity
        public void addQuantity(BigDecimal quantity) {
            this.quantity = this.quantity.add(quantity);
        }

        // Remove quantity
        public void removeQuantity(BigDecimal quantity) {
            this.quantity = this.quantity.subtract(quantity);
            // Ensure the quantity doesn't go negative
            if (this.quantity.compareTo(BigDecimal.ZERO) < 0) {
                this.quantity = BigDecimal.ZERO;
            }
        }

        // Calculate new average price after adding new quantity at a given price
        public BigDecimal calculateNewAveragePrice(BigDecimal newQuantity, BigDecimal newPrice) {
            if (this.quantity.compareTo(BigDecimal.ZERO) == 0) {
                // If there's no existing quantity, the new average price is just the new price
                return newPrice;
            }

            // Calculate the weighted average price
            BigDecimal totalCost = this.averagePrice.multiply(this.quantity).add(newPrice.multiply(newQuantity));
            BigDecimal newTotalQuantity = this.quantity.add(newQuantity);
            return totalCost.divide(newTotalQuantity, 4, BigDecimal.ROUND_HALF_UP);
        }
    }
}
