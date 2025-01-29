package org.example.repository;

import org.example.models.StockFundamentals;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class StockFundamentalsRepository {
    private final SessionFactory sessionFactory;

    public StockFundamentalsRepository() {
        // Build SessionFactory only once for performance
        sessionFactory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(StockFundamentals.class)
                .buildSessionFactory();
    }

    // Save or update a stock fundamental record
    public void saveOrUpdate(StockFundamentals stockFundamentals) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(stockFundamentals);  // Works for both new and existing objects
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();  // Consider using a logging framework here
        }
    }

    // Find a stock fundamental by its ticker
    public StockFundamentals findByTicker(String ticker) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM StockFundamentals WHERE ticker = :ticker", StockFundamentals.class)
                    .setParameter("ticker", ticker)
                    .uniqueResult();
        }
    }

    // Fetch all stock fundamentals
    public List<StockFundamentals> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM StockFundamentals", StockFundamentals.class).list();
        }
    }

    // Cleanly close the session factory when shutting down
    public void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
