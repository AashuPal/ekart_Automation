package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Product Detail Validation & Navigation Tests
 *
 * ── Validate Product Details ──────────────────────────────────────
 *  TC_VAL_001  Product name is non-empty and not a placeholder
 *  TC_VAL_002  Selling price has valid currency format (₹ / number)
 *  TC_VAL_003  Base price >= selling price (discount not negative)
 *  TC_VAL_004  Discount badge matches calculated discount %
 *  TC_VAL_005  Brand name is visible and non-empty
 *  TC_VAL_006  Product description is non-empty
 *  TC_VAL_007  Main product image loads (src not blank)
 *  TC_VAL_008  Name on detail page matches name shown on listing card
 *  TC_VAL_009  Price on detail page matches price shown on listing card
 *
 * ── Verify Product Navigation ─────────────────────────────────────
 *  TC_NAV_001  Clicking any card lands on /product/<uuid> URL
 *  TC_NAV_002  URL product-id matches the card that was clicked
 *  TC_NAV_003  Browser Back returns to listing page with cards intact
 *  TC_NAV_004  Direct URL navigation loads correct product
 *  TC_NAV_005  Navigating card-by-card loads different products
 *  TC_NAV_006  "Back to Home" breadcrumb returns to home page
 *  TC_NAV_007  Invalid product UUID shows not-found, not crash
 *  TC_NAV_008  Multiple cards each have a unique /product/<id> URL
 */
public class ProductValidationTest extends BaseTest {

    private static final String KNOWN_PRODUCT_ID =
        "656a732e-9e66-496b-83c0-8991a7a987c0";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;

    @BeforeMethod
    public void initPages() {
        listingPage = new ProductListingPage(getDriver());
        detailPage  = new ProductDetailPage(getDriver());
    }

    // =========================================================================
    //  VALIDATE PRODUCT DETAILS
    // =========================================================================

    // ── TC_VAL_001 ────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_VAL_001: Product name is non-empty and not a placeholder")
    public void testProductNameNotEmpty() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String name = detailPage.getProductName();

        Assert.assertFalse(name.isEmpty(),
            "Product name must not be empty");
        Assert.assertFalse(name.equalsIgnoreCase("undefined"),
            "Product name must not be 'undefined'");
        Assert.assertFalse(name.equalsIgnoreCase("null"),
            "Product name must not be 'null'");
        Assert.assertTrue(name.length() >= 3,
            "Product name too short (< 3 chars): '" + name + "'");

        log.info("PASS TC_VAL_001 | name='" + name + "'");
    }

    // ── TC_VAL_002 ────────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_VAL_002: Selling price has valid currency format")
    public void testSellingPriceFormat() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String rawPrice = detailPage.getSellingPrice();

        Assert.assertFalse(rawPrice.isEmpty(),
            "Selling price must not be empty");

        // Strip currency symbol and whitespace, then verify numeric
        String numeric = rawPrice.replaceAll("[₹Rs.,\\s]", "");
        Assert.assertTrue(numeric.matches("\\d+(\\.\\d{1,2})?"),
            "Selling price '" + rawPrice + "' is not a valid numeric amount");

        double price = Double.parseDouble(numeric.replace(",", ""));
        Assert.assertTrue(price > 0,
            "Selling price must be > 0, got: " + price);

        log.info("PASS TC_VAL_002 | sellingPrice='" + rawPrice + "'");
    }

    // ── TC_VAL_003 ────────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_VAL_003: Base price >= selling price (no negative discount)")
    public void testBasePriceGreaterThanOrEqualToSellingPrice() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String rawBase    = detailPage.getBasePrice();
        String rawSelling = detailPage.getSellingPrice();

        // Base price is optional (not all products show a strike-through price)
        if (rawBase.isEmpty()) {
            log.info("INFO TC_VAL_003 | No base/original price shown — skip comparison");
            return;
        }

        double basePrice    = parsePrice(rawBase);
        double sellingPrice = parsePrice(rawSelling);

        Assert.assertTrue(basePrice >= sellingPrice,
            "Base price (" + basePrice + ") must be >= selling price (" + sellingPrice + ")");

        log.info("PASS TC_VAL_003 | base='" + rawBase + "' selling='" + rawSelling + "'");
    }

    // ── TC_VAL_004 ────────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_VAL_004: Discount badge percentage matches calculated discount")
    public void testDiscountBadgeMatchesCalculation() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String badgeText  = detailPage.getDiscountBadgeText();
        String rawBase    = detailPage.getBasePrice();
        String rawSelling = detailPage.getSellingPrice();

        if (badgeText.isEmpty() || rawBase.isEmpty()) {
            log.info("INFO TC_VAL_004 | No discount badge or base price — skip");
            return;
        }

        String badgeNum = badgeText.replaceAll("[^0-9]", "");
        if (badgeNum.isEmpty()) {
            log.info("INFO TC_VAL_004 | Badge has no numeric value: '" + badgeText + "' — skip");
            return;
        }

        double base     = parsePrice(rawBase);
        double selling  = parsePrice(rawSelling);
        double calcPct  = Math.round(((base - selling) / base) * 100);
        double badgePct = Double.parseDouble(badgeNum);

        // Allow ±2% rounding tolerance
        Assert.assertTrue(Math.abs(calcPct - badgePct) <= 2,
            "Discount badge (" + badgePct + "%) does not match calculated ("
            + calcPct + "%). base=" + base + " selling=" + selling);

        log.info("PASS TC_VAL_004 | badge=" + badgePct + "% calculated=" + calcPct + "%");
    }

    // ── TC_VAL_005 ────────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_VAL_005: Brand name is visible and non-empty on detail page")
    public void testBrandNameVisible() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String brand = detailPage.getBrandName();

        Assert.assertFalse(brand.isEmpty(),
            "Brand name should be visible and non-empty");
        Assert.assertFalse(brand.equalsIgnoreCase("null"),
            "Brand name should not be 'null'");

        log.info("PASS TC_VAL_005 | brand='" + brand + "'");
    }

    // ── TC_VAL_006 ────────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_VAL_006: Product description is non-empty")
    public void testProductDescriptionNotEmpty() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        String desc = detailPage.getDescription();

        Assert.assertFalse(desc.isEmpty(),
            "Product description must not be empty");
        Assert.assertTrue(desc.length() >= 10,
            "Description too short (< 10 chars): '" + desc + "'");

        log.info("PASS TC_VAL_006 | description length=" + desc.length());
    }

    // ── TC_VAL_007 ────────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_VAL_007: Main product image loads with valid src")
    public void testProductImageLoads() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        Assert.assertTrue(detailPage.isMainImageVisible(),
            "Main product image must be visible with a non-empty src");

        log.info("PASS TC_VAL_007 | Product image visible");
    }

    // ── TC_VAL_008 ────────────────────────────────────────────────────────────
    // FIX: capture cardName BEFORE click, then compare on detail page.
    //      cards list is NOT accessed after navigation — avoids StaleElementReferenceException.
    @Test(priority = 8,
          description = "TC_VAL_008: Product name on detail page matches name on listing card")
    public void testDetailNameMatchesListingCardName() {
        listingPage.navigateTo(baseUrl);

        List<WebElement> cards = listingPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Need at least one card");

        // Capture BEFORE navigation — no reference to cards after click
        String cardName     = listingPage.getCardName(cards.get(0)).trim();
        String cardHref     = listingPage.getCardHref(cards.get(0));
        Assert.assertFalse(cardName.isEmpty(), "Card name must not be empty");

        // Navigate directly by URL to avoid stale element on click
        String productId = listingPage.getCardProductId(cards.get(0));
        listingPage.navigateToProduct(baseUrl, productId);
        detailPage.waitForLoad();

        String detailName = detailPage.getProductName().trim();

        Assert.assertFalse(detailName.isEmpty(), "Detail page name must not be empty");
        Assert.assertTrue(
            detailName.toLowerCase().contains(cardName.toLowerCase()) ||
            cardName.toLowerCase().contains(detailName.toLowerCase()),
            "Detail name '" + detailName + "' should match card name '" + cardName + "'"
        );

        log.info("PASS TC_VAL_008 | card='" + cardName + "' detail='" + detailName + "'");
    }

    // ── TC_VAL_009 ────────────────────────────────────────────────────────────
    // FIX: same stale element fix — capture productId before navigation,
    //      navigate by URL, never touch the cards list after page change.
    @Test(priority = 9,
          description = "TC_VAL_009: Product price on detail page matches price shown on listing card")
    public void testDetailPriceMatchesListingCardPrice() {
        listingPage.navigateTo(baseUrl);

        List<WebElement> cards = listingPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Need at least one card");

        // Capture BEFORE navigation
        String cardPrice = listingPage.getCardPrice(cards.get(0)).trim();
        String productId = listingPage.getCardProductId(cards.get(0));
        Assert.assertFalse(cardPrice.isEmpty(), "Card price must not be empty");

        listingPage.navigateToProduct(baseUrl, productId);
        detailPage.waitForLoad();

        String detailPrice = detailPage.getSellingPrice().trim();
        Assert.assertFalse(detailPrice.isEmpty(), "Detail price must not be empty");

        double cardNum   = parsePrice(cardPrice);
        double detailNum = parsePrice(detailPrice);

        Assert.assertEquals(cardNum, detailNum, 1.0,
            "Card price (" + cardPrice + " = " + cardNum + ") must match " +
            "detail price (" + detailPrice + " = " + detailNum + ")");

        log.info("PASS TC_VAL_009 | cardPrice='" + cardPrice + "' detailPrice='" + detailPrice + "'");
    }

    // =========================================================================
    //  VERIFY PRODUCT NAVIGATION
    // =========================================================================

    // ── TC_NAV_001 ────────────────────────────────────────────────────────────
    @Test(priority = 10,
          description = "TC_NAV_001: Clicking any card navigates to /product/<uuid> URL")
    public void testClickCardLandsOnProductUrl() {
        listingPage.navigateTo(baseUrl);

        List<WebElement> cards = listingPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Need at least one card");

        // Capture href before click — cards list becomes stale after navigation
        String expectedProductId = listingPage.getCardProductId(cards.get(0));
        cards.get(0).click();
        detailPage.waitForLoad();

        String url = getDriver().getCurrentUrl();
        Assert.assertTrue(url.contains("/product/"),
            "URL must contain /product/ after clicking card. URL: " + url);

        // UUID pattern: 8-4-4-4-12 hex chars
        String actualId = detailPage.getProductIdFromUrl();
        Assert.assertTrue(actualId.matches("[0-9a-f\\-]{36}"),
            "Product ID in URL must be a valid UUID. Got: '" + actualId + "'");

        log.info("PASS TC_NAV_001 | URL: " + url);
    }

    // ── TC_NAV_002 ────────────────────────────────────────────────────────────
    // FIX: capture expectedId BEFORE click — cards list is stale after navigation.
    @Test(priority = 11,
          description = "TC_NAV_002: URL product-id matches the card that was clicked")
    public void testUrlProductIdMatchesClickedCard() {
        listingPage.navigateTo(baseUrl);

        List<WebElement> cards = listingPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Need at least one card");

        int idx = cards.size() > 1 ? 1 : 0;

        // Capture BEFORE click — stale after navigation
        String expectedId = listingPage.getCardProductId(cards.get(idx));
        Assert.assertFalse(expectedId.isEmpty(), "Card must have a product ID in href");

        cards.get(idx).click();
        detailPage.waitForLoad();

        String actualId = detailPage.getProductIdFromUrl();
        Assert.assertEquals(actualId, expectedId,
            "URL product ID must match the card's product ID");

        log.info("PASS TC_NAV_002 | expected=" + expectedId + " actual=" + actualId);
    }

    // ── TC_NAV_003 ────────────────────────────────────────────────────────────
    @Test(priority = 12,
          description = "TC_NAV_003: Browser Back from detail page returns to listing with cards intact")
    public void testBrowserBackReturnsToListing() {
        listingPage.navigateTo(baseUrl);

        int cardsBefore = listingPage.getProductCount();
        Assert.assertTrue(cardsBefore > 0, "Need products on listing page");

        // clickFirstProduct() navigates away — return value (productId) captured but not needed here
        listingPage.clickFirstProduct();
        detailPage.waitForLoad();
        Assert.assertTrue(detailPage.isOnProductDetailPage(), "Should be on detail page");

        getDriver().navigate().back();
        WaitUtils.waitForPageReady(getDriver());
        listingPage.waitForPageLoad();

        String urlAfterBack = getDriver().getCurrentUrl();
        Assert.assertFalse(urlAfterBack.contains("/product/"),
            "After Back, URL must not contain /product/. URL: " + urlAfterBack);

        int cardsAfter = listingPage.getProductCount();
        Assert.assertTrue(cardsAfter > 0,
            "Product grid must still have cards after Back. Found: " + cardsAfter);

        log.info("PASS TC_NAV_003 | cards after back=" + cardsAfter + " URL: " + urlAfterBack);
    }

    // ── TC_NAV_004 ────────────────────────────────────────────────────────────
    @Test(priority = 13,
          description = "TC_NAV_004: Direct URL navigation loads the correct product")
    public void testDirectUrlLoadsCorrectProduct() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();

        Assert.assertFalse(detailPage.isProductNotFound(),
            "Known product URL should not show not-found");

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(currentUrl.contains(KNOWN_PRODUCT_ID),
            "URL must contain the requested product ID. URL: " + currentUrl);

        String name = detailPage.getProductName();
        Assert.assertFalse(name.isEmpty(),
            "Product name must load via direct URL navigation");

        log.info("PASS TC_NAV_004 | product='" + name + "' ID=" + KNOWN_PRODUCT_ID);
    }

    // ── TC_NAV_005 ────────────────────────────────────────────────────────────
    @Test(priority = 14,
          description = "TC_NAV_005: Navigating card-by-card loads different products each time")
    public void testEachCardLoadsUniqueProduct() {
        listingPage.navigateTo(baseUrl);

        // Capture all product IDs from listing page BEFORE any navigation
        List<WebElement> cards = listingPage.getAllProductCards();
        int toCheck = Math.min(cards.size(), 3);
        Assert.assertTrue(toCheck >= 2, "Need at least 2 cards to compare");

        List<String> cardIds = new ArrayList<>();
        for (int i = 0; i < toCheck; i++) {
            cardIds.add(listingPage.getCardProductId(cards.get(i)));
        }

        List<String> visitedIds   = new ArrayList<>();
        List<String> visitedNames = new ArrayList<>();

        for (int i = 0; i < toCheck; i++) {
            // Navigate by URL — no stale element risk
            listingPage.navigateToProduct(baseUrl, cardIds.get(i));
            detailPage.waitForLoad();

            String detailId   = detailPage.getProductIdFromUrl();
            String detailName = detailPage.getProductName();

            Assert.assertFalse(visitedIds.contains(detailId),
                "Product ID '" + detailId + "' already visited — cards must be unique");

            visitedIds.add(detailId);
            visitedNames.add(detailName);
            log.info("Card[" + i + "] → id=" + detailId + " name='" + detailName + "'");
        }

        log.info("PASS TC_NAV_005 | Visited unique products: " + visitedIds);
    }

    // ── TC_NAV_006 ────────────────────────────────────────────────────────────
    @Test(priority = 15,
          description = "TC_NAV_006: 'Back to Home' breadcrumb returns to home page")
    public void testBackToHomeFromDetailPage() {
        listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
        detailPage.waitForLoad();
        Assert.assertFalse(detailPage.isProductNotFound());

        detailPage.goBackToHome();
        WaitUtils.waitForPageReady(getDriver());

        String urlAfter = getDriver().getCurrentUrl();
        Assert.assertFalse(urlAfter.contains("/product/"),
            "After clicking Back/Home, URL must not contain /product/. URL: " + urlAfter);

        listingPage.waitForPageLoad();
        Assert.assertTrue(listingPage.isProductGridVisible(),
            "Product grid must be visible after returning to home");

        log.info("PASS TC_NAV_006 | URL after back: " + urlAfter);
    }

    // ── TC_NAV_007 ────────────────────────────────────────────────────────────
    @Test(priority = 16,
          description = "TC_NAV_007: Invalid product UUID shows not-found state, not crash")
    public void testInvalidProductUrlShowsNotFound() {
        String fakeId = "00000000-0000-0000-0000-000000000000";
        listingPage.navigateToProduct(baseUrl, fakeId);
        detailPage.waitForLoad();

        boolean notFoundShown  = detailPage.isProductNotFound();
        boolean nameEmpty      = detailPage.getProductName().isEmpty();
        boolean urlIsOnProduct = getDriver().getCurrentUrl().contains("/product/");

        Assert.assertTrue(
            notFoundShown || (nameEmpty && urlIsOnProduct),
            "Invalid product URL should show not-found message or empty product, not crash"
        );

        log.info("PASS TC_NAV_007 | notFoundShown=" + notFoundShown
                + " URL: " + getDriver().getCurrentUrl());
    }

    // ── TC_NAV_008 ────────────────────────────────────────────────────────────
    @Test(priority = 17,
          description = "TC_NAV_008: Multiple cards each produce a unique /product/<id> URL")
    public void testMultipleCardsHaveUniqueProductUrls() {
        listingPage.navigateTo(baseUrl);

        List<WebElement> cards = listingPage.getAllProductCards();
        Assert.assertTrue(cards.size() >= 2,
            "Need at least 2 cards to verify uniqueness");

        int toCheck = Math.min(cards.size(), 6);
        List<String> hrefs = new ArrayList<>();

        for (int i = 0; i < toCheck; i++) {
            String href = listingPage.getCardHref(cards.get(i));
            Assert.assertNotNull(href, "Card[" + i + "] href must not be null");
            Assert.assertTrue(href.contains("/product/"),
                "Card[" + i + "] href must contain /product/. href: " + href);
            Assert.assertFalse(hrefs.contains(href),
                "Card[" + i + "] href is a duplicate: " + href);
            hrefs.add(href);
        }

        log.info("PASS TC_NAV_008 | Verified " + toCheck + " unique product card URLs");
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    /** Strips ₹, Rs, commas and spaces — returns numeric double. e.g. "₹1,299" → 1299.0 */
    private double parsePrice(String raw) {
        try {
            return Double.parseDouble(raw.replaceAll("[₹Rs.,\\s]", ""));
        } catch (NumberFormatException e) {
            log.warn("Could not parse price: '" + raw + "'");
            return 0;
        }
    }
}