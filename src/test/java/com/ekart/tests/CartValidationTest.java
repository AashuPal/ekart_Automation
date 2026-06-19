package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.CartPage;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.DynamicElementHandler;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

/**
 * Cart Item Validation & Quantity/Price Verification Tests
 *
 * ── Validate Cart Items ───────────────────────────────────────────
 *  TC_CIV_001  Cart page loads with correct heading
 *  TC_CIV_002  Each cart item has a visible name
 *  TC_CIV_003  Each cart item has a visible unit price > 0
 *  TC_CIV_004  Each cart item has a visible row total > 0
 *  TC_CIV_005  Cart item image is visible
 *  TC_CIV_006  Cart item has a clickable product link
 *  TC_CIV_007  Order summary section is visible with all fields
 *  TC_CIV_008  Heading item count badge matches actual row count
 *  TC_CIV_009  localStorage item count matches visible rows
 *  TC_CIV_010  Removing an item updates the heading badge count
 *
 * ── Verify Quantity ───────────────────────────────────────────────
 *  TC_QTY_001  Default quantity is 1 on first add
 *  TC_QTY_002  Plus button increments quantity by 1 each click
 *  TC_QTY_003  Minus button decrements quantity by 1 each click
 *  TC_QTY_004  Quantity cannot go below 1 (minus disabled at min)
 *  TC_QTY_005  Quantity persists after page refresh
 *  TC_QTY_006  Adding same product again increments qty, not rows
 *  TC_QTY_007  Each item maintains independent quantity
 *
 * ── Verify Price ──────────────────────────────────────────────────
 *  TC_PRC_001  Unit price on cart matches detail page price
 *  TC_PRC_002  Row total = unit price × quantity (qty=1)
 *  TC_PRC_003  Row total updates correctly after qty increase
 *  TC_PRC_004  Row total updates correctly after qty decrease
 *  TC_PRC_005  Subtotal = sum of all row totals
 *  TC_PRC_006  Tax displayed is > 0 for non-zero subtotal
 *  TC_PRC_007  Total = subtotal + shipping + tax
 *  TC_PRC_008  Prices use consistent currency symbol (₹)
 *  TC_PRC_009  Price values are valid numbers (no NaN / null)
 *  TC_PRC_010  All prices remain correct after removing one item
 */
public class CartValidationTest extends BaseTest {

    private static final String KNOWN_PRODUCT_ID =
        "656a732e-9e66-496b-83c0-8991a7a987c0";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;
    private CartPage           cartPage;

    /**
     * @BeforeClass — runs ONCE before all cart tests.
     * Login once here so every test starts already authenticated.
     * Overrides BaseTest.setUp() chain: setUp() inits driver, then loginOnce() logs in.
     */
    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));

        // Login ONCE before all tests in this class
        loginAsValidUser();
        log.info("Logged in once for CartValidationTest suite");
    }

    /**
     * @BeforeMethod — runs before EACH test.
     * Only re-initialises page objects and clears cart.
     * Does NOT login again — session from loginOnce() is reused.
     */
    @BeforeMethod
    public void setup() {
        listingPage = new ProductListingPage(getDriver());
        detailPage  = new ProductDetailPage(getDriver());
        cartPage    = new CartPage(getDriver());

        // If session expired mid-suite, re-login silently
        ensureLoggedIn();

        // Clear cart for clean state before each test
        try {
            getDriver().get(baseUrl);
            WaitUtils.waitForPageReady(getDriver());
            cartPage.clearCartViaLocalStorage();
            log.info("Cart cleared for next test");
        } catch (Exception e) {
            log.warn("Cart clear skipped: " + e.getMessage());
        }
    }

    /**
     * Re-login only if the session has expired (URL contains /login).
     * Called in @BeforeMethod to silently recover from session timeouts.
     */
    private void ensureLoggedIn() {
        try {
            String url = getDriver().getCurrentUrl();
            if (url.contains("/login") || url.contains("/register")) {
                log.warn("Session expired — re-logging in");
                loginAsValidUser();
            }
        } catch (Exception e) {
            log.warn("ensureLoggedIn check failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Add known product and navigate to cart. Returns unit price from detail page. */
    private double addKnownProductAndGoToCart() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        String raw = detailPage.getSellingPrice();
        detailPage.clickAddToCart();
        cartPage.navigateTo(baseUrl);
        return parsePrice(raw);
    }

    /** Add two different products from listing and navigate to cart. */
    private void addTwoProductsAndGoToCart() {
        listingPage.navigateTo(baseUrl);
        List<org.openqa.selenium.WebElement> cards = listingPage.getAllProductCards();
        Assert.assertTrue(cards.size() >= 2, "Need at least 2 products on listing");
        String id1 = listingPage.getCardProductId(cards.get(0));
        String id2 = listingPage.getCardProductId(cards.get(1));

        listingPage.navigateToProduct(baseUrl, id1);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        listingPage.navigateToProduct(baseUrl, id2);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);
    }

    private double parsePrice(String raw) {
        try {
            return Double.parseDouble(
                raw.replace("₹", "").replace("Rs.", "").replace("Rs", "")
                   .replace("FREE", "0").replaceAll("\\s+", "").replace(",", "").trim());
        } catch (NumberFormatException e) { return 0; }
    }

    private void waitForQtyToBe(int expected, int row) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> { try { return cartPage.getItemQuantity(row) == expected; }
                              catch (Exception ex) { return false; } });
        } catch (Exception ignored) {}
    }

    private void waitForRowTotalToChange(double prev, int row) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> { try { return cartPage.getItemRowTotal(row) != prev; }
                              catch (Exception ex) { return false; } });
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // VALIDATE CART ITEMS
    // =========================================================================

    // ── TC_CIV_001 ────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_CIV_001: Cart page loads with 'Shopping Cart' heading")
    public void testCartPageHeadingVisible() {
        addKnownProductAndGoToCart();

        By heading = By.xpath("//h1[contains(.,'Shopping Cart')]");
        Assert.assertTrue(
            DynamicElementHandler.appearsWithin(getDriver(), heading, 10),
            "'Shopping Cart' heading must be visible on cart page");

        log.info("PASS TC_CIV_001 | Cart heading visible");
    }

    // ── TC_CIV_002 ────────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_CIV_002: Each cart item has a visible non-empty name")
    public void testCartItemNamesVisible() {
        addTwoProductsAndGoToCart();

        List<String> names = cartPage.getCartItemNames();
        Assert.assertTrue(names.size() >= 1,
            "Cart must have at least 1 item with a name");

        for (int i = 0; i < names.size(); i++) {
            Assert.assertFalse(names.get(i).isEmpty(),
                "Item[" + i + "] name must not be empty");
            Assert.assertFalse(names.get(i).equalsIgnoreCase("null"),
                "Item[" + i + "] name must not be 'null'");
            Assert.assertTrue(names.get(i).length() >= 2,
                "Item[" + i + "] name too short: '" + names.get(i) + "'");
        }

        log.info("PASS TC_CIV_002 | Item names: " + names);
    }

    // ── TC_CIV_003 ────────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_CIV_003: Each cart item has a visible unit price > 0")
    public void testCartItemUnitPricesVisible() {
        addTwoProductsAndGoToCart();

        int count = cartPage.getCartItemCount();
        Assert.assertTrue(count >= 1, "Cart must have items");

        for (int i = 0; i < count; i++) {
            double price = cartPage.getItemUnitPrice(i);
            Assert.assertTrue(price > 0,
                "Item[" + i + "] unit price must be > 0. Got=" + price);
        }

        log.info("PASS TC_CIV_003 | Unit prices verified for " + count + " items");
    }

    // ── TC_CIV_004 ────────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_CIV_004: Each cart item has a visible row total > 0")
    public void testCartItemRowTotalsVisible() {
        addTwoProductsAndGoToCart();

        int count = cartPage.getCartItemCount();
        for (int i = 0; i < count; i++) {
            double rowTotal = cartPage.getItemRowTotal(i);
            Assert.assertTrue(rowTotal > 0,
                "Item[" + i + "] row total must be > 0. Got=" + rowTotal);
        }

        log.info("PASS TC_CIV_004 | Row totals verified for " + count + " items");
    }

    // ── TC_CIV_005 ────────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_CIV_005: Cart item product image is visible")
    public void testCartItemImageVisible() {
        addKnownProductAndGoToCart();

        List<org.openqa.selenium.WebElement> rows = cartPage.getCartRows();
        Assert.assertFalse(rows.isEmpty(), "Cart must have rows");

        org.openqa.selenium.WebElement img = null;
        try {
            img = rows.get(0).findElement(By.cssSelector("img"));
        } catch (Exception e) {
            Assert.fail("Cart item must have an <img> element");
        }

        Assert.assertTrue(img.isDisplayed(), "Cart item image must be displayed");
        String src = img.getAttribute("src");
        Assert.assertNotNull(src, "Image src must not be null");
        Assert.assertFalse(src.isEmpty(), "Image src must not be empty");

        log.info("PASS TC_CIV_005 | Cart item image visible | src=" + src.substring(0, Math.min(60, src.length())));
    }

    // ── TC_CIV_006 ────────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_CIV_006: Cart item name is a clickable product link")
    public void testCartItemIsClickableLink() {
        addKnownProductAndGoToCart();

        List<org.openqa.selenium.WebElement> rows = cartPage.getCartRows();
        Assert.assertFalse(rows.isEmpty(), "Cart must have rows");

        // Item name should be inside an <a> or have a link
        By itemLink = By.cssSelector("a.font-semibold.text-gray-900");
        org.openqa.selenium.WebElement link = rows.get(0).findElement(itemLink);
        Assert.assertNotNull(link, "Cart item must have a product link");

        String href = link.getAttribute("href");
        Assert.assertNotNull(href, "Product link href must not be null");
        Assert.assertTrue(href.contains("/product/"),
            "Product link must point to /product/<id>. href=" + href);

        log.info("PASS TC_CIV_006 | Cart item link → " + href);
    }

    // ── TC_CIV_007 ────────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_CIV_007: Order summary section visible with subtotal, tax, total")
    public void testOrderSummaryVisible() {
        addKnownProductAndGoToCart();

        Assert.assertTrue(cartPage.isOrderSummaryVisible(),
            "Order Summary heading must be visible");

        double subtotal = cartPage.getSubtotal();
        double tax      = cartPage.getTax();
        double total    = cartPage.getTotal();

        Assert.assertTrue(subtotal > 0,
            "Subtotal must be > 0. Got=" + subtotal);
        Assert.assertTrue(tax >= 0,
            "Tax must be >= 0. Got=" + tax);
        Assert.assertTrue(total > 0,
            "Total must be > 0. Got=" + total);

        log.info("PASS TC_CIV_007 | subtotal=" + subtotal + " tax=" + tax + " total=" + total);
    }

    // ── TC_CIV_008 ────────────────────────────────────────────────────────────
    @Test(priority = 8,
          description = "TC_CIV_008: Heading badge count matches actual visible row count")
    public void testHeadingBadgeMatchesRows() {
        addTwoProductsAndGoToCart();

        int headingCount = cartPage.getHeadingItemCount();
        int rowCount     = cartPage.getCartItemCount();

        Assert.assertTrue(headingCount > 0,
            "Heading badge must show > 0. Got=" + headingCount);
        Assert.assertEquals(headingCount, rowCount,
            "Heading badge (" + headingCount + ") must match row count (" + rowCount + ")");

        log.info("PASS TC_CIV_008 | badge=" + headingCount + " rows=" + rowCount);
    }

    // ── TC_CIV_009 ────────────────────────────────────────────────────────────
    @Test(priority = 9,
          description = "TC_CIV_009: localStorage item count matches visible cart rows")
    public void testLocalStorageCountMatchesRows() {
        addTwoProductsAndGoToCart();

        int lsCount  = cartPage.getLocalStorageCartCount();
        int rowCount = cartPage.getCartItemCount();

        Assert.assertTrue(lsCount > 0,
            "localStorage cart must have > 0 items. Got=" + lsCount);
        Assert.assertEquals(lsCount, rowCount,
            "localStorage count (" + lsCount + ") must match UI rows (" + rowCount + ")");

        log.info("PASS TC_CIV_009 | localStorage=" + lsCount + " rows=" + rowCount);
    }

    // ── TC_CIV_010 ────────────────────────────────────────────────────────────
    @Test(priority = 10,
          description = "TC_CIV_010: Removing an item updates heading badge count")
    public void testRemoveItemUpdatesBadge() {
        addTwoProductsAndGoToCart();

        int badgeBefore = cartPage.getHeadingItemCount();
        Assert.assertTrue(badgeBefore >= 2, "Need 2 items for this test");

        cartPage.removeItem(0);

        int badgeAfter = cartPage.getHeadingItemCount();
        Assert.assertEquals(badgeAfter, badgeBefore - 1,
            "Badge must decrease by 1 after remove. Before=" + badgeBefore + " After=" + badgeAfter);

        log.info("PASS TC_CIV_010 | Badge: " + badgeBefore + " → " + badgeAfter);
    }

    // =========================================================================
    // VERIFY QUANTITY
    // =========================================================================

    // ── TC_QTY_001 ────────────────────────────────────────────────────────────
    @Test(priority = 11,
          description = "TC_QTY_001: Default quantity is 1 when product first added")
    public void testDefaultQuantityIsOne() {
        addKnownProductAndGoToCart();

        int qty = cartPage.getItemQuantity(0);
        Assert.assertEquals(qty, 1,
            "Initial quantity must be 1. Got=" + qty);

        log.info("PASS TC_QTY_001 | Initial qty=" + qty);
    }

    // ── TC_QTY_002 ────────────────────────────────────────────────────────────
    @Test(priority = 12,
          description = "TC_QTY_002: Plus button increments quantity by 1 each click")
    public void testPlusButtonIncrementsQty() {
        addKnownProductAndGoToCart();

        // qty 1 → 2
        cartPage.increaseQuantity(0);
        waitForQtyToBe(2, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 2,
            "Qty must be 2 after 1st increment");

        // qty 2 → 3
        cartPage.increaseQuantity(0);
        waitForQtyToBe(3, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 3,
            "Qty must be 3 after 2nd increment");

        // qty 3 → 4
        cartPage.increaseQuantity(0);
        waitForQtyToBe(4, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 4,
            "Qty must be 4 after 3rd increment");

        log.info("PASS TC_QTY_002 | Qty incremented to 4");
    }

    // ── TC_QTY_003 ────────────────────────────────────────────────────────────
    @Test(priority = 13,
          description = "TC_QTY_003: Minus button decrements quantity by 1 each click")
    public void testMinusButtonDecrementsQty() {
        addKnownProductAndGoToCart();

        // Increase to 4 first
        for (int i = 0; i < 3; i++) cartPage.increaseQuantity(0);
        waitForQtyToBe(4, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 4, "Setup: qty should be 4");

        // 4 → 3
        cartPage.decreaseQuantity(0);
        waitForQtyToBe(3, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 3, "Qty must be 3 after 1st decrement");

        // 3 → 2
        cartPage.decreaseQuantity(0);
        waitForQtyToBe(2, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 2, "Qty must be 2 after 2nd decrement");

        // 2 → 1
        cartPage.decreaseQuantity(0);
        waitForQtyToBe(1, 0);
        Assert.assertEquals(cartPage.getItemQuantity(0), 1, "Qty must be 1 after 3rd decrement");

        log.info("PASS TC_QTY_003 | Qty decremented: 4→3→2→1");
    }

    // ── TC_QTY_004 ────────────────────────────────────────────────────────────
    @Test(priority = 14,
          description = "TC_QTY_004: Quantity cannot go below 1 (minus disabled at minimum)")
    public void testQtyCannotGoBelowOne() {
        addKnownProductAndGoToCart();

        Assert.assertEquals(cartPage.getItemQuantity(0), 1, "Starting qty must be 1");

        // Click minus at minimum
        cartPage.decreaseQuantity(0);

        // Wait briefly for any DOM change
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int qtyAfter = cartPage.getItemQuantity(0);
        Assert.assertEquals(qtyAfter, 1,
            "Qty must remain 1 when minus clicked at minimum. Got=" + qtyAfter);

        // Item must still be in cart (not removed)
        Assert.assertFalse(cartPage.isCartEmpty(),
            "Cart must not become empty when minus clicked at qty=1");

        log.info("PASS TC_QTY_004 | Qty stays 1, cart not empty");
    }

    // ── TC_QTY_005 ────────────────────────────────────────────────────────────
    @Test(priority = 15,
          description = "TC_QTY_005: Quantity persists after page refresh")
    public void testQtyPersistsAfterRefresh() {
        addKnownProductAndGoToCart();

        cartPage.increaseQuantity(0);
        cartPage.increaseQuantity(0);
        waitForQtyToBe(3, 0);

        int qtyBefore = cartPage.getItemQuantity(0);
        Assert.assertEquals(qtyBefore, 3, "Qty must be 3 before refresh");

        getDriver().navigate().refresh();
        WaitUtils.waitForPageReady(getDriver());
        cartPage.waitForCartLoad();

        int qtyAfter = cartPage.getItemQuantity(0);
        Assert.assertEquals(qtyAfter, qtyBefore,
            "Qty must persist after refresh. Before=" + qtyBefore + " After=" + qtyAfter);

        log.info("PASS TC_QTY_005 | Qty " + qtyBefore + " persisted after refresh");
    }

    // ── TC_QTY_006 ────────────────────────────────────────────────────────────
    @Test(priority = 16,
          description = "TC_QTY_006: Adding same product again increases qty, not row count")
    public void testAddSameProductIncreasesQtyNotRows() {
        // Add once
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        // Add again
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        int rows = cartPage.getCartItemCount();
        int qty  = cartPage.getItemQuantity(0);

        Assert.assertEquals(rows, 1,
            "Same product added twice must NOT create 2 rows. Got=" + rows);
        Assert.assertEquals(qty, 2,
            "Qty must be 2 after adding same product twice. Got=" + qty);

        log.info("PASS TC_QTY_006 | rows=1 qty=2 (no duplicate row)");
    }

    // ── TC_QTY_007 ────────────────────────────────────────────────────────────
    @Test(priority = 17,
          description = "TC_QTY_007: Two different items maintain independent quantities")
    public void testIndependentQuantitiesPerItem() {
        addTwoProductsAndGoToCart();

        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Need 2 items");

        // Increase only item 0's qty to 3
        cartPage.increaseQuantity(0);
        cartPage.increaseQuantity(0);
        waitForQtyToBe(3, 0);

        int qty0 = cartPage.getItemQuantity(0);
        int qty1 = cartPage.getItemQuantity(1);

        Assert.assertEquals(qty0, 3,
            "Item[0] qty must be 3 after 2 increments. Got=" + qty0);
        Assert.assertEquals(qty1, 1,
            "Item[1] qty must remain 1 (unchanged). Got=" + qty1);

        log.info("PASS TC_QTY_007 | Item[0] qty=" + qty0 + " Item[1] qty=" + qty1 + " (independent)");
    }

    // =========================================================================
    // VERIFY PRICE
    // =========================================================================

    // ── TC_PRC_001 ────────────────────────────────────────────────────────────
    @Test(priority = 18,
          description = "TC_PRC_001: Unit price in cart matches selling price on detail page")
    public void testUnitPriceMatchesDetailPage() {
        double detailPrice = addKnownProductAndGoToCart();

        double cartPrice = cartPage.getItemUnitPrice(0);

        Assert.assertTrue(cartPrice > 0, "Cart unit price must be > 0. Got=" + cartPrice);
        Assert.assertEquals(cartPrice, detailPrice, 2.0,
            "Cart unit price (" + cartPrice + ") must match detail page price (" + detailPrice + ")");

        log.info("PASS TC_PRC_001 | detailPrice=" + detailPrice + " cartPrice=" + cartPrice);
    }

    // ── TC_PRC_002 ────────────────────────────────────────────────────────────
    @Test(priority = 19,
          description = "TC_PRC_002: Row total = unit price × quantity when qty=1")
    public void testRowTotalEqualsUnitPriceAtQtyOne() {
        addKnownProductAndGoToCart();

        int    qty       = cartPage.getItemQuantity(0);
        double unitPrice = cartPage.getItemUnitPrice(0);
        double rowTotal  = cartPage.getItemRowTotal(0);

        Assert.assertEquals(qty, 1, "Qty must be 1 for this test");
        Assert.assertEquals(rowTotal, unitPrice * qty, 2.0,
            "Row total (" + rowTotal + ") must equal unitPrice×qty ("
            + unitPrice + "×" + qty + "=" + (unitPrice * qty) + ")");

        log.info("PASS TC_PRC_002 | " + unitPrice + " × " + qty + " = " + rowTotal);
    }

    // ── TC_PRC_003 ────────────────────────────────────────────────────────────
    @Test(priority = 20,
          description = "TC_PRC_003: Row total updates correctly after quantity increase")
    public void testRowTotalUpdatesAfterIncrease() {
        addKnownProductAndGoToCart();

        double unitPrice    = cartPage.getItemUnitPrice(0);
        double rowTotalBefore = cartPage.getItemRowTotal(0);

        cartPage.increaseQuantity(0);
        waitForRowTotalToChange(rowTotalBefore, 0);
        waitForQtyToBe(2, 0);

        int    newQty   = cartPage.getItemQuantity(0);
        double newTotal = cartPage.getItemRowTotal(0);
        double expected = unitPrice * newQty;

        Assert.assertEquals(newQty, 2, "Qty must be 2 after increment");
        Assert.assertEquals(newTotal, expected, 5.0,
            "Row total after increase: expected=" + expected + " got=" + newTotal
            + " (unitPrice=" + unitPrice + " qty=" + newQty + ")");

        log.info("PASS TC_PRC_003 | " + unitPrice + "×" + newQty + "=" + expected + " UI=" + newTotal);
    }

    // ── TC_PRC_004 ────────────────────────────────────────────────────────────
    @Test(priority = 21,
          description = "TC_PRC_004: Row total updates correctly after quantity decrease")
    public void testRowTotalUpdatesAfterDecrease() {
        addKnownProductAndGoToCart();

        // Increase to 3 first
        cartPage.increaseQuantity(0);
        cartPage.increaseQuantity(0);
        waitForQtyToBe(3, 0);

        double unitPrice = cartPage.getItemUnitPrice(0);
        double totalAt3  = cartPage.getItemRowTotal(0);

        // Decrease to 2
        cartPage.decreaseQuantity(0);
        waitForRowTotalToChange(totalAt3, 0);
        waitForQtyToBe(2, 0);

        int    newQty   = cartPage.getItemQuantity(0);
        double newTotal = cartPage.getItemRowTotal(0);
        double expected = unitPrice * newQty;

        Assert.assertEquals(newQty, 2, "Qty must be 2 after decrement");
        Assert.assertEquals(newTotal, expected, 5.0,
            "Row total after decrease: expected=" + expected + " got=" + newTotal
            + " (unitPrice=" + unitPrice + " qty=" + newQty + ")");

        log.info("PASS TC_PRC_004 | " + unitPrice + "×" + newQty + "=" + expected + " UI=" + newTotal);
    }

    // ── TC_PRC_005 ────────────────────────────────────────────────────────────
    @Test(priority = 22,
          description = "TC_PRC_005: Order summary subtotal equals sum of all row totals")
    public void testSubtotalEqualsSumOfRowTotals() {
        addTwoProductsAndGoToCart();

        int count = cartPage.getCartItemCount();
        Assert.assertTrue(count >= 1, "Cart must have items");

        double rowSum = 0;
        for (int i = 0; i < count; i++) {
            double rt = cartPage.getItemRowTotal(i);
            Assert.assertTrue(rt > 0, "Row[" + i + "] total must be > 0. Got=" + rt);
            rowSum += rt;
        }

        double subtotal = cartPage.getSubtotal();
        Assert.assertTrue(subtotal > 0, "Subtotal must be > 0. Got=" + subtotal);
        Assert.assertEquals(subtotal, rowSum, 5.0,
            "Subtotal (" + subtotal + ") must equal sum of row totals (" + rowSum + ")");

        log.info("PASS TC_PRC_005 | rowSum=" + rowSum + " subtotal=" + subtotal);
    }

    // ── TC_PRC_006 ────────────────────────────────────────────────────────────
    @Test(priority = 23,
          description = "TC_PRC_006: Tax is > 0 when subtotal is non-zero")
    public void testTaxIsPositive() {
        addKnownProductAndGoToCart();

        double subtotal = cartPage.getSubtotal();
        double tax      = cartPage.getTax();

        Assert.assertTrue(subtotal > 0, "Subtotal must be > 0");
        Assert.assertTrue(tax > 0,
            "Tax must be > 0 when subtotal=" + subtotal + ". Got=" + tax);

        // Verify tax is reasonable (between 1% and 30% of subtotal)
        double taxPercent = (tax / subtotal) * 100;
        Assert.assertTrue(taxPercent >= 1 && taxPercent <= 30,
            "Tax % must be between 1–30. Got=" + taxPercent + "% (tax=" + tax + " subtotal=" + subtotal + ")");

        log.info("PASS TC_PRC_006 | subtotal=" + subtotal + " tax=" + tax
                 + " (" + String.format("%.1f", taxPercent) + "%)");
    }

    // ── TC_PRC_007 ────────────────────────────────────────────────────────────
    @Test(priority = 24,
          description = "TC_PRC_007: Total = subtotal + shipping + tax")
    public void testTotalEqualsSubtotalPlusShippingPlusTax() {
        addKnownProductAndGoToCart();

        double subtotal = cartPage.getSubtotal();
        double shipping = cartPage.getShipping();
        double tax      = cartPage.getTax();
        double total    = cartPage.getTotal();
        double expected = subtotal + shipping + tax;

        Assert.assertTrue(total > 0, "Total must be > 0. Got=" + total);
        Assert.assertEquals(total, expected, 5.0,
            "Total (" + total + ") must equal subtotal+shipping+tax ("
            + subtotal + "+" + shipping + "+" + tax + "=" + expected + ")");

        log.info("PASS TC_PRC_007 | " + subtotal + "+" + shipping + "+" + tax + "=" + expected
                 + " UI=" + total);
    }

    // ── TC_PRC_008 ────────────────────────────────────────────────────────────
    @Test(priority = 25,
          description = "TC_PRC_008: All prices display with ₹ currency symbol")
    public void testPricesHaveCurrencySymbol() {
        addKnownProductAndGoToCart();

        List<org.openqa.selenium.WebElement> rows = cartPage.getCartRows();
        Assert.assertFalse(rows.isEmpty(), "Need at least one row");

        // Unit price element
        String unitPriceText = rows.get(0)
            .findElement(By.cssSelector("p.text-indigo-600.font-extrabold"))
            .getText();
        Assert.assertTrue(
            unitPriceText.contains("₹") || unitPriceText.contains("Rs"),
            "Unit price must show ₹ symbol. Got='" + unitPriceText + "'");

        log.info("PASS TC_PRC_008 | Currency symbol present in price: '" + unitPriceText + "'");
    }

    // ── TC_PRC_009 ────────────────────────────────────────────────────────────
    @Test(priority = 26,
          description = "TC_PRC_009: All price values are valid numbers (no NaN / 0 / null)")
    public void testAllPricesAreValidNumbers() {
        addTwoProductsAndGoToCart();

        int count = cartPage.getCartItemCount();

        for (int i = 0; i < count; i++) {
            double unit  = cartPage.getItemUnitPrice(i);
            double total = cartPage.getItemRowTotal(i);
            int    qty   = cartPage.getItemQuantity(i);

            Assert.assertFalse(Double.isNaN(unit),    "Item[" + i + "] unit price is NaN");
            Assert.assertFalse(Double.isNaN(total),   "Item[" + i + "] row total is NaN");
            Assert.assertTrue(unit > 0,  "Item[" + i + "] unit price must be > 0. Got=" + unit);
            Assert.assertTrue(total > 0, "Item[" + i + "] row total must be > 0. Got=" + total);
            Assert.assertTrue(qty > 0,   "Item[" + i + "] qty must be > 0. Got=" + qty);
        }

        double subtotal = cartPage.getSubtotal();
        double tax      = cartPage.getTax();
        double total    = cartPage.getTotal();

        Assert.assertFalse(Double.isNaN(subtotal), "Subtotal is NaN");
        Assert.assertFalse(Double.isNaN(tax),      "Tax is NaN");
        Assert.assertFalse(Double.isNaN(total),    "Total is NaN");
        Assert.assertTrue(subtotal > 0, "Subtotal must be > 0. Got=" + subtotal);
        Assert.assertTrue(total > 0,    "Total must be > 0. Got=" + total);

        log.info("PASS TC_PRC_009 | All prices valid | items=" + count
                 + " subtotal=" + subtotal + " tax=" + tax + " total=" + total);
    }

    // ── TC_PRC_010 ────────────────────────────────────────────────────────────
    @Test(priority = 27,
          description = "TC_PRC_010: All prices recalculate correctly after removing one item")
    public void testPricesRecalculateAfterRemove() {
        addTwoProductsAndGoToCart();

        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Need 2 items");

        // Capture state before removal
        double rowTotal1   = cartPage.getItemRowTotal(0);
        double rowTotal2   = cartPage.getItemRowTotal(1);
        double subtotalBefore = cartPage.getSubtotal();

        // Remove item 0
        cartPage.removeItem(0);

        // Wait for recalculation
        DynamicElementHandler.waitForCountToChangeFrom(
            getDriver(), By.cssSelector("a.font-semibold.text-gray-900"), 2);

        double subtotalAfter = cartPage.getSubtotal();
        double tax           = cartPage.getTax();
        double total         = cartPage.getTotal();
        double shipping      = cartPage.getShipping();

        // Subtotal after = row total of remaining item
        Assert.assertEquals(subtotalAfter, rowTotal2, 5.0,
            "After removing item[0], subtotal must equal row[1] total. "
            + "rowTotal2=" + rowTotal2 + " subtotalAfter=" + subtotalAfter);

        // Total must still be consistent
        double expectedTotal = subtotalAfter + shipping + tax;
        Assert.assertEquals(total, expectedTotal, 5.0,
            "Total must equal subtotal+shipping+tax after remove. "
            + subtotalAfter + "+" + shipping + "+" + tax + "=" + expectedTotal
            + " UI=" + total);

        log.info("PASS TC_PRC_010 | Before subtotal=" + subtotalBefore
                 + " removed row=" + rowTotal1
                 + " after subtotal=" + subtotalAfter + " total=" + total);
    }
}