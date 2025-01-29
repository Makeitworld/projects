package org.example.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AppLogger {
    private final Logger logger;

    public AppLogger(Class<?> clazz) {
        this.logger = Logger.getLogger(clazz.getName());
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warning(message);
    }

    public void error(String message) {
        logger.severe(message);
    }

    public void error(String message, Throwable exception) {
        logger.log(Level.SEVERE, message, exception);
    }
}