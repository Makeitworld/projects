package org.example.repository;

import org.example.models.MarketDataPoint;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class MarketDataRepository {
    private final SessionFactory sessionFactory;

    public MarketDataRepository() {
        // Load Hibernate configuration
        sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
    }

    // Save or update a MarketDataPoint based on unique identifier (timestamp + ticker)
    public void saveOrUpdate(MarketDataPoint marketDataPoint) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.merge(marketDataPoint); // merge will handle save or update based on existence
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    // Retrieve all MarketDataPoints
    public List<MarketDataPoint> getAll() {
        Session session = sessionFactory.openSession();
        List<MarketDataPoint> dataPoints = session.createQuery("FROM MarketDataPoint", MarketDataPoint.class).list();
        session.close();
        return dataPoints;
    }

    // Retrieve by ticker
    public List<MarketDataPoint> getByTicker(String ticker) {
        Session session = sessionFactory.openSession();
        List<MarketDataPoint> dataPoints = session.createQuery("FROM MarketDataPoint WHERE ticker = :ticker", MarketDataPoint.class)
                .setParameter("ticker", ticker).list();
        session.close();
        return dataPoints;
    }

    // Close SessionFactory
    public void close() {
        sessionFactory.close();
    }
}
