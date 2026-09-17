package com.netbanking.auth.config;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
public final class DotenvLoader {

    private DotenvLoader() {}

    public static void load() {
        File[] candidateFiles = new File[] {
                new File(".env"),
                new File("Model_Class/auth-service/.env"),
                new File("auth-service/.env"),
                new File("../.env"),
                new File("../../.env")
        };

        boolean loaded = false;
        for (File file : candidateFiles) {
            if (file.exists() && file.isFile()) {
                loadFromFile(file);
                loaded = true;
                break;
            }
        }

        if (!loaded) {
            InputStream is = DotenvLoader.class.getResourceAsStream("/.env");
            if (is != null) {
                loadFromStream(is, "classpath:/.env");
            }
        }
    }

    private static void loadFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            loadFromBufferedReader(reader, file.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to read .env file at {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    private static void loadFromStream(InputStream is, String source) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            loadFromBufferedReader(reader, source);
        } catch (Exception e) {
            log.warn("Failed to read .env from {}: {}", source, e.getMessage());
        }
    }

    private static void loadFromBufferedReader(BufferedReader reader, String source) throws Exception {
        String line;
        int count = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int sepIndex = line.indexOf('=');
            if (sepIndex > 0) {
                String key = line.substring(0, sepIndex).trim();
                String value = line.substring(sepIndex + 1).trim();

                // Strip outer quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                    count++;
                }
            }
        }
        log.info("Loaded {} environment variables from .env ({})", count, source);
    }
}
