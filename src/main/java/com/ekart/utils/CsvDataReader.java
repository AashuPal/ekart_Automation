package com.ekart.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CsvDataReader — Reusable utility to load test data from CSV files.
 *
 * Usage:
 *   List<Map<String, String>> rows = CsvDataReader.read("testdata/cart-order-summary-data.csv");
 *   Object[][] data = CsvDataReader.toDataProviderArray("testdata/cart-order-summary-data.csv");
 *
 * CSV format: first row = headers, subsequent rows = data.
 * Files must be on the classpath (src/test/resources/).
 */
public class CsvDataReader {

    private static final Logger log = LogManager.getLogger(CsvDataReader.class);

    /**
     * Read a CSV file from the classpath.
     * @param resourcePath path relative to classpath root, e.g. "testdata/cart-order-summary-data.csv"
     * @return list of rows where each row is a Map of header→value
     */
    public static List<Map<String, String>> read(String resourcePath) {
        List<Map<String, String>> rows = new ArrayList<>();
        try {
            InputStream is = CsvDataReader.class.getClassLoader()
                                 .getResourceAsStream(resourcePath);
            if (is == null) {
                log.error("CSV file not found on classpath: " + resourcePath);
                return rows;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String headerLine = reader.readLine();
            if (headerLine == null) return rows;

            String[] headers = headerLine.split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i].trim(),
                            i < values.length ? values[i].trim() : "");
                }
                rows.add(row);
            }
            log.info("CSV loaded: " + resourcePath + " | rows=" + rows.size());
        } catch (Exception e) {
            log.error("Failed to read CSV: " + resourcePath + " | " + e.getMessage());
        }
        return rows;
    }

    /**
     * Convert CSV to TestNG @DataProvider format (Object[][]).
     * Each Object[] contains one Map<String, String> row.
     */
    public static Object[][] toDataProviderArray(String resourcePath) {
        List<Map<String, String>> rows = read(resourcePath);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    /**
     * Filter rows by a specific column value.
     * e.g. filter by scenario="above_threshold"
     */
    public static List<Map<String, String>> readFiltered(String resourcePath,
                                                          String column,
                                                          String value) {
        List<Map<String, String>> all = read(resourcePath);
        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> row : all) {
            if (value.equalsIgnoreCase(row.getOrDefault(column, ""))) {
                filtered.add(row);
            }
        }
        return filtered;
    }
}