package org.example.strategy;

import static java.util.Objects.requireNonNull;

public class StrategyFactory {
    public enum StrategyType {
        RSI,
        MOVING_AVERAGE,

    }

    /**
     * Creates a strategy based on the given type
     *
     * @param type Strategy type to create
     * @return Corresponding Strategy implementation
     */
    public static Strategy createStrategy(StrategyType type) {
        if (StrategyType.RSI == requireNonNull(type)) {
            return new RsiStrategy();
            // Future strategies can be added here
        }
        throw new IllegalArgumentException("Unsupported strategy type: " + type);
    }
}