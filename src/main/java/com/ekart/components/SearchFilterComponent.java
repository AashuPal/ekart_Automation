package com.ekart.components;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;
import java.util.List;

/**
 * Reusable Component: Search Bar + Filter Panel
 *
 * Encapsulates all search and filter interactions on the home page.
 * Used by SearchFilterTest and any other test needing search/filter.
 *
 * Usage:
 *   SearchFilterComponent search = new SearchFilterComponent(driver);
 *   search.searchByKeyword("laptop");
 *   search.openFilters().selectCategory("Electronics").applyFilters();
 */
public class SearchFilterComponent {

    private final WebDriver driver;
    protected static final Logger log = LogManager.getLogger(SearchFilterComponent.class);

    // ── Navbar search (desktop) ───────────────────────────────────────────────
    private static final By NAVBAR_SEARCH_INPUT =
        By.cssSelector("input[placeholder='Search for products, brands and more...']");

    // ── Toolbar: sort dropdown ────────────────────────────────────────────────
    private static final By SORT_DROPDOWN =
        By.cssSelector("select");   // first <select> in toolbar

    // ── Toolbar: Filters button ───────────────────────────────────────────────
    private static final By FILTERS_BUTTON =
        By.xpath("//button[.//span[text()='Filters']]");

    private static final By ACTIVE_FILTER_COUNT_BADGE =
        By.xpath("//button[.//span[text()='Filters']]//span[contains(@class,'rounded-full')]");

    // ── Filter panel fields ───────────────────────────────────────────────────
    private static final By FILTER_PANEL =
        By.xpath("//div[.//label[.//span[text()='Category']]]");

    private static final By CATEGORY_SELECT =
        By.xpath("//label[.//span[text()='Category']]/following-sibling::select" +
                 " | //label[.//span[text()='Category']]/..//select");

    private static final By BRAND_SELECT =
        By.xpath("//label[.//span[text()='Brand']]/following-sibling::select" +
                 " | //label[.//span[text()='Brand']]/..//select");

    private static final By MIN_PRICE_INPUT =
        By.cssSelector("input[placeholder='Min']");

    private static final By MAX_PRICE_INPUT =
        By.cssSelector("input[placeholder='Max']");

    private static final By RATING_SELECT =
        By.xpath("//label[.//span[text()='Rating']]/..//select" +
                 " | //select[option[text()='Any Rating']]");

    private static final By APPLY_BUTTON =
        By.xpath("//button[text()='Apply']");

    private static final By CLEAR_BUTTON =
        By.xpath("//button[.//span[text()='Clear']]");

    // ── Active filter chips ───────────────────────────────────────────────────
    private static final By ACTIVE_CHIPS =
        By.xpath("//div[contains(@class,'flex-wrap') and .//button[@class[contains(.,'hover:text')]]]//span");

    // ── Section heading ───────────────────────────────────────────────────────
    private static final By SECTION_HEADING =
        By.xpath("//h2[contains(.,'All Products') or contains(.,'Search Results')]");

    // ── Result count text ─────────────────────────────────────────────────────
    private static final By RESULT_COUNT_BADGE =
        By.xpath("//span[contains(@class,'rounded-full') and (contains(text(),'product') or contains(text(),'result'))]");

    // ── Empty state ───────────────────────────────────────────────────────────
    private static final By EMPTY_STATE =
        By.xpath("//h3[text()='No products found'] | //p[contains(text(),'Try adjusting')]");

    // ── Product cards ─────────────────────────────────────────────────────────
    private static final By PRODUCT_CARDS =
        By.cssSelector("a[href*='/product/']");

    // ── Loading indicator ─────────────────────────────────────────────────────
    private static final By LOADING =
        By.cssSelector(".animate-spin, .animate-pulse");

    public SearchFilterComponent(WebDriver driver) {
        this.driver = driver;
    }

    // =========================================================================
    // Search
    // =========================================================================

    /**
     * Type keyword into navbar search and press Enter.
     * Waits for results to load.
     */
    public SearchFilterComponent searchByKeyword(String keyword) {
        WebElement input = WaitUtils.waitForVisible(driver, NAVBAR_SEARCH_INPUT);
        input.clear();
        input.sendKeys(keyword);
        input.sendKeys(Keys.ENTER);
        waitForResults();
        log.info("Searched for: '" + keyword + "'");
        return this;
    }

    /**
     * Clear the search box and restore all-products view.
     */
    public SearchFilterComponent clearSearch() {
        try {
            WebElement input = driver.findElement(NAVBAR_SEARCH_INPUT);
            input.clear();
            input.sendKeys(Keys.ENTER);
            waitForResults();
        } catch (Exception e) {
            log.warn("Could not clear search: " + e.getMessage());
        }
        return this;
    }

    public String getSearchInputValue() {
        try { return driver.findElement(NAVBAR_SEARCH_INPUT).getAttribute("value"); }
        catch (Exception e) { return ""; }
    }

    // =========================================================================
    // Sort
    // =========================================================================

    /**
     * Select sort option by visible text.
     * e.g. "Price: Low to High", "Newest First"
     */
    public SearchFilterComponent sortBy(String optionText) {
        try {
            new Select(WaitUtils.waitForVisible(driver, SORT_DROPDOWN))
                .selectByVisibleText(optionText);
            waitForResults();
            log.info("Sorted by: '" + optionText + "'");
        } catch (Exception e) {
            log.warn("Could not sort by '" + optionText + "': " + e.getMessage());
        }
        return this;
    }

    public String getSelectedSortOption() {
        try {
            return new Select(driver.findElement(SORT_DROPDOWN)).getFirstSelectedOption().getText();
        } catch (Exception e) { return ""; }
    }

    // =========================================================================
    // Filter panel open / close
    // =========================================================================

    /**
     * Open the filter panel if it's not already open.
     */
    public SearchFilterComponent openFilters() {
        if (!isFilterPanelOpen()) {
            click(FILTERS_BUTTON);
            waitForFilterPanel();
            log.info("Filter panel opened");
        }
        return this;
    }

    /**
     * Close the filter panel if it's open.
     */
    public SearchFilterComponent closeFilters() {
        if (isFilterPanelOpen()) {
            click(FILTERS_BUTTON);
        }
        return this;
    }

    public boolean isFilterPanelOpen() {
        return isDisplayed(MIN_PRICE_INPUT);
    }

    // =========================================================================
    // Filter setters (fluent — chain them)
    // =========================================================================

    /** Select category by visible text, e.g. "Electronics". */
    public SearchFilterComponent selectCategory(String categoryName) {
        openFilters();
        selectDropdown(CATEGORY_SELECT, categoryName);
        log.info("Category selected: '" + categoryName + "'");
        return this;
    }

    /** Select brand by visible text, e.g. "Samsung". */
    public SearchFilterComponent selectBrand(String brandName) {
        openFilters();
        selectDropdown(BRAND_SELECT, brandName);
        log.info("Brand selected: '" + brandName + "'");
        return this;
    }

    /** Set minimum price filter. */
    public SearchFilterComponent setMinPrice(String min) {
        openFilters();
        typeInto(MIN_PRICE_INPUT, min);
        return this;
    }

    /** Set maximum price filter. */
    public SearchFilterComponent setMaxPrice(String max) {
        openFilters();
        typeInto(MAX_PRICE_INPUT, max);
        return this;
    }

    /** Set price range in one call. */
    public SearchFilterComponent setPriceRange(String min, String max) {
        openFilters();
        typeInto(MIN_PRICE_INPUT, min);
        typeInto(MAX_PRICE_INPUT, max);
        log.info("Price range set: ₹" + min + " – ₹" + max);
        return this;
    }

    /**
     * Select minimum rating filter.
     * @param stars  "4", "3", "2", "1" or "" for any rating
     */
    public SearchFilterComponent setMinRating(String stars) {
        openFilters();
        try {
            new Select(WaitUtils.waitForVisible(driver, RATING_SELECT))
                .selectByValue(stars);
            log.info("Min rating set: " + stars + "★");
        } catch (Exception e) {
            log.warn("Could not set rating '" + stars + "': " + e.getMessage());
        }
        return this;
    }

    // =========================================================================
    // Apply / Clear
    // =========================================================================

    /**
     * Click Apply and wait for results to reload.
     */
    public SearchFilterComponent applyFilters() {
        click(APPLY_BUTTON);
        waitForResults();
        log.info("Filters applied");
        return this;
    }

    /**
     * Click Clear and wait for results to reload.
     */
    public SearchFilterComponent clearFilters() {
        if (isDisplayed(CLEAR_BUTTON)) {
            click(CLEAR_BUTTON);
            waitForResults();
            log.info("Filters cleared");
        }
        return this;
    }

    // =========================================================================
    // State readers
    // =========================================================================

    /** Number of product cards currently visible. */
    public int getResultCount() {
        return driver.findElements(PRODUCT_CARDS).size();
    }

    /** Returns true if no products are displayed. */
    public boolean isEmptyStateVisible() {
        return isDisplayed(EMPTY_STATE);
    }

    /** Returns the section heading text: "All Products" or "Search Results". */
    public String getSectionHeading() {
        try { return driver.findElement(SECTION_HEADING).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    /** Returns number of active filters shown in the badge on the Filters button. */
    public int getActiveFilterCount() {
        try {
            String text = driver.findElement(ACTIVE_FILTER_COUNT_BADGE).getText().trim();
            return Integer.parseInt(text);
        } catch (Exception e) { return 0; }
    }

    /** Returns text of all active filter chips (e.g. ["Electronics", "₹500–₹2000"]). */
    public List<String> getActiveFilterChips() {
        return driver.findElements(ACTIVE_CHIPS)
                     .stream()
                     .map(el -> el.getText().trim())
                     .filter(t -> !t.isEmpty())
                     .collect(java.util.stream.Collectors.toList());
    }

    /** Returns all product card name texts currently visible. */
    public List<String> getVisibleProductNames() {
        return driver.findElements(By.cssSelector("a[href*='/product/'] h3"))
                     .stream()
                     .map(el -> el.getText().trim())
                     .collect(java.util.stream.Collectors.toList());
    }

    /** Returns all product prices as parsed doubles. */
    public List<Double> getVisibleProductPrices() {
        return driver.findElements(
                By.cssSelector("a[href*='/product/'] span.font-extrabold, " +
                               "a[href*='/product/'] span[class*='font-extrabold']"))
                     .stream()
                     .map(el -> {
                         try {
                             return Double.parseDouble(
                                 el.getText().replaceAll("[₹Rs.,\\s]", ""));
                         } catch (Exception e2) { return -1.0; }
                     })
                     .filter(p -> p >= 0)
                     .collect(java.util.stream.Collectors.toList());
    }

    // =========================================================================
    // Dynamic element waits (reusable across tests)
    // =========================================================================

    /**
     * Wait for the product grid to finish loading.
     * Handles: loading spinner → cards appear OR empty state appears.
     */
    public void waitForResults() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        // 1. Wait for loading indicators to disappear
        try {
            wait.until(d -> d.findElements(LOADING).isEmpty());
        } catch (TimeoutException ignored) {}
        // 2. Wait for cards OR empty state
        wait.until(d ->
            !d.findElements(PRODUCT_CARDS).isEmpty() ||
            !d.findElements(EMPTY_STATE).isEmpty()
        );
    }

    /**
     * Wait for the filter panel to appear after clicking Filters button.
     */
    public void waitForFilterPanel() {
        WaitUtils.waitForVisible(driver, MIN_PRICE_INPUT);
    }

    /**
     * Wait for product count to change from a known baseline.
     * Useful after applying a filter: waitForCountToChangFrom(before).
     */
    public void waitForCountToChangeFrom(int previousCount) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(d -> d.findElements(PRODUCT_CARDS).size() != previousCount);
    }

    /**
     * Wait for the section heading to become "Search Results".
     */
    public void waitForSearchResults() {
        new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(d -> {
                List<WebElement> h = d.findElements(SECTION_HEADING);
                return !h.isEmpty() && h.get(0).getText().contains("Search Results");
            });
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void click(By locator) {
        WaitUtils.waitForClickable(driver, locator).click();
    }

    private void typeInto(By locator, String text) {
        WebElement el = WaitUtils.waitForVisible(driver, locator);
        el.clear();
        el.sendKeys(text);
    }

    private void selectDropdown(By locator, String visibleText) {
        try {
            new Select(WaitUtils.waitForVisible(driver, locator))
                .selectByVisibleText(visibleText);
        } catch (NoSuchElementException | org.openqa.selenium.support.ui.UnexpectedTagNameException e) {
            log.warn("Dropdown option '" + visibleText + "' not found: " + e.getMessage());
        }
    }

    private boolean isDisplayed(By locator) {
        try { return driver.findElement(locator).isDisplayed(); }
        catch (NoSuchElementException e) { return false; }
    }
}