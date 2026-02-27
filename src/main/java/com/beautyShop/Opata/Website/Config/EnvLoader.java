package com.beautyShop.Opata.Website.Config;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * ─────────────────────────────────────────────────────────────
 * EnvLoader
 * ─────────────────────────────────────────────────────────────
 * Reads the .env file from the project root directory and loads
 * every key=value pair as a System property BEFORE Spring Boot
 * initializes, so ${PLACEHOLDER} values in application.properties
 * resolve correctly.
 *
 * Priority (highest → lowest):
 *   1. Real OS / platform environment variables (Render, Railway)
 *   2. .env file (local development)
 *   3. application.properties defaults  e.g. ${PORT:8080}
 *
 * Place this file at:
 *   src/main/java/com/beautyShop/Opata/Website/Config/EnvLoader.java
 * ─────────────────────────────────────────────────────────────
 */
@Slf4j
public class EnvLoader {

    private EnvLoader() {
        // Utility class — not instantiable
    }

    /**
     * Call this as the FIRST line inside main() before
     * SpringApplication.run(...) is called.
     *
     * Supported .env syntax:
     *   KEY=value
     *   KEY="value with spaces"
     *   KEY='value with spaces'
     *   # This is a comment  (skipped)
     *   (blank lines are skipped)
     */
    public static void load() {
        load(".env"); // default: project root .env
    }

    /**
     * Overload — load a custom path (e.g. for tests or multi-env setups).
     * Example: EnvLoader.load(".env.test")
     */
    public static void load(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            int loaded = 0;
            int skipped = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // ── Skip blank lines and comments ─────────────
                if (line.isEmpty() || line.startsWith("#")) continue;

                // ── Must contain '=' ──────────────────────────
                int equalIndex = line.indexOf('=');
                if (equalIndex <= 0) {
                    log.warn("⚠️  [EnvLoader] Skipping malformed line (no '='): {}", line);
                    continue;
                }

                String key   = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();

                // ── Strip surrounding quotes ──────────────────
                // Handles: KEY="my value"  or  KEY='my value'
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last  = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                // ── OS env vars take priority over .env ───────
                // This ensures real deployment env vars are never
                // overwritten by local .env values
                if (System.getenv(key) != null) {
                    log.debug("⏭️  [EnvLoader] Skipping '{}' — already set in OS environment", key);
                    skipped++;
                    continue;
                }

                // ── Set as system property for Spring to pick up
                System.setProperty(key, value);
                loaded++;
                log.debug("✅ [EnvLoader] Loaded: {} = {}", key, maskSecret(key, value));
            }

            log.info("✅ [EnvLoader] .env loaded — {} properties set, {} skipped (OS env priority)", loaded, skipped);

        } catch (IOException e) {
            // Not an error — on production servers (Railway, Render, Heroku)
            // there is no .env file; env vars are injected by the platform.
            log.info("ℹ️  [EnvLoader] No .env file found at '{}' — using OS environment variables only", filePath);
        }
    }

    /**
     * Masks secret values in debug logs so they never appear in plaintext.
     * Keys containing: secret, password, key, token → shown as ****
     */
    private static String maskSecret(String key, String value) {
        String lower = key.toLowerCase();
        if (lower.contains("secret") || lower.contains("password")
                || lower.contains("key") || lower.contains("token")) {
            return "****";
        }
        return value;
    }
}