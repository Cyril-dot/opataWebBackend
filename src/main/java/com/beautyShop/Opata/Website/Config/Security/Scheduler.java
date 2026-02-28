package com.beautyShop.Opata.Website.Config.Security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final JdbcTemplate jdbcTemplate;

    private static final String RENDER_URL = "https://opatawebbackend.onrender.com/ping";

    public Scheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Ping every 5 seconds to keep Render free tier warm
    @Scheduled(fixedRate = 5000)
    public void keepAlive() {

        // 1. HTTP self-ping — keeps Render from sleeping
        try {
            String response = restTemplate.getForObject(RENDER_URL, String.class);
            logger.info("HTTP ping OK at {} | {}", java.time.LocalDateTime.now(), response);
        } catch (Exception e) {
            logger.error("HTTP ping failed at {} | {}", java.time.LocalDateTime.now(), e.getMessage());
        }

        // 2. DB ping — keeps Railway PostgreSQL connection pool alive
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            logger.debug("DB ping OK at {}", java.time.LocalDateTime.now());
        } catch (Exception e) {
            logger.error("DB ping failed at {} | {}", java.time.LocalDateTime.now(), e.getMessage());
        }
    }
}