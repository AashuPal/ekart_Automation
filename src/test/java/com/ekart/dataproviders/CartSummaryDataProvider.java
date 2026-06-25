package com.ekart.dataproviders;

import com.ekart.utils.CsvDataReader;
import org.testng.annotations.DataProvider;

import java.util.List;
import java.util.Map;

/**
 * Data Provider for Cart Order Summary & Total Calculation tests.
 *
 * Frontend calculation formulas (from CartPage.jsx):
 *   subtotal = sum(unitPrice × quantity)
 *   shipping = subtotal > 500 ? 0 : 40
 *   tax      = Math.round(subtotal × 0.18)
 *   total    = subtotal + shipping + tax
 *
 * Provides 3 types of datasets:
 *  1. orderSummaryDataset  — hardcoded inline data (quick verification)
 *  2. csvOrderSummaryData  — CSV-driven data (all 7 rows from CSV file)
 *  3. totalCalculationDataset — for total formula verification
 *  4. shippingThresholdDataset — boundary tests around ₹500
 *  5. csvAboveThresholdData — CSV rows filtered by scenario=above_threshold
 */
public class CartSummaryDataProvider {

    private static final String PRODUCT_A = "656a732e-9e66-496b-83c0-8991a7a987c0";
    private static final String CSV_PATH  = "testdata/cart-order-summary-data.csv";

    // =========================================================================
    // 1. Inline @DataProvider — order summary across quantities
    // =========================================================================
    /**
     * Columns: testId, description, productId, quantity, expectedShipping
     */
    @DataProvider(name = "orderSummaryDataset")
    public static Object[][] orderSummaryDataset() {
        return new Object[][] {
            // testId    description                           productId   qty  shipping
            { "DS_001", "qty=1 subtotal=5525 → shipping=40",  PRODUCT_A,  1,   40.0 },
            { "DS_002", "qty=2 subtotal=11050 → FREE ship",   PRODUCT_A,  2,   0.0  },
            { "DS_003", "qty=3 subtotal=16575 → FREE ship",   PRODUCT_A,  3,   0.0  },
            { "DS_004", "qty=5 subtotal=27625 → FREE ship",   PRODUCT_A,  5,   0.0  },
            { "DS_005", "qty=10 subtotal=55250 → FREE ship",  PRODUCT_A,  10,  0.0  },
        };
    }

    // =========================================================================
    // 2. CSV-driven @DataProvider — reads from cart-order-summary-data.csv
    // =========================================================================
    /**
     * Each Object[] contains one Map<String, String> with keys:
     *   testCaseId, description, productId, quantity,
     *   expectedShipping, expectedTaxRate, scenario
     */
    @DataProvider(name = "csvOrderSummaryData")
    public static Object[][] csvOrderSummaryData() {
        return CsvDataReader.toDataProviderArray(CSV_PATH);
    }

    // =========================================================================
    // 3. Total calculation dataset
    // =========================================================================
    /**
     * Columns: testId, description, productId, quantity
     */
    @DataProvider(name = "totalCalculationDataset")
    public static Object[][] totalCalculationDataset() {
        return new Object[][] {
            { "TC_001", "qty=1  total=subtotal+40+tax",  PRODUCT_A, 1  },
            { "TC_002", "qty=2  total=subtotal+0+tax",   PRODUCT_A, 2  },
            { "TC_003", "qty=5  total=subtotal+0+tax",   PRODUCT_A, 5  },
            { "TC_004", "qty=10 total=subtotal+0+tax",   PRODUCT_A, 10 },
        };
    }

    // =========================================================================
    // 4. Shipping threshold boundary dataset
    // =========================================================================
    /**
     * Columns: testId, description, productId, quantity, subtotalAbove500
     */
    @DataProvider(name = "shippingThresholdDataset")
    public static Object[][] shippingThresholdDataset() {
        return new Object[][] {
            // qty=1 → 5525 > 500 → FREE
            { "ST_001", "subtotal=5525 > 500 → FREE shipping", PRODUCT_A, 1, true  },
            // qty=2 → 11050 > 500 → FREE
            { "ST_002", "subtotal=11050 > 500 → FREE",         PRODUCT_A, 2, true  },
        };
    }

    // =========================================================================
    // 5. CSV filtered — only above_threshold rows
    // =========================================================================
    @DataProvider(name = "csvAboveThresholdData")
    public static Object[][] csvAboveThresholdData() {
        List<Map<String, String>> rows = CsvDataReader.readFiltered(
            CSV_PATH, "scenario", "above_threshold");
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) data[i][0] = rows.get(i);
        return data;
    }
}