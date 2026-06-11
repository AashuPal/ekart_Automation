package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.List;

/**
 * Page Object: Home Page — Product Listing (https://ekartms.netlify.app/)
 *
 * Covers:
 *  - Product grid visibility
 *  - Product card elements (image, name, price, rating, Add-to-Cart)
 *  - Click through to detail page
 *  - Search / filter toolbar
 */
public class ProductListingPage extends BasePage {

    // ── Top-level container ──────────────────────────────────────────────────
    private static final By PRODUCT_GRID =
        By.cssSelector("a[href*='/product/']");                 // ProductCard links

    private static final By ALL_PRODUCT_CARDS =
        By.cssSelector("a[href*='/product/']");

    // ── Toolbar ──────────────────────────────────────────────────────────────
    private static final By SORT_DROPDOWN =
        By.cssSelector("select");

    private static final By FILTERS_BUTTON =
        By.xpath("//button[.//span[text()='Filters']]");

    private static final By PRODUCT_COUNT_BADGE =
        By.xpath("//span[contains(@class,'rounded-full') and contains(text(),'products')]");

    private static final By GRID_VIEW_BUTTON =
        By.xpath("//button[@title='Grid view']");

    private static final By LIST_VIEW_BUTTON =
        By.xpath("//button[@title='List view']");

    // ── Product card internals (relative) ────────────────────────────────────
    private static final By CARD_IMAGE =
        By.cssSelector("img");

    private static final By CARD_NAME =
        By.cssSelector("h3");

    private static final By CARD_PRICE =
        By.xpath(".//span[contains(@class,'font-extrabold')]");

    private static final By CARD_ADD_TO_CART =
        By.xpath(".//button[.//span[text()='Add to Cart']]");

    private static final By CARD_WISHLIST_BUTTON =
        By.xpath(".//button[@aria-label]");

    // ── Pagination ───────────────────────────────────────────────────────────
    private static final By PAGINATION_NEXT =
        By.xpath("//button[contains(text(),'Next')]");

    private static final By PAGINATION_PREV =
        By.xpath("//button[contains(text(),'Previous')]");

    // ── Filter panel ─────────────────────────────────────────────────────────
    private static final By CATEGORY_SELECT =
        By.xpath("//label[.//span[text()='Category']]/following-sibling::select");

    private static final By MIN_PRICE_INPUT =
        By.xpath("//input[@placeholder='Min']");

    private static final By MAX_PRICE_INPUT =
        By.xpath("//input[@placeholder='Max']");

    private static final By APPLY_FILTER_BTN =
        By.xpath("//button[text()='Apply']");

    private static final By CLEAR_FILTER_BTN =
        By.xpath("//button[.//span[text()='Clear']]");

    // ── No-products empty state ───────────────────────────────────────────────
    private static final By NO_PRODUCTS_HEADING =
        By.xpath("//h3[text()='No products found']");

    // ── Loading skeletons ─────────────────────────────────────────────────────
    private static final By LOADING_SKELETONS =
        By.cssSelector(".skeleton");

    public ProductListingPage(WebDriver driver) {
        super(driver);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation
    // ────────────────────────────────────────────────────────────────────────

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl);
        waitForPageLoad();
    }

    /** Wait until the product grid has at least one card OR the empty state appears. */
    public void waitForPageLoad() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d ->
            !d.findElements(ALL_PRODUCT_CARDS).isEmpty() ||
            !d.findElements(NO_PRODUCTS_HEADING).isEmpty() ||
            !d.findElements(LOADING_SKELETONS).isEmpty()
        );
        // Also wait for skeletons to disappear
        try {
            wait.until(d -> d.findElements(LOADING_SKELETONS).isEmpty() ||
                           !d.findElements(ALL_PRODUCT_CARDS).isEmpty());
        } catch (TimeoutException ignored) {}
    }

    // ────────────────────────────────────────────────────────────────────────
    // Product grid queries
    // ────────────────────────────────────────────────────────────────────────

    /** Returns all product card <a> elements currently rendered. */
    public List<WebElement> getAllProductCards() {
        return driver.findElements(ALL_PRODUCT_CARDS);
    }

    public int getProductCount() {
        return getAllProductCards().size();
    }

    public boolean isProductGridVisible() {
        return !getAllProductCards().isEmpty();
    }

    /** Returns the product count shown in the toolbar badge (e.g. "24 products"). */
    public int getDisplayedProductCount() {
        try {
            String text = WaitUtils.waitForVisible(driver, PRODUCT_COUNT_BADGE).getText();
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Card detail helpers (operate on a single card element)
    // ────────────────────────────────────────────────────────────────────────

    public String getCardName(WebElement card) {
        try { return card.findElement(CARD_NAME).getText(); }
        catch (Exception e) { return ""; }
    }

    public String getCardPrice(WebElement card) {
        try { return card.findElement(CARD_PRICE).getText(); }
        catch (Exception e) { return ""; }
    }

    public boolean isCardImageVisible(WebElement card) {
        try {
            WebElement img = card.findElement(CARD_IMAGE);
            return img.isDisplayed() && !img.getAttribute("src").isEmpty();
        } catch (Exception e) { return false; }
    }

    /** Hover-triggered Add-to-Cart on desktop — use JS click to bypass hover requirement. */
    public void addToCart(WebElement card) {
        try {
            WebElement btn = card.findElement(CARD_ADD_TO_CART);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        } catch (Exception e) {
            log.warn("Add-to-cart button not accessible on card: " + e.getMessage());
        }
    }

    /** Returns the href URL of a product card (e.g. /product/<uuid>). */
    public String getCardHref(WebElement card) {
        return card.getAttribute("href");
    }

    public String getCardProductId(WebElement card) {
        String href = getCardHref(card);
        if (href != null && href.contains("/product/")) {
            return href.substring(href.lastIndexOf("/product/") + 9);
        }
        return "";
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation to Product Detail
    // ────────────────────────────────────────────────────────────────────────

    /** Clicks the first product card and returns the expected product ID. */
    public String clickFirstProduct() {
        List<WebElement> cards = getAllProductCards();
        if (cards.isEmpty()) throw new RuntimeException("No product cards found on page");
        String productId = getCardProductId(cards.get(0));
        cards.get(0).click();
        return productId;
    }

    /** Clicks the product card at the given zero-based index. */
    public void clickProductAt(int index) {
        List<WebElement> cards = getAllProductCards();
        if (index >= cards.size()) throw new RuntimeException("Card index out of bounds: " + index);
        cards.get(index).click();
    }

    /** Navigate directly to a product detail page by product ID. */
    public void navigateToProduct(String baseUrl, String productId) {
        driver.get(baseUrl + "/product/" + productId);
    }

    // ────────────────────────────────────────────────────────────────────────
    // View mode toggle
    // ────────────────────────────────────────────────────────────────────────

    public void switchToGridView() {
        if (isDisplayed(GRID_VIEW_BUTTON)) click(GRID_VIEW_BUTTON);
    }

    public void switchToListView() {
        if (isDisplayed(LIST_VIEW_BUTTON)) click(LIST_VIEW_BUTTON);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Filters
    // ────────────────────────────────────────────────────────────────────────

    public void openFilters() {
        click(FILTERS_BUTTON);
    }

    public void setPriceRange(String min, String max) {
        if (!min.isEmpty()) type(MIN_PRICE_INPUT, min);
        if (!max.isEmpty()) type(MAX_PRICE_INPUT, max);
    }

    public void applyFilters() {
        click(APPLY_FILTER_BTN);
        waitForPageLoad();
    }

    public void clearFilters() {
        if (isDisplayed(CLEAR_FILTER_BTN)) click(CLEAR_FILTER_BTN);
    }

    public boolean isFiltersVisible() {
        return isDisplayed(MIN_PRICE_INPUT);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Pagination
    // ────────────────────────────────────────────────────────────────────────

    public boolean hasNextPage() {
        try {
            WebElement btn = driver.findElement(PAGINATION_NEXT);
            return btn.isDisplayed() && btn.isEnabled();
        } catch (Exception e) { return false; }
    }

    public void goToNextPage() {
        click(PAGINATION_NEXT);
        waitForPageLoad();
    }

    // ────────────────────────────────────────────────────────────────────────
    // State checks
    // ────────────────────────────────────────────────────────────────────────

    public boolean isEmptyStateVisible() {
        return isDisplayed(NO_PRODUCTS_HEADING);
    }

    public boolean isLoadingVisible() {
        return !driver.findElements(LOADING_SKELETONS).isEmpty();
    }
}