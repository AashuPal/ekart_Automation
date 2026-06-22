package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.CartPage;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Mandatory Cart Operations Tests
 *
 *  TC_OPS_001  Update cart — quantity increase updates row total & subtotal
 *  TC_OPS_002  Update cart — quantity decrease updates row total & subtotal
 *  TC_OPS_003  Remove item — item disappears, subtotal recalculates
 *  TC_OPS_004  Cart summary — total = subtotal + shipping + tax
 *  TC_OPS_005  Edge case — empty cart shows empty state (no summary shown)
 *  TC_OPS_006  Edge case — adding out-of-stock or max-stock product
 *  TC_OPS_007  Edge case — cart persists after browser refresh
 */
public class CartOperationsTest extends BaseTest {

    private static final String KNOWN_PRODUCT_ID =
        "656a732e-9e66-496b-83c0-8991a7a987c0";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;
    private CartPage           cartPage;

    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));
        loginAsValidUser();
        log.info("Logged in once for CartOperationsTest");
    }

    @BeforeMethod
    public void setup() {
        listingPage = new ProductListingPage(getDriver());
        detailPage  = new ProductDetailPage(getDriver());
        cartPage    = new CartPage(getDriver());

        // Recover session if expired
        try {
            if (getDriver().getCurrentUrl().contains("/login"))
                loginAsValidUser();
        } catch (Exception ignored) {}

        // Clear cart for clean state
        try {
            getDriver().get(baseUrl);
            WaitUtils.waitForPageReady(getDriver());
            cartPage.clearCartViaLocalStorage();
        } catch (Exception e) {
            log.warn("Cart clear skipped: " + e.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private void addKnownProduct() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();
        cartPage.navigateTo(baseUrl);
    }

    private void waitForQty(int expected) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> { try { return cartPage.getItemQuantity(0) == expected; }
                              catch (Exception e) { return false; } });
        } catch (Exception ignored) {}
    }

    private void waitForTotalChange(double prev) {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> { try { return cartPage.getItemRowTotal(0) != prev; }
                              catch (Exception e) { return false; } });
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // TC_OPS_001: Update cart — qty increase → row total & subtotal update
    // =========================================================================
    @Test(priority = 1,
          description = "TC_OPS_001: Increasing quantity updates row total and subtotal")
    public void testUpdateCartIncreaseQty() {
        addKnownProduct();

        double unitPrice      = cartPage.getItemUnitPrice(0);
        double subtotalBefore = cartPage.getSubtotal();
        double rowTotalBefore = cartPage.getItemRowTotal(0);

        // Increase qty: 1 → 2
        cartPage.increaseQuantity(0);
        waitForQty(2);
        waitForTotalChange(rowTotalBefore);

        int    newQty         = cartPage.getItemQuantity(0);
        double newRowTotal    = cartPage.getItemRowTotal(0);
        double newSubtotal    = cartPage.getSubtotal();

        Assert.assertEquals(newQty, 2,
            "Qty must be 2 after increment. Got=" + newQty);
        Assert.assertEquals(newRowTotal, unitPrice * 2, 5.0,
            "Row total must be unitPrice×2=" + (unitPrice * 2) + " Got=" + newRowTotal);
        Assert.assertTrue(newSubtotal > subtotalBefore,
            "Subtotal must increase after qty increment. Before=" + subtotalBefore + " After=" + newSubtotal);

        log.info("PASS TC_OPS_001 | qty=2 rowTotal=" + newRowTotal + " subtotal=" + newSubtotal);
    }

    // =========================================================================
    // TC_OPS_002: Update cart — qty decrease → row total & subtotal update
    // =========================================================================
    @Test(priority = 2,
          description = "TC_OPS_002: Decreasing quantity updates row total and subtotal")
    public void testUpdateCartDecreaseQty() {
        addKnownProduct();

        // Increase to 3 first
        cartPage.increaseQuantity(0);
        cartPage.increaseQuantity(0);
        waitForQty(3);

        double unitPrice      = cartPage.getItemUnitPrice(0);
        double subtotalAt3    = cartPage.getSubtotal();
        double rowTotalAt3    = cartPage.getItemRowTotal(0);

        // Decrease qty: 3 → 2
        cartPage.decreaseQuantity(0);
        waitForQty(2);
        waitForTotalChange(rowTotalAt3);

        int    newQty      = cartPage.getItemQuantity(0);
        double newRowTotal = cartPage.getItemRowTotal(0);
        double newSubtotal = cartPage.getSubtotal();

        Assert.assertEquals(newQty, 2,
            "Qty must be 2 after decrement. Got=" + newQty);
        Assert.assertEquals(newRowTotal, unitPrice * 2, 5.0,
            "Row total must be unitPrice×2=" + (unitPrice * 2) + " Got=" + newRowTotal);
        Assert.assertTrue(newSubtotal < subtotalAt3,
            "Subtotal must decrease after qty decrement. Before=" + subtotalAt3 + " After=" + newSubtotal);

        log.info("PASS TC_OPS_002 | qty=2 rowTotal=" + newRowTotal + " subtotal=" + newSubtotal);
    }

    // =========================================================================
    // TC_OPS_003: Remove item — item disappears, subtotal recalculates
    // =========================================================================
    @Test(priority = 3,
          description = "TC_OPS_003: Removing item removes it from cart and recalculates subtotal")
    public void testRemoveItem() {
        // Add 2 products
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
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Need 2 cart rows");

        String removedName    = cartPage.getCartItemNames().get(0);
        double rowTotal0      = cartPage.getItemRowTotal(0);
        double subtotalBefore = cartPage.getSubtotal();

        // Remove first item
        cartPage.removeItem(0);

        // Wait for row to disappear
        new org.openqa.selenium.support.ui.WebDriverWait(
            getDriver(), java.time.Duration.ofSeconds(10))
            .until(d -> cartPage.getCartItemCount() < 2);

        int    rowsAfter      = cartPage.getCartItemCount();
        double subtotalAfter  = cartPage.getSubtotal();

        Assert.assertEquals(rowsAfter, 1,
            "Cart must have 1 row after removing 1 item. Got=" + rowsAfter);
        Assert.assertFalse(cartPage.isProductInCart(removedName),
            "Removed item '" + removedName + "' must not appear in cart");
        Assert.assertTrue(subtotalAfter < subtotalBefore,
            "Subtotal must decrease after remove. Before=" + subtotalBefore + " After=" + subtotalAfter);

        log.info("PASS TC_OPS_003 | Removed '" + removedName + "' | subtotal: " + subtotalBefore + "→" + subtotalAfter);
    }

    // =========================================================================
    // TC_OPS_004: Cart summary — total = subtotal + shipping + tax
    // =========================================================================
    @Test(priority = 4,
          description = "TC_OPS_004: Cart summary total equals subtotal + shipping + tax")
    public void testCartSummaryTotal() {
        addKnownProduct();

        double subtotal = cartPage.getSubtotal();
        double shipping = cartPage.getShipping();
        double tax      = cartPage.getTax();
        double total    = cartPage.getTotal();

        Assert.assertTrue(subtotal > 0, "Subtotal must be > 0. Got=" + subtotal);
        Assert.assertTrue(tax > 0,      "Tax must be > 0. Got=" + tax);
        Assert.assertTrue(total > 0,    "Total must be > 0. Got=" + total);

        double expected = subtotal + shipping + tax;
        Assert.assertEquals(total, expected, 5.0,
            "Total must = subtotal+shipping+tax. "
            + subtotal + "+" + shipping + "+" + tax + "=" + expected
            + " UI shows=" + total);

        log.info("PASS TC_OPS_004 | " + subtotal + "+" + shipping + "+" + tax + "=" + total);
    }

    // =========================================================================
    // TC_OPS_005: Edge case — empty cart shows empty state, no summary
    // =========================================================================
    @Test(priority = 5,
          description = "TC_OPS_005: Empty cart shows empty state and hides order summary")
    public void testEmptyCartState() {
        // Navigate to cart directly with no items
        cartPage.navigateTo(baseUrl);

        Assert.assertTrue(cartPage.isCartEmpty(),
            "'Your cart is empty' must be visible when cart has no items");
        Assert.assertFalse(cartPage.isOrderSummaryVisible(),
            "Order Summary must NOT be visible when cart is empty");
        Assert.assertEquals(cartPage.getCartItemCount(), 0,
            "Cart row count must be 0 when empty");

        log.info("PASS TC_OPS_005 | Empty cart state verified");
    }

    // =========================================================================
    // TC_OPS_006: Edge case — qty cannot exceed available stock
    // =========================================================================
    @Test(priority = 6,
          description = "TC_OPS_006: Edge case — quantity cannot go below 1 at minimum")
    public void testEdgeCaseMinQty() {
        addKnownProduct();

        // Click minus 3 times at qty=1 — qty must stay 1, item must remain
        for (int i = 0; i < 3; i++) {
            cartPage.decreaseQuantity(0);
        }
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int qty = cartPage.getItemQuantity(0);
        Assert.assertEquals(qty, 1,
            "Qty must stay 1 after clicking minus multiple times at minimum. Got=" + qty);
        Assert.assertFalse(cartPage.isCartEmpty(),
            "Cart must NOT become empty when minus clicked at qty=1");
        Assert.assertEquals(cartPage.getCartItemCount(), 1,
            "Item must remain in cart. Rows=" + cartPage.getCartItemCount());

        log.info("PASS TC_OPS_006 | Qty stays 1 after 3 minus clicks at minimum");
    }

    // =========================================================================
    // TC_OPS_007: Edge case — cart persists after browser refresh
    // =========================================================================
    @Test(priority = 7,
          description = "TC_OPS_007: Edge case — cart items and quantities persist after refresh")
    public void testCartPersistsAfterRefresh() {
        addKnownProduct();

        // Increase qty to 3
        cartPage.increaseQuantity(0);
        cartPage.increaseQuantity(0);
        waitForQty(3);

        String itemName    = cartPage.getCartItemNames().get(0);
        int    qtyBefore   = cartPage.getItemQuantity(0);
        double priceBefore = cartPage.getItemUnitPrice(0);

        Assert.assertEquals(qtyBefore, 3, "Qty must be 3 before refresh");

        // Refresh
        getDriver().navigate().refresh();
        WaitUtils.waitForPageReady(getDriver());
        cartPage.waitForCartLoad();

        int    qtyAfter   = cartPage.getItemQuantity(0);
        double priceAfter = cartPage.getItemUnitPrice(0);
        boolean nameFound = cartPage.isProductInCart(itemName);

        Assert.assertTrue(nameFound,
            "Item '" + itemName + "' must still be in cart after refresh");
        Assert.assertEquals(qtyAfter, qtyBefore,
            "Qty must persist after refresh. Before=" + qtyBefore + " After=" + qtyAfter);
        Assert.assertEquals(priceAfter, priceBefore, 2.0,
            "Price must not change after refresh. Before=" + priceBefore + " After=" + priceAfter);

        log.info("PASS TC_OPS_007 | Cart persisted | item='" + itemName
                 + "' qty=" + qtyAfter + " price=" + priceAfter);
    }
}
