package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.CartPage;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.DynamicElementHandler;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

/**
 * Add-to-Cart Automation & Product Addition Validation
 *
 * ── Add to Cart ───────────────────────────────────────────────────
 *  TC_CART_001  Add product from detail page → cart icon count updates
 *  TC_CART_002  Product appears in cart page after add
 *  TC_CART_003  Product name on cart matches detail page name
 *  TC_CART_004  Product price on cart matches detail page price
 *  TC_CART_005  Cart heading badge shows correct item count
 *  TC_CART_006  localStorage contains added product ID
 *  TC_CART_007  Adding same product twice increases quantity
 *  TC_CART_008  Add multiple different products — all appear in cart
 *
 * ── Validate Product Addition ─────────────────────────────────────
 *  TC_CART_009  Quantity starts at 1 after first add
 *  TC_CART_010  Increase quantity → row total updates (price × qty)
 *  TC_CART_011  Decrease quantity → row total updates correctly
 *  TC_CART_012  Quantity cannot go below 1 (minus button disabled)
 *  TC_CART_013  Remove item → item disappears from cart
 *  TC_CART_014  Remove last item → empty cart state is shown
 *  TC_CART_015  Order summary subtotal = sum of all row totals
 *  TC_CART_016  Tax = 18% of subtotal
 *  TC_CART_017  Shipping free when subtotal > ₹500
 *  TC_CART_018  Total = subtotal + shipping + tax
 *  TC_CART_019  Move to Wishlist removes item from cart
 *  TC_CART_020  Continue Shopping navigates back to home page
 */
public class AddToCartTest extends BaseTest {

    private static final String KNOWN_PRODUCT_ID =
        "656a732e-9e66-496b-83c0-8991a7a987c0";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;
    private CartPage           cartPage;

    /**
     * @BeforeClass — login ONCE before all AddToCart tests.
     * Runs after BaseTest.setUp() which initialises the driver.
     */
    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));
        loginAsValidUser();
        log.info("Logged in once for AddToCartTest suite");
    }

    /**
     * @BeforeMethod — before each test: reinit pages, recover session if expired,
     * navigate to home, clear cart. Does NOT login again.
     */
    @BeforeMethod
    public void initPages() {
        listingPage = new ProductListingPage(getDriver());
        detailPage  = new ProductDetailPage(getDriver());
        cartPage    = new CartPage(getDriver());

        // Silently re-login if session expired between tests
        try {
            String url = getDriver().getCurrentUrl();
            if (url.contains("/login") || url.contains("/register")) {
                log.warn("Session expired — re-logging in");
                loginAsValidUser();
            }
        } catch (Exception ignored) {}

        // Navigate to home and clear cart for clean state
        try {
            getDriver().get(baseUrl);
            WaitUtils.waitForPageReady(getDriver());
            cartPage.clearCartViaLocalStorage();
            log.info("Cart cleared before test");
        } catch (org.openqa.selenium.TimeoutException e) {
            log.warn("Page load timed out — retrying");
            getDriver().navigate().refresh();
            WaitUtils.waitForPageReady(getDriver());
            cartPage.clearCartViaLocalStorage();
        } catch (Exception e) {
            log.warn("Could not clear cart: " + e.getMessage());
        }
    }

    // =========================================================================
    // ADD TO CART
    // =========================================================================

    // ── TC_CART_001 ───────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_CART_001: Add product from detail page — cart icon count updates")
    public void testAddToCartUpdatesNavIcon() {
        int cartCountBefore = getNavCartCount();

        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        // Wait for navbar count to update
        DynamicElementHandler.waitForCountToChangeFrom(
            getDriver(),
            By.cssSelector("span[class*='absolute'][class*='rounded-full']"),
            cartCountBefore
        );

        int cartCountAfter = getNavCartCount();
        Assert.assertTrue(cartCountAfter > cartCountBefore,
            "Navbar cart count must increase after add. Before=" + cartCountBefore
            + " After=" + cartCountAfter);

        log.info("PASS TC_CART_001 | Cart count: " + cartCountBefore + " → " + cartCountAfter);
    }

    // ── TC_CART_002 ───────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_CART_002: Product appears on cart page after adding from detail page")
    public void testProductAppearsInCart() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        Assert.assertFalse(cartPage.isCartEmpty(),
            "Cart must not be empty after adding a product");
        Assert.assertTrue(cartPage.getCartItemCount() >= 1,
            "Cart must have at least 1 item. Found: " + cartPage.getCartItemCount());

        log.info("PASS TC_CART_002 | Items in cart: " + cartPage.getCartItemCount());
    }

    // ── TC_CART_003 ───────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_CART_003: Product name in cart matches name on detail page")
    public void testCartItemNameMatchesDetailPage() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String detailName = detailPage.getProductName().trim();
        Assert.assertFalse(detailName.isEmpty(), "Detail page name must not be empty");

        detailPage.clickAddToCart();
        cartPage.navigateTo(baseUrl);

        List<String> cartNames = cartPage.getCartItemNames();
        Assert.assertFalse(cartNames.isEmpty(), "Cart must contain at least one item");

        boolean nameMatch = cartNames.stream()
            .anyMatch(n -> n.toLowerCase().contains(detailName.toLowerCase()) ||
                           detailName.toLowerCase().contains(n.toLowerCase()));

        Assert.assertTrue(nameMatch,
            "Cart item name must match detail page name. " +
            "Detail='" + detailName + "' Cart=" + cartNames);

        log.info("PASS TC_CART_003 | Detail='" + detailName + "' Cart=" + cartNames);
    }

    // ── TC_CART_004 ───────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_CART_004: Product price in cart matches price on detail page")
    public void testCartItemPriceMatchesDetailPage() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String rawDetailPrice = detailPage.getSellingPrice();
        Assert.assertFalse(rawDetailPrice.isEmpty(), "Detail price must not be empty");
        double detailPrice = parsePrice(rawDetailPrice);

        detailPage.clickAddToCart();
        cartPage.navigateTo(baseUrl);

        double cartPrice = cartPage.getItemUnitPrice(0);
        Assert.assertTrue(cartPrice > 0,
            "Cart item unit price must be > 0. Got: " + cartPrice);
        Assert.assertEquals(cartPrice, detailPrice, 1.0,
            "Cart price (" + cartPrice + ") must match detail price (" + detailPrice + ")");

        log.info("PASS TC_CART_004 | DetailPrice=" + detailPrice + " CartPrice=" + cartPrice);
    }

    // ── TC_CART_005 ───────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_CART_005: Cart heading badge shows correct item count")
    public void testCartHeadingBadgeCount() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        int headingCount = cartPage.getHeadingItemCount();
        int actualRows   = cartPage.getCartItemCount();

        Assert.assertTrue(headingCount > 0,
            "Heading badge must show > 0 items");
        Assert.assertEquals(headingCount, actualRows,
            "Heading count (" + headingCount + ") must match actual rows (" + actualRows + ")");

        log.info("PASS TC_CART_005 | Heading count=" + headingCount + " Rows=" + actualRows);
    }

    // ── TC_CART_006 ───────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_CART_006: localStorage contains added product ID")
    public void testLocalStorageContainsProduct() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        // Navigate to cart page so localStorage is readable in same origin
        cartPage.navigateTo(baseUrl);

        Assert.assertTrue(cartPage.localStorageContainsProduct(KNOWN_PRODUCT_ID),
            "localStorage cart must contain product ID: " + KNOWN_PRODUCT_ID);

        int lsCount = cartPage.getLocalStorageCartCount();
        Assert.assertTrue(lsCount >= 1,
            "localStorage cart must have >= 1 item. Found: " + lsCount);

        log.info("PASS TC_CART_006 | localStorage cart count=" + lsCount);
    }

    // ── TC_CART_007 ───────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_CART_007: Adding same product twice increases quantity, not row count")
    public void testAddingSameProductTwiceIncreasesQuantity() {
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
            "Adding same product twice must NOT create duplicate rows. Rows=" + rows);
        Assert.assertEquals(qty, 2,
            "Quantity must be 2 after adding same product twice. Got=" + qty);

        log.info("PASS TC_CART_007 | rows=" + rows + " qty=" + qty);
    }

    // ── TC_CART_008 ───────────────────────────────────────────────────────────
    @Test(priority = 8,
          description = "TC_CART_008: Adding multiple different products — all appear in cart")
    public void testAddMultipleDifferentProducts() {
        listingPage.navigateTo(baseUrl);

        // Pick first 2 product IDs from listing
        java.util.List<org.openqa.selenium.WebElement> cards =
            listingPage.getAllProductCards();
        Assert.assertTrue(cards.size() >= 2, "Need at least 2 products");

        String id1 = listingPage.getCardProductId(cards.get(0));
        String id2 = listingPage.getCardProductId(cards.get(1));

        // Add product 1
        listingPage.navigateToProduct(baseUrl, id1);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        // Add product 2
        listingPage.navigateToProduct(baseUrl, id2);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        int rows = cartPage.getCartItemCount();
        Assert.assertEquals(rows, 2,
            "Cart must have 2 rows after adding 2 different products. Got=" + rows);

        // Verify both product IDs in localStorage
        Assert.assertTrue(cartPage.localStorageContainsProduct(id1),
            "localStorage must contain product 1: " + id1);
        Assert.assertTrue(cartPage.localStorageContainsProduct(id2),
            "localStorage must contain product 2: " + id2);

        log.info("PASS TC_CART_008 | 2 products in cart | id1=" + id1 + " id2=" + id2);
    }

    // =========================================================================
    // VALIDATE PRODUCT ADDITION
    // =========================================================================

    // ── TC_CART_009 ───────────────────────────────────────────────────────────
    @Test(priority = 9,
          description = "TC_CART_009: Quantity starts at 1 after first add")
    public void testInitialQuantityIsOne() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        int qty = cartPage.getItemQuantity(0);
        Assert.assertEquals(qty, 1,
            "Initial quantity after first add must be 1. Got=" + qty);

        log.info("PASS TC_CART_009 | Initial qty=" + qty);
    }

    // ── TC_CART_010 ───────────────────────────────────────────────────────────
    @Test(priority = 10,
          description = "TC_CART_010: Increasing quantity updates row total (price × qty)")
    public void testIncreaseQtyUpdatesRowTotal() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        double unitPrice   = cartPage.getItemUnitPrice(0);
        double totalBefore = cartPage.getItemRowTotal(0);

        cartPage.increaseQuantity(0);

        // Wait for DOM to update
        waitForRowTotalToChange(totalBefore);

        int    newQty       = cartPage.getItemQuantity(0);
        double newRowTotal  = cartPage.getItemRowTotal(0);
        double expectedTotal = unitPrice * newQty;

        Assert.assertEquals(newQty, 2,
            "Quantity should be 2 after increment. Got=" + newQty);
        Assert.assertEquals(newRowTotal, expectedTotal, 5.0,
            "Row total must equal unitPrice × qty. " +
            unitPrice + " × " + newQty + " = " + expectedTotal + " but got " + newRowTotal);

        log.info("PASS TC_CART_010 | unitPrice=" + unitPrice + " qty=" + newQty
                 + " rowTotal=" + newRowTotal);
    }

    // ── TC_CART_011 ───────────────────────────────────────────────────────────
    @Test(priority = 11,
          description = "TC_CART_011: Decreasing quantity updates row total correctly")
    public void testDecreaseQtyUpdatesRowTotal() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        // First increase to 3
        cartPage.increaseQuantity(0);
        cartPage.increaseQuantity(0);
        waitForQuantityToBe(2, 0);

        double unitPrice = cartPage.getItemUnitPrice(0);

        cartPage.decreaseQuantity(0);
        waitForQuantityToBe(2, 0);

        int    newQty      = cartPage.getItemQuantity(0);
        double newRowTotal = cartPage.getItemRowTotal(0);
        double expected    = unitPrice * newQty;

        Assert.assertEquals(newQty, 2,
            "Quantity should be 2 after increment×2 then decrement. Got=" + newQty);
        Assert.assertEquals(newRowTotal, expected, 5.0,
            "Row total must be price × qty. Expected=" + expected + " Got=" + newRowTotal);

        log.info("PASS TC_CART_011 | qty=" + newQty + " rowTotal=" + newRowTotal);
    }

    // ── TC_CART_012 ───────────────────────────────────────────────────────────
    @Test(priority = 12,
          description = "TC_CART_012: Quantity cannot go below 1 (minus disabled at qty=1)")
    public void testQuantityCannotGoBelowOne() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        Assert.assertEquals(cartPage.getItemQuantity(0), 1,
            "Starting quantity must be 1");

        // Try to decrease below 1
        cartPage.decreaseQuantity(0);

        int qtyAfter = cartPage.getItemQuantity(0);
        Assert.assertEquals(qtyAfter, 1,
            "Quantity must remain 1 after clicking minus at minimum. Got=" + qtyAfter);

        log.info("PASS TC_CART_012 | Qty stays at 1 when minus clicked at minimum");
    }

    // ── TC_CART_013 ───────────────────────────────────────────────────────────
    @Test(priority = 13,
          description = "TC_CART_013: Remove item — item disappears from cart")
    public void testRemoveItemFromCart() {
        // Capture IDs BEFORE any navigation to avoid StaleElementReferenceException
        listingPage.navigateTo(baseUrl);
        java.util.List<org.openqa.selenium.WebElement> cards =
            listingPage.getAllProductCards();
        Assert.assertTrue(cards.size() >= 2, "Need 2 products");
        String id1 = listingPage.getCardProductId(cards.get(0));
        String id2 = listingPage.getCardProductId(cards.get(1));

        listingPage.navigateToProduct(baseUrl, id1);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        listingPage.navigateToProduct(baseUrl, id2);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);
        int rowsBefore = cartPage.getCartItemCount();
        String removedName = cartPage.getCartItemNames().get(0);

        cartPage.removeItem(0);

        int rowsAfter = cartPage.getCartItemCount();
        Assert.assertEquals(rowsAfter, rowsBefore - 1,
            "Row count must decrease by 1 after remove. Before=" + rowsBefore
            + " After=" + rowsAfter);
        Assert.assertFalse(cartPage.isProductInCart(removedName),
            "Removed product '" + removedName + "' must not appear in cart");

        log.info("PASS TC_CART_013 | Removed '" + removedName
                 + "' | rows " + rowsBefore + "→" + rowsAfter);
    }

    // ── TC_CART_014 ───────────────────────────────────────────────────────────
    @Test(priority = 14,
          description = "TC_CART_014: Removing last item shows empty cart state")
    public void testRemoveLastItemShowsEmptyState() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);
        Assert.assertEquals(cartPage.getCartItemCount(), 1,
            "Cart must have exactly 1 item for this test");

        cartPage.removeItem(0);

        boolean emptyState = DynamicElementHandler.appearsWithin(
            getDriver(),
            By.xpath("//h2[text()='Your cart is empty']"), 10);

        Assert.assertTrue(emptyState,
            "'Your cart is empty' heading must appear after removing last item");

        log.info("PASS TC_CART_014 | Empty state shown after last item removed");
    }

    // ── TC_CART_015 ───────────────────────────────────────────────────────────
    @Test(priority = 15,
          description = "TC_CART_015: Order summary subtotal = sum of all row totals")
    public void testSubtotalEqualsRowTotalsSum() {
        // Capture IDs BEFORE any navigation to avoid StaleElementReferenceException
        listingPage.navigateTo(baseUrl);
        java.util.List<org.openqa.selenium.WebElement> cards =
            listingPage.getAllProductCards();
        Assert.assertTrue(cards.size() >= 2, "Need 2 products");
        String id1 = listingPage.getCardProductId(cards.get(0));
        String id2 = listingPage.getCardProductId(cards.get(1));

        listingPage.navigateToProduct(baseUrl, id1);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        listingPage.navigateToProduct(baseUrl, id2);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        // Sum all row totals
        double rowSum = 0;
        for (int i = 0; i < cartPage.getCartItemCount(); i++) {
            rowSum += cartPage.getItemRowTotal(i);
        }

        double summarySubtotal = cartPage.getSubtotal();

        Assert.assertEquals(summarySubtotal, rowSum, 2.0,
            "Order summary subtotal (" + summarySubtotal + ") must equal " +
            "sum of row totals (" + rowSum + ")");

        log.info("PASS TC_CART_015 | rowSum=" + rowSum + " subtotal=" + summarySubtotal);
    }

    // ── TC_CART_016 ───────────────────────────────────────────────────────────
    @Test(priority = 16,
          description = "TC_CART_016: Tax = 18% of subtotal")
    public void testTaxIsEighteenPercent() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        double subtotal    = cartPage.getSubtotal();
        double tax         = cartPage.getTax();
        // Frontend: tax = Math.round(subtotal * 0.18)
        // toLocaleString('en-IN') formats with Indian commas — parsePrice handles this
        double expectedTax = Math.round(subtotal * 0.18);

        Assert.assertTrue(tax > 0,
            "Tax must be > 0. Got=" + tax);
        Assert.assertEquals(tax, expectedTax, 5.0,
            "Tax must be ~18% of subtotal. subtotal=" + subtotal
            + " expected=" + expectedTax + " got=" + tax);

        log.info("PASS TC_CART_016 | subtotal=" + subtotal + " tax=" + tax
                 + " (18%=" + expectedTax + ")");
    }

    // ── TC_CART_017 ───────────────────────────────────────────────────────────
    @Test(priority = 17,
          description = "TC_CART_017: Shipping is FREE when subtotal > ₹500")
    public void testFreeShippingAboveFiveHundred() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        double subtotal = cartPage.getSubtotal();

        double shipping = cartPage.getShipping();
        if (subtotal > 500) {
            // Frontend: shipping = subtotal > 500 ? 0 : 40 → shows "FREE" text
            Assert.assertEquals(shipping, 0.0,
                "Shipping must be FREE (0) when subtotal=" + subtotal + " > ₹500. Got=" + shipping);
            log.info("PASS TC_CART_017 | subtotal=" + subtotal + " → FREE shipping");
        } else {
            Assert.assertEquals(shipping, 40.0,
                "Shipping must be ₹40 when subtotal=" + subtotal + " <= ₹500. Got=" + shipping);
            log.info("PASS TC_CART_017 | subtotal=" + subtotal + " → ₹40 shipping");
        }
    }

    // ── TC_CART_018 ───────────────────────────────────────────────────────────
    @Test(priority = 18,
          description = "TC_CART_018: Total = subtotal + shipping + tax")
    public void testTotalEqualsSubtotalPlusShippingPlusTax() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);

        double subtotal = cartPage.getSubtotal();
        double shipping = cartPage.getShipping();
        double tax      = cartPage.getTax();
        double total    = cartPage.getTotal();
        double expected = subtotal + shipping + tax;

        Assert.assertEquals(total, expected, 5.0,
            "Total must equal subtotal + shipping + tax. " +
            subtotal + " + " + shipping + " + " + tax + " = " + expected
            + " but UI shows " + total);

        log.info("PASS TC_CART_018 | " + subtotal + " + " + shipping + " + " + tax
                 + " = " + expected + " | UI total=" + total);
    }

    // ── TC_CART_019 ───────────────────────────────────────────────────────────
    @Test(priority = 19,
          description = "TC_CART_019: Move to Wishlist removes item from cart")
    public void testMoveToWishlistRemovesFromCart() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);
        Assert.assertEquals(cartPage.getCartItemCount(), 1,
            "Cart must have 1 item before move-to-wishlist");

        String itemName = cartPage.getCartItemNames().get(0);
        cartPage.moveToWishlist(0);

        boolean emptyShown = DynamicElementHandler.appearsWithin(
            getDriver(),
            By.xpath("//h2[text()='Your cart is empty']"), 10);

        Assert.assertTrue(emptyShown || cartPage.getCartItemCount() == 0,
            "Cart must be empty after moving last item to wishlist");

        // Verify wishlist localStorage contains the item
        Object wishlist = ((JavascriptExecutor) getDriver())
            .executeScript("return localStorage.getItem('wishlist');");
        Assert.assertNotNull(wishlist,
            "Wishlist localStorage must not be null after move");

        log.info("PASS TC_CART_019 | '" + itemName + "' moved to wishlist");
    }

    // ── TC_CART_020 ───────────────────────────────────────────────────────────
    @Test(priority = 20,
          description = "TC_CART_020: Continue Shopping navigates back to home page")
    public void testContinueShoppingNavigatesHome() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();

        cartPage.navigateTo(baseUrl);
        Assert.assertTrue(cartPage.isOnCartPage(), "Must be on cart page");

        cartPage.clickContinueShopping();
        WaitUtils.waitForPageReady(getDriver());

        String urlAfter = getDriver().getCurrentUrl();
        Assert.assertFalse(urlAfter.contains("/cart"),
            "After Continue Shopping, URL must not be /cart. Got: " + urlAfter);

        listingPage.waitForPageLoad();
        Assert.assertTrue(listingPage.isProductGridVisible(),
            "Home page product grid must be visible after Continue Shopping");

        log.info("PASS TC_CART_020 | URL after continue shopping: " + urlAfter);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Read cart item count from navbar badge. */
    private int getNavCartCount() {
        try {
            By navBadge = By.cssSelector(
                "span[class*='absolute'][class*='rounded-full']," +
                "span[class*='bg-red'][class*='rounded-full']," +
                "span[class*='bg-orange'][class*='rounded-full']");
            java.util.List<org.openqa.selenium.WebElement> badges =
                getDriver().findElements(navBadge);
            if (badges.isEmpty()) return 0;
            String text = badges.get(0).getText().trim();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (Exception e) { return 0; }
    }

    /** Wait up to 5s for a row total to change from a previous value. */
    private void waitForRowTotalToChange(double previousTotal) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> {
                    try {
                        return cartPage.getItemRowTotal(0) != previousTotal;
                    } catch (Exception e) { return false; }
                });
        } catch (Exception ignored) {}
    }

    /** Wait for a specific qty value at a row index. */
    private void waitForQuantityToBe(int expected, int rowIndex) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> {
                    try {
                        return cartPage.getItemQuantity(rowIndex) == expected;
                    } catch (Exception e) { return false; }
                });
        } catch (Exception ignored) {}
    }

    /** Parse price string to double. */
    private double parsePrice(String raw) {
        try {
            return Double.parseDouble(raw.replaceAll("[₹Rs.,\\s]", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}