package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.dataproviders.CartSummaryDataProvider;
import com.ekart.pages.CartPage;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Data-Driven Order Summary Validation & Total Calculation Tests
 *
 * Frontend formulas (CartPage.jsx lines 51-54):
 *   subtotal = sum(unitPrice × quantity)
 *   shipping = subtotal > 500 ? 0 : 40
 *   tax      = Math.round(subtotal × 0.18)
 *   total    = subtotal + shipping + tax
 *
 * ── Validate Order Summary ────────────────────────────────────────
 *  TC_SUM_001  Order Summary section visible with all 4 values
 *  TC_SUM_002  Subtotal equals sum of all item row totals
 *  TC_SUM_003  [Data-Driven] Shipping = 0 when subtotal > ₹500, else ₹40
 *  TC_SUM_004  [Data-Driven] Tax = Math.round(subtotal × 0.18)
 *  TC_SUM_005  Free shipping nudge absent when subtotal > ₹500
 *
 * ── Verify Total Calculation ──────────────────────────────────────
 *  TC_TOT_001  [Data-Driven] Total = subtotal + shipping + tax
 *  TC_TOT_002  Total recalculates after quantity change
 *  TC_TOT_003  [Data-Driven] Summary consistent at shipping threshold
 *
 * ── CSV-Driven Tests ──────────────────────────────────────────────
 *  TC_CSV_001  [CSV] All datasets: shipping rule correct
 *  TC_CSV_002  [CSV] All datasets: tax = 18% of subtotal
 *  TC_CSV_003  [CSV-Filtered] above_threshold rows: shipping = FREE
 */
public class OrderSummaryTest extends BaseTest {

    private static final String PRODUCT_A = "656a732e-9e66-496b-83c0-8991a7a987c0";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;
    private CartPage           cartPage;

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));
        loginAsValidUser();
        log.info("Logged in once for OrderSummaryTest");
    }

    @BeforeMethod
    public void setup() {
        listingPage = new ProductListingPage(getDriver());
        detailPage  = new ProductDetailPage(getDriver());
        cartPage    = new CartPage(getDriver());

        try {
            if (getDriver().getCurrentUrl().contains("/login"))
                loginAsValidUser();
        } catch (Exception ignored) {}

        try {
            getDriver().get(baseUrl);
            WaitUtils.waitForPageReady(getDriver());
            cartPage.clearCartViaLocalStorage();
        } catch (Exception e) {
            log.warn("Cart clear skipped: " + e.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Add product by ID, set qty via + button, navigate to cart. */
    private void addProductWithQty(String productId, int qty) {
        listingPage.navigateToProduct(baseUrl, productId);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();
        cartPage.navigateTo(baseUrl);
        for (int i = 1; i < qty; i++) {
            cartPage.increaseQuantity(0);
            waitForQty(i + 1);
        }
    }

    private void waitForQty(int expected) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> {
                    try { return cartPage.getItemQuantity(0) == expected; }
                    catch (Exception e) { return false; }
                });
        } catch (Exception ignored) {}
    }

    private double calcExpectedShipping(double subtotal) {
        return subtotal > 500 ? 0.0 : 40.0;
    }

    private double calcExpectedTax(double subtotal) {
        return Math.round(subtotal * 0.18);
    }

    // =========================================================================
    // VALIDATE ORDER SUMMARY
    // =========================================================================

    // ── TC_SUM_001 ─────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_SUM_001: Order Summary section visible with all 4 values")
    public void testOrderSummaryAllValuesVisible() {
        addProductWithQty(PRODUCT_A, 1);

        Assert.assertTrue(cartPage.isOrderSummaryVisible(),
            "Order Summary heading must be visible");

        double subtotal = cartPage.getSubtotal();
        double shipping = cartPage.getShipping();
        double tax      = cartPage.getTax();
        double total    = cartPage.getTotal();

        Assert.assertTrue(subtotal > 0,  "Subtotal must be > 0. Got=" + subtotal);
        Assert.assertTrue(shipping >= 0, "Shipping must be >= 0. Got=" + shipping);
        Assert.assertTrue(tax > 0,       "Tax must be > 0. Got=" + tax);
        Assert.assertTrue(total > 0,     "Total must be > 0. Got=" + total);

        log.info("PASS TC_SUM_001 | subtotal=" + subtotal + " shipping="
                 + shipping + " tax=" + tax + " total=" + total);
    }

    // ── TC_SUM_002 ─────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_SUM_002: Subtotal equals sum of all item row totals")
    public void testSubtotalEqualsSumOfRows() {
        listingPage.navigateTo(baseUrl);
        java.util.List<org.openqa.selenium.WebElement> cards =
            listingPage.getAllProductCards();
        Assert.assertTrue(cards.size() >= 2, "Need at least 2 products");

        String id1 = listingPage.getCardProductId(cards.get(0));
        String id2 = listingPage.getCardProductId(cards.get(1));

        listingPage.navigateToProduct(baseUrl, id1);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        listingPage.navigateToProduct(baseUrl, id2);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        double rowSum = 0;
        for (int i = 0; i < cartPage.getCartItemCount(); i++) {
            rowSum += cartPage.getItemRowTotal(i);
        }
        double subtotal = cartPage.getSubtotal();

        Assert.assertEquals(subtotal, rowSum, 5.0,
            "Subtotal (" + subtotal + ") must equal sum of row totals (" + rowSum + ")");

        log.info("PASS TC_SUM_002 | rowSum=" + rowSum + " subtotal=" + subtotal);
    }

    // ── TC_SUM_003 — DATA-DRIVEN ────────────────────────────────────────────
    @Test(priority = 3,
          dataProvider = "orderSummaryDataset",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_SUM_003: [Data-Driven] Shipping = 0 when subtotal > 500, else 40")
    public void testShippingRuleDataDriven(String testId, String desc,
                                           String productId, int qty,
                                           double expectedShipping) {
        addProductWithQty(productId, qty);

        double subtotal       = cartPage.getSubtotal();
        double actualShipping = cartPage.getShipping();

        Assert.assertTrue(subtotal > 0, "[" + testId + "] Subtotal must be > 0");
        Assert.assertEquals(actualShipping, expectedShipping, 2.0,
            "[" + testId + "] " + desc
            + " | subtotal=" + subtotal
            + " expected=" + expectedShipping
            + " actual=" + actualShipping);

        log.info("PASS " + testId + " | qty=" + qty + " subtotal=" + subtotal
                 + " shipping=" + actualShipping);
    }

    // ── TC_SUM_004 — DATA-DRIVEN ────────────────────────────────────────────
    @Test(priority = 4,
          dataProvider = "orderSummaryDataset",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_SUM_004: [Data-Driven] Tax = Math.round(subtotal x 0.18)")
    public void testTaxCalculationDataDriven(String testId, String desc,
                                              String productId, int qty,
                                              double ignoredShipping) {
        addProductWithQty(productId, qty);

        double subtotal    = cartPage.getSubtotal();
        double actualTax   = cartPage.getTax();
        double expectedTax = calcExpectedTax(subtotal);

        Assert.assertTrue(actualTax > 0, "[" + testId + "] Tax must be > 0");
        Assert.assertEquals(actualTax, expectedTax, 5.0,
            "[" + testId + "] " + desc
            + " | subtotal=" + subtotal
            + " expected_tax=" + expectedTax
            + " actual=" + actualTax);

        log.info("PASS " + testId + " | subtotal=" + subtotal
                 + " tax=" + actualTax + " expected=" + expectedTax);
    }

    // ── TC_SUM_005 ─────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_SUM_005: Free shipping nudge absent when subtotal > 500")
    public void testFreeShippingNudge() {
        addProductWithQty(PRODUCT_A, 1);  // ₹5525 > ₹500

        double subtotal = cartPage.getSubtotal();
        if (subtotal > 500) {
            Assert.assertFalse(cartPage.isFreeShippingMessageVisible(),
                "Nudge must NOT appear when subtotal > 500. subtotal=" + subtotal);
            log.info("PASS TC_SUM_005 | subtotal=" + subtotal + " > 500 → no nudge ✓");
        } else {
            Assert.assertTrue(cartPage.isFreeShippingMessageVisible(),
                "Nudge must appear when subtotal <= 500. subtotal=" + subtotal);
            log.info("PASS TC_SUM_005 | subtotal=" + subtotal + " <= 500 → nudge shown ✓");
        }
    }

    // =========================================================================
    // VERIFY TOTAL CALCULATION
    // =========================================================================

    // ── TC_TOT_001 — DATA-DRIVEN ────────────────────────────────────────────
    @Test(priority = 6,
          dataProvider = "totalCalculationDataset",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_TOT_001: [Data-Driven] Total = subtotal + shipping + tax")
    public void testTotalFormulaDataDriven(String testId, String desc,
                                           String productId, int qty) {
        addProductWithQty(productId, qty);

        double subtotal      = cartPage.getSubtotal();
        double shipping      = cartPage.getShipping();
        double tax           = cartPage.getTax();
        double total         = cartPage.getTotal();
        double expectedTotal = subtotal + shipping + tax;

        Assert.assertTrue(total > 0, "[" + testId + "] Total must be > 0");
        Assert.assertEquals(total, expectedTotal, 5.0,
            "[" + testId + "] " + desc
            + " | " + subtotal + "+" + shipping + "+" + tax
            + "=" + expectedTotal + " actual=" + total);

        log.info("PASS " + testId + " | " + subtotal + "+" + shipping
                 + "+" + tax + "=" + expectedTotal + " UI=" + total);
    }

    // ── TC_TOT_002 ─────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_TOT_002: Total recalculates correctly after quantity change")
    public void testTotalRecalculatesAfterQtyChange() {
        addProductWithQty(PRODUCT_A, 1);

        double totalAtQty1 = cartPage.getTotal();
        Assert.assertTrue(totalAtQty1 > 0, "Total at qty=1 must be > 0");

        cartPage.increaseQuantity(0);
        waitForQty(2);

        // Wait for total to update
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> cartPage.getTotal() != totalAtQty1);
        } catch (Exception ignored) {}

        double subtotal2     = cartPage.getSubtotal();
        double shipping2     = cartPage.getShipping();
        double tax2          = cartPage.getTax();
        double totalAtQty2   = cartPage.getTotal();
        double expectedTotal = subtotal2 + shipping2 + tax2;

        Assert.assertTrue(totalAtQty2 > totalAtQty1,
            "Total must increase after qty increase. qty1=" + totalAtQty1 + " qty2=" + totalAtQty2);
        Assert.assertEquals(totalAtQty2, expectedTotal, 5.0,
            "Recalculated total must equal subtotal+shipping+tax. expected="
            + expectedTotal + " actual=" + totalAtQty2);

        log.info("PASS TC_TOT_002 | qty1 total=" + totalAtQty1
                 + " qty2 total=" + totalAtQty2);
    }

    // ── TC_TOT_003 — DATA-DRIVEN ────────────────────────────────────────────
    @Test(priority = 8,
          dataProvider = "shippingThresholdDataset",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_TOT_003: [Data-Driven] Summary consistent at shipping threshold")
    public void testSummaryAtThreshold(String testId, String desc,
                                       String productId, int qty,
                                       boolean expectAbove500) {
        addProductWithQty(productId, qty);

        double subtotal  = cartPage.getSubtotal();
        double shipping  = cartPage.getShipping();
        double tax       = cartPage.getTax();
        double total     = cartPage.getTotal();

        boolean actuallyAbove = subtotal > 500;
        Assert.assertEquals(actuallyAbove, expectAbove500,
            "[" + testId + "] subtotal=" + subtotal
            + " expectAbove500=" + expectAbove500);

        double expectedTotal = subtotal + shipping + tax;
        Assert.assertEquals(total, expectedTotal, 5.0,
            "[" + testId + "] " + desc + " total mismatch. expected="
            + expectedTotal + " actual=" + total);

        log.info("PASS " + testId + " | " + desc
                 + " | subtotal=" + subtotal + " shipping=" + shipping
                 + " tax=" + tax + " total=" + total);
    }

    // =========================================================================
    // CSV-DRIVEN TESTS
    // =========================================================================

    // ── TC_CSV_001 ─────────────────────────────────────────────────────────
    @Test(priority = 9,
          dataProvider = "csvOrderSummaryData",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_CSV_001: [CSV] Shipping rule correct for all CSV datasets")
    @SuppressWarnings("unchecked")
    public void testShippingFromCSV(Object rowObj) {
        java.util.Map<String, String> row =
            (java.util.Map<String, String>) rowObj;
        String testId           = row.get("testCaseId");
        String desc             = row.get("description");
        String productId        = row.get("productId");
        int    qty              = Integer.parseInt(row.get("quantity"));
        double expectedShipping = Double.parseDouble(row.get("expectedShipping"));

        addProductWithQty(productId, qty);

        double subtotal       = cartPage.getSubtotal();
        double actualShipping = cartPage.getShipping();

        Assert.assertEquals(actualShipping, expectedShipping, 2.0,
            "[CSV:" + testId + "] " + desc
            + " | subtotal=" + subtotal
            + " expected=" + expectedShipping
            + " actual=" + actualShipping);

        log.info("PASS CSV:" + testId + " | " + desc
                 + " | shipping=" + actualShipping);
    }

    // ── TC_CSV_002 ─────────────────────────────────────────────────────────
    @Test(priority = 10,
          dataProvider = "csvOrderSummaryData",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_CSV_002: [CSV] Tax = 18% of subtotal for all datasets")
    @SuppressWarnings("unchecked")
    public void testTaxFromCSV(Object rowObj) {
        java.util.Map<String, String> row =
            (java.util.Map<String, String>) rowObj;
        String testId     = row.get("testCaseId");
        String desc       = row.get("description");
        String productId  = row.get("productId");
        int    qty        = Integer.parseInt(row.get("quantity"));
        double taxRate    = Double.parseDouble(row.get("expectedTaxRate"));

        addProductWithQty(productId, qty);

        double subtotal    = cartPage.getSubtotal();
        double actualTax   = cartPage.getTax();
        double expectedTax = Math.round(subtotal * taxRate);

        Assert.assertEquals(actualTax, expectedTax, 5.0,
            "[CSV:" + testId + "] " + desc
            + " | subtotal=" + subtotal
            + " expected_tax=" + expectedTax
            + " actual=" + actualTax);

        log.info("PASS CSV:" + testId + " | tax=" + actualTax
                 + " expected=" + expectedTax
                 + " (" + (taxRate * 100) + "% of " + subtotal + ")");
    }

    // ── TC_CSV_003 ─────────────────────────────────────────────────────────
    @Test(priority = 11,
          dataProvider = "csvAboveThresholdData",
          dataProviderClass = CartSummaryDataProvider.class,
          description = "TC_CSV_003: [CSV-Filtered] above_threshold rows → FREE shipping")
    @SuppressWarnings("unchecked")
    public void testAboveThresholdFreeShipping(Object rowObj) {
        java.util.Map<String, String> row =
            (java.util.Map<String, String>) rowObj;
        String testId    = row.get("testCaseId");
        String productId = row.get("productId");
        int    qty       = Integer.parseInt(row.get("quantity"));

        addProductWithQty(productId, qty);

        double subtotal = cartPage.getSubtotal();
        double shipping = cartPage.getShipping();

        Assert.assertTrue(subtotal > 500,
            "[CSV:" + testId + "] subtotal must be > 500. Got=" + subtotal);
        Assert.assertEquals(shipping, 0.0, 1.0,
            "[CSV:" + testId + "] FREE shipping expected. Got=" + shipping);

        log.info("PASS CSV:" + testId
                 + " | subtotal=" + subtotal + " → FREE shipping ✓");
    }
}