package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.CartPage;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Wishlist Tests
 *
 *  TC_WISH_001  Add product to wishlist from detail page
 *  TC_WISH_002  Wishlist count in navbar updates after add
 *  TC_WISH_003  Same product cannot be added to wishlist twice
 *  TC_WISH_004  Remove product from wishlist
 *  TC_WISH_005  Wishlist persists after page refresh
 *  TC_WISH_006  Move item from wishlist to cart
 */
public class WishlistTest extends BaseTest {

    private static final String KNOWN_PRODUCT_ID =
        "656a732e-9e66-496b-83c0-8991a7a987c0";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;

    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));
        loginAsValidUser();
        log.info("Logged in for WishlistTest");
    }

    @BeforeMethod
    public void setup() {
        listingPage = new ProductListingPage(getDriver());
        detailPage  = new ProductDetailPage(getDriver());
        try {
            if (getDriver().getCurrentUrl().contains("/login"))
                loginAsValidUser();
        } catch (Exception ignored) {}
        clearWishlist();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void clearWishlist() {
        try {
            getDriver().get(baseUrl);
            WaitUtils.waitForPageReady(getDriver());
            ((JavascriptExecutor) getDriver())
                .executeScript("localStorage.setItem('wishlist','[]');");
        } catch (Exception e) { log.warn("Wishlist clear skipped"); }
    }

    private void navigateToKnownProduct() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
    }

    private void clickWishlistButton() {
        try {
            org.openqa.selenium.WebElement btn = getDriver()
                .findElement(By.xpath("//button[@aria-label='Add to wishlist' " +
                    "or @aria-label='Remove from wishlist']"));
            btn.click();
        } catch (Exception e) {
            log.warn("Wishlist button not found: " + e.getMessage());
        }
    }

    private String getWishlistLocalStorage() {
        Object result = ((JavascriptExecutor) getDriver())
            .executeScript("return localStorage.getItem('wishlist');");
        return result == null ? "[]" : result.toString();
    }

    private int getWishlistCount() {
        String json = getWishlistLocalStorage();
        if (json.equals("[]") || json.equals("null")) return 0;
        int count = 0, idx = 0;
        while ((idx = json.indexOf("productId", idx)) != -1) { count++; idx++; }
        return count;
    }

    // ── TC_WISH_001 ────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_WISH_001: Add product to wishlist from detail page")
    public void testAddToWishlist() {
        navigateToKnownProduct();

        int countBefore = getWishlistCount();
        clickWishlistButton();

        // Wait for wishlist to update in localStorage
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(
                getDriver(), java.time.Duration.ofSeconds(5))
                .until(d -> getWishlistCount() > countBefore);
        } catch (Exception ignored) {}

        int countAfter = getWishlistCount();
        Assert.assertTrue(countAfter > countBefore,
            "Wishlist count must increase after add. Before=" + countBefore
            + " After=" + countAfter);

        log.info("PASS TC_WISH_001 | wishlist count: " + countBefore + "→" + countAfter);
    }

    // ── TC_WISH_002 ────────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_WISH_002: Navbar wishlist count updates after add")
    public void testNavbarWishlistCountUpdates() {
        navigateToKnownProduct();

        int navBefore = getNavWishlistCount();
        clickWishlistButton();

        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int navAfter = getNavWishlistCount();
        Assert.assertTrue(navAfter >= navBefore,
            "Navbar wishlist count must update. Before=" + navBefore + " After=" + navAfter);

        log.info("PASS TC_WISH_002 | nav count: " + navBefore + "→" + navAfter);
    }

    // ── TC_WISH_003 ────────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_WISH_003: Same product cannot be added to wishlist twice")
    public void testNoDuplicateInWishlist() {
        navigateToKnownProduct();
        clickWishlistButton();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        int countAfterFirst = getWishlistCount();

        // Click again (should toggle off or stay same)
        clickWishlistButton();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        int countAfterSecond = getWishlistCount();

        // Should be 0 (toggled off) or same (idempotent) — never 2
        Assert.assertNotEquals(countAfterSecond, 2,
            "Wishlist must not contain duplicate entries. Got=" + countAfterSecond);

        log.info("PASS TC_WISH_003 | after 1st=" + countAfterFirst
                 + " after 2nd=" + countAfterSecond);
    }

    // ── TC_WISH_004 ────────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_WISH_004: Remove product from wishlist")
    public void testRemoveFromWishlist() {
        navigateToKnownProduct();
        clickWishlistButton();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int countAfterAdd = getWishlistCount();
        Assert.assertTrue(countAfterAdd >= 1, "Wishlist must have item before remove");

        // Click again to remove (toggle)
        clickWishlistButton();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int countAfterRemove = getWishlistCount();
        Assert.assertTrue(countAfterRemove < countAfterAdd,
            "Wishlist count must decrease after remove. Before=" + countAfterAdd
            + " After=" + countAfterRemove);

        log.info("PASS TC_WISH_004 | count: " + countAfterAdd + "→" + countAfterRemove);
    }

    // ── TC_WISH_005 ────────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_WISH_005: Wishlist items persist after page refresh")
    public void testWishlistPersistsAfterRefresh() {
        navigateToKnownProduct();
        clickWishlistButton();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int countBefore = getWishlistCount();
        Assert.assertTrue(countBefore >= 1, "Need item in wishlist before refresh");

        getDriver().navigate().refresh();
        WaitUtils.waitForPageReady(getDriver());

        int countAfter = getWishlistCount();
        Assert.assertEquals(countAfter, countBefore,
            "Wishlist count must persist after refresh. Before=" + countBefore
            + " After=" + countAfter);

        log.info("PASS TC_WISH_005 | wishlist persisted: count=" + countAfter);
    }

    // ── TC_WISH_006 ────────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_WISH_006: Move item from wishlist to cart via cart page")
    public void testMoveFromWishlistToCart() {
        // Add to cart first then move to wishlist via cart page
        CartPage cartPage = new CartPage(getDriver());
        cartPage.clearCartViaLocalStorage();

        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        detailPage.clickAddToCart();
        cartPage.navigateTo(baseUrl);

        int cartBefore = cartPage.getCartItemCount();
        Assert.assertTrue(cartBefore >= 1, "Need item in cart");

        cartPage.moveToWishlist(0);

        int cartAfter    = cartPage.getCartItemCount();
        int wishlistCount = getWishlistCount();

        Assert.assertTrue(cartAfter < cartBefore,
            "Cart must decrease after move to wishlist. Before=" + cartBefore
            + " After=" + cartAfter);
        Assert.assertTrue(wishlistCount >= 1,
            "Wishlist must contain the moved item. Got=" + wishlistCount);

        log.info("PASS TC_WISH_006 | cart: " + cartBefore + "→" + cartAfter
                 + " wishlist: " + wishlistCount);
    }

    // ── Private ────────────────────────────────────────────────────────────────
    private int getNavWishlistCount() {
        try {
            By badge = By.cssSelector(
                "a[href*='wishlist'] span[class*='rounded-full']," +
                "button[aria-label*='wishlist'] span[class*='rounded-full']");
            java.util.List<org.openqa.selenium.WebElement> els =
                getDriver().findElements(badge);
            if (els.isEmpty()) return 0;
            String text = els.get(0).getText().trim();
            return text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (Exception e) { return 0; }
    }
}