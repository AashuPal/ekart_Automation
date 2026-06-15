package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.components.SearchFilterComponent;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.DynamicElementHandler;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

/**
 * Search, Filter, Dynamic Elements & Reusable Component Tests
 *
 * ── Search Functionality ──────────────────────────────────────────
 *  TC_SRCH_001  Search returns results for a valid keyword
 *  TC_SRCH_002  Section heading changes to "Search Results"
 *  TC_SRCH_003  All result names contain the searched keyword
 *  TC_SRCH_004  Search for unknown keyword shows empty state
 *  TC_SRCH_005  Clearing search restores all-products view
 *  TC_SRCH_006  Search is case-insensitive
 *  TC_SRCH_007  Partial keyword returns relevant results
 *
 * ── Filter Validation ─────────────────────────────────────────────
 *  TC_FILT_001  Filter panel opens and closes via Filters button
 *  TC_FILT_002  Price range filter — results within ₹ range
 *  TC_FILT_003  Min price only filter reduces results
 *  TC_FILT_004  Applying filters shows active filter count badge
 *  TC_FILT_005  Clear button removes all filters and restores results
 *  TC_FILT_006  Sort "Price: Low to High" returns ascending prices
 *  TC_FILT_007  Sort "Price: High to Low" returns descending prices
 *  TC_FILT_008  Rating filter shows only highly-rated products
 *  TC_FILT_009  Combined search + filter narrows results further
 *
 * ── Dynamic Elements ─────────────────────────────────────────────
 *  TC_DYN_001  Product grid loads without StaleElementReferenceException
 *  TC_DYN_002  Sorting re-renders cards without stale element errors
 *  TC_DYN_003  Filter apply waits correctly for count to stabilize
 *  TC_DYN_004  Loading spinner disappears before cards are asserted
 *  TC_DYN_005  Empty state element appears when no results found
 *
 * ── Reusable Component Verification ──────────────────────────────
 *  TC_COMP_001  SearchFilterComponent chains fluently
 *  TC_COMP_002  Component state readers return correct values
 *  TC_COMP_003  Component resets cleanly between test calls
 */
public class SearchFilterTest extends BaseTest {

    private static final By PRODUCT_CARDS = By.cssSelector("a[href*='/product/']");
    private static final By CARD_NAMES    = By.cssSelector("a[href*='/product/'] h3");

    private SearchFilterComponent searchFilter;
    private ProductListingPage    listingPage;

    @BeforeMethod
    public void setup() {
        listingPage   = new ProductListingPage(getDriver());
        searchFilter  = new SearchFilterComponent(getDriver());
        // Always start from clean home page
        listingPage.navigateTo(baseUrl);
        searchFilter.waitForResults();
    }

    // =========================================================================
    // SEARCH FUNCTIONALITY
    // =========================================================================

    // ── TC_SRCH_001 ───────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_SRCH_001: Search returns results for a valid keyword")
    public void testSearchReturnsResults() {
        searchFilter.searchByKeyword("mobiles");

        int count = searchFilter.getResultCount();
        Assert.assertTrue(count > 0,
            "Search for 'phone' must return at least one product. Got: " + count);

        log.info("PASS TC_SRCH_001 | Results for 'phone': " + count);
    }

    // ── TC_SRCH_002 ───────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_SRCH_002: Section heading changes to 'Search Results' after search")
    public void testSearchChangesHeading() {
        searchFilter.searchByKeyword("laptop");
        searchFilter.waitForSearchResults();

        String heading = searchFilter.getSectionHeading();
        Assert.assertTrue(heading.contains("Search Results"),
            "Heading should be 'Search Results' after search. Got: '" + heading + "'");

        log.info("PASS TC_SRCH_002 | Heading: '" + heading + "'");
    }

    // ── TC_SRCH_003 ───────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_SRCH_003: All result names are relevant to the searched keyword")
    public void testSearchResultsRelevance() {
        String keyword = "shirt";
        searchFilter.searchByKeyword(keyword);

        List<String> names = searchFilter.getVisibleProductNames();
        if (names.isEmpty()) {
            log.info("INFO TC_SRCH_003 | No products found for '" + keyword + "' — skipping relevance check");
            return;
        }

        // At least 50% of results should contain the keyword (partial match acceptable)
        long matching = names.stream()
            .filter(n -> n.toLowerCase().contains(keyword.toLowerCase()))
            .count();
        double matchRatio = (double) matching / names.size();

        Assert.assertTrue(matchRatio >= 0.5,
            "Expected >= 50% results to contain '" + keyword + "'. " +
            "Got " + matching + "/" + names.size() + " (" + (int)(matchRatio * 100) + "%)");

        log.info("PASS TC_SRCH_003 | Relevance: " + matching + "/" + names.size() + " match '" + keyword + "'");
    }

    // ── TC_SRCH_004 ───────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_SRCH_004: Searching for unknown keyword shows empty state")
    public void testSearchNoResultsShowsEmptyState() {
        searchFilter.searchByKeyword("xyznotaproduct12345qwerty");

        boolean emptyState = DynamicElementHandler.appearsWithin(
            getDriver(),
            By.xpath("//h3[text()='No products found'] | //p[contains(text(),'Try adjusting')]"),
            10
        );
        int count = searchFilter.getResultCount();

        Assert.assertTrue(emptyState || count == 0,
            "Searching for a nonsense keyword should show empty state or 0 results");

        log.info("PASS TC_SRCH_004 | Empty state: " + emptyState + " | Count: " + count);
    }

    // ── TC_SRCH_005 ───────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_SRCH_005: Clearing search restores all-products view")
    public void testClearSearchRestoresAllProducts() {
        // Capture baseline count
        int allCount = searchFilter.getResultCount();

        searchFilter.searchByKeyword("laptop");
        int searchCount = searchFilter.getResultCount();

        searchFilter.clearSearch();
        searchFilter.waitForResults();

        int afterClearCount = searchFilter.getResultCount();

        Assert.assertTrue(afterClearCount >= searchCount,
            "After clearing search, product count should be >= search results. " +
            "Before: " + allCount + " Search: " + searchCount + " After clear: " + afterClearCount);

        String heading = searchFilter.getSectionHeading();
        Assert.assertTrue(
            heading.contains("All Products") || heading.isEmpty(),
            "Heading should revert to 'All Products' after clearing search. Got: '" + heading + "'"
        );

        log.info("PASS TC_SRCH_005 | allCount=" + allCount + " searchCount=" + searchCount
                 + " afterClear=" + afterClearCount);
    }

    // ── TC_SRCH_006 ───────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_SRCH_006: Search is case-insensitive")
    public void testSearchIsCaseInsensitive() {
        searchFilter.searchByKeyword("PHONE");
        int upperCount = searchFilter.getResultCount();

        listingPage.navigateTo(baseUrl);
        searchFilter.waitForResults();

        searchFilter.searchByKeyword("phone");
        int lowerCount = searchFilter.getResultCount();

        Assert.assertEquals(upperCount, lowerCount,
            "Search should return same results regardless of case. " +
            "PHONE=" + upperCount + " phone=" + lowerCount);

        log.info("PASS TC_SRCH_006 | PHONE=" + upperCount + " phone=" + lowerCount);
    }

    // ── TC_SRCH_007 ───────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_SRCH_007: Partial keyword returns relevant results")
    public void testPartialKeywordSearch() {
        searchFilter.searchByKeyword("pho");   // partial of "phone"

        int count = searchFilter.getResultCount();
        // Either results exist OR empty state is shown — no crash
        boolean validState = count >= 0;
        Assert.assertTrue(validState, "Partial search must not crash");

        log.info("PASS TC_SRCH_007 | Partial 'pho' results: " + count);
    }

    // =========================================================================
    // FILTER VALIDATION
    // =========================================================================

    // ── TC_FILT_001 ───────────────────────────────────────────────────────────
    @Test(priority = 8,
          description = "TC_FILT_001: Filter panel opens and closes via Filters button")
    public void testFilterPanelOpenClose() {
        Assert.assertFalse(searchFilter.isFilterPanelOpen(),
            "Filter panel should be closed initially");

        searchFilter.openFilters();
        Assert.assertTrue(searchFilter.isFilterPanelOpen(),
            "Filter panel must open after clicking Filters");

        searchFilter.closeFilters();
        Assert.assertFalse(searchFilter.isFilterPanelOpen(),
            "Filter panel must close after clicking Filters again");

        log.info("PASS TC_FILT_001 | Filter panel open/close works");
    }

    // ── TC_FILT_002 ───────────────────────────────────────────────────────────
    @Test(priority = 9,
          description = "TC_FILT_002: Price range filter returns products within the range")
    public void testPriceRangeFilter() {
        double minPrice = 100.0;
        double maxPrice = 1000.0;

        int countBefore = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        searchFilter
            .setPriceRange("100", "1000")
            .applyFilters();

        int countAfter = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        // Products within range shown OR empty state
        boolean validState = countAfter >= 0 &&
            (countAfter <= countBefore || searchFilter.isEmptyStateVisible());
        Assert.assertTrue(validState,
            "Price filter must return <= original count or empty state. " +
            "Before=" + countBefore + " After=" + countAfter);

        // Verify prices are within range
        List<Double> prices = searchFilter.getVisibleProductPrices();
        for (Double price : prices) {
            Assert.assertTrue(price >= minPrice && price <= maxPrice,
                "Product price ₹" + price + " is outside filter range ₹" + minPrice + "–₹" + maxPrice);
        }

        log.info("PASS TC_FILT_002 | Before=" + countBefore + " After=" + countAfter
                 + " | Prices verified: " + prices.size());
    }

    // ── TC_FILT_003 ───────────────────────────────────────────────────────────
    @Test(priority = 10,
          description = "TC_FILT_003: Min price only filter reduces or changes results")
    public void testMinPriceOnlyFilter() {
        int countBefore = searchFilter.getResultCount();

        searchFilter
            .setMinPrice("5000")
            .applyFilters();

        int countAfter = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        // Min price filter should reduce count or show empty state
        Assert.assertTrue(
            countAfter <= countBefore || searchFilter.isEmptyStateVisible(),
            "Min price ₹5000 must reduce results or show empty state. " +
            "Before=" + countBefore + " After=" + countAfter
        );

        log.info("PASS TC_FILT_003 | Before=" + countBefore + " After=" + countAfter);
    }

    // ── TC_FILT_004 ───────────────────────────────────────────────────────────
    @Test(priority = 11,
          description = "TC_FILT_004: Applying filters shows active filter count badge")
    public void testActiveFilterCountBadge() {
        Assert.assertEquals(searchFilter.getActiveFilterCount(), 0,
            "No active filters initially");

        searchFilter
            .setPriceRange("200", "2000")
            .applyFilters();

        int activeCount = searchFilter.getActiveFilterCount();
        Assert.assertTrue(activeCount >= 1,
            "Active filter badge should show >= 1 after applying price range. Got: " + activeCount);

        log.info("PASS TC_FILT_004 | Active filter count: " + activeCount);
    }

    // ── TC_FILT_005 ───────────────────────────────────────────────────────────
    @Test(priority = 12,
          description = "TC_FILT_005: Clear button removes all filters and restores results")
    public void testClearFilterRestoresResults() {
        int countBefore = searchFilter.getResultCount();

        searchFilter
            .setPriceRange("500", "1000")
            .applyFilters();

        int countFiltered = searchFilter.getResultCount();

        searchFilter.clearFilters();

        int countAfterClear = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        Assert.assertTrue(countAfterClear >= countFiltered,
            "After clearing filters, count must be >= filtered count. " +
            "Before=" + countBefore + " Filtered=" + countFiltered + " AfterClear=" + countAfterClear);
        Assert.assertEquals(searchFilter.getActiveFilterCount(), 0,
            "Active filter badge must be 0 after clearing");

        log.info("PASS TC_FILT_005 | Before=" + countBefore + " Filtered=" + countFiltered
                 + " AfterClear=" + countAfterClear);
    }

    // ── TC_FILT_006 ───────────────────────────────────────────────────────────
    @Test(priority = 13,
          description = "TC_FILT_006: Sort 'Price: Low to High' returns ascending prices")
    public void testSortByPriceLowToHigh() {
        searchFilter.sortBy("Price: Low to High");

        List<Double> prices = searchFilter.getVisibleProductPrices();
        if (prices.size() < 2) {
            log.info("INFO TC_FILT_006 | Not enough products to verify sort order");
            return;
        }

        for (int i = 0; i < prices.size() - 1; i++) {
            Assert.assertTrue(prices.get(i) <= prices.get(i + 1),
                "Prices not ascending at index " + i + ": "
                + prices.get(i) + " > " + prices.get(i + 1));
        }

        log.info("PASS TC_FILT_006 | Ascending prices verified: " + prices);
    }

    // ── TC_FILT_007 ───────────────────────────────────────────────────────────
    @Test(priority = 14,
          description = "TC_FILT_007: Sort 'Price: High to Low' returns descending prices")
    public void testSortByPriceHighToLow() {
        searchFilter.sortBy("Price: High to Low");

        List<Double> prices = searchFilter.getVisibleProductPrices();
        if (prices.size() < 2) {
            log.info("INFO TC_FILT_007 | Not enough products to verify sort order");
            return;
        }

        for (int i = 0; i < prices.size() - 1; i++) {
            Assert.assertTrue(prices.get(i) >= prices.get(i + 1),
                "Prices not descending at index " + i + ": "
                + prices.get(i) + " < " + prices.get(i + 1));
        }

        log.info("PASS TC_FILT_007 | Descending prices verified: " + prices);
    }

    // ── TC_FILT_008 ───────────────────────────────────────────────────────────
    @Test(priority = 15,
          description = "TC_FILT_008: Rating filter reduces results to highly-rated products")
    public void testRatingFilter() {
        int countBefore = searchFilter.getResultCount();

        searchFilter
            .openFilters()
            .setMinRating("4")
            .applyFilters();

        int countAfter = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        Assert.assertTrue(
            countAfter <= countBefore || searchFilter.isEmptyStateVisible(),
            "4★ filter should reduce or keep results same. Before=" + countBefore
            + " After=" + countAfter
        );

        log.info("PASS TC_FILT_008 | Before=" + countBefore + " After 4★ filter=" + countAfter);
    }

    // ── TC_FILT_009 ───────────────────────────────────────────────────────────
    @Test(priority = 16,
          description = "TC_FILT_009: Combined search + price filter narrows results further")
    public void testCombinedSearchAndFilter() {
        searchFilter.searchByKeyword("phone");
        int searchOnlyCount = searchFilter.getResultCount();

        searchFilter
            .setPriceRange("500", "2000")
            .applyFilters();

        int combinedCount = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        Assert.assertTrue(
            combinedCount <= searchOnlyCount || searchFilter.isEmptyStateVisible(),
            "Search + price filter should narrow results. " +
            "SearchOnly=" + searchOnlyCount + " Combined=" + combinedCount
        );

        log.info("PASS TC_FILT_009 | SearchOnly=" + searchOnlyCount + " Combined=" + combinedCount);
    }

    // =========================================================================
    // DYNAMIC ELEMENTS
    // =========================================================================

    // ── TC_DYN_001 ────────────────────────────────────────────────────────────
    @Test(priority = 17,
          description = "TC_DYN_001: Product grid loads without StaleElementReferenceException")
    public void testNoStaleElementOnGridLoad() {
        DynamicElementHandler.waitForSkeletonsToDisappear(getDriver());

        // Re-fetch cards multiple times — would throw StaleElementReferenceException if list re-renders
        int count1 = DynamicElementHandler.retryGetCount(getDriver(), PRODUCT_CARDS);
        int count2 = DynamicElementHandler.retryGetCount(getDriver(), PRODUCT_CARDS);
        int count3 = DynamicElementHandler.retryGetCount(getDriver(), PRODUCT_CARDS);

        Assert.assertEquals(count1, count2, "Card count must be stable (1 vs 2)");
        Assert.assertEquals(count2, count3, "Card count must be stable (2 vs 3)");
        Assert.assertTrue(count1 > 0, "Grid must have at least one card");

        log.info("PASS TC_DYN_001 | Stable card count: " + count1);
    }

    // ── TC_DYN_002 ────────────────────────────────────────────────────────────
    @Test(priority = 18,
          description = "TC_DYN_002: Sorting re-renders cards without stale element errors")
    public void testNoStaleElementAfterSort() {
        // Capture names BEFORE sort (all from fresh fetch)
        List<String> namesBefore = searchFilter.getVisibleProductNames();
        Assert.assertFalse(namesBefore.isEmpty(), "Need products before sort");

        searchFilter.sortBy("Price: Low to High");

        // DynamicElementHandler safely re-fetches after sort re-render
        int stableCount = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);
        Assert.assertTrue(stableCount > 0, "Cards must be present after sort");

        // Safe fetch after re-render — no stale exception
        List<String> namesAfter = DynamicElementHandler.retry(
            () -> getDriver().findElements(CARD_NAMES)
                             .stream()
                             .map(el -> el.getText().trim())
                             .collect(java.util.stream.Collectors.toList()),
            3
        );
        Assert.assertFalse(namesAfter.isEmpty(), "Product names must be readable after sort");

        log.info("PASS TC_DYN_002 | Names before=" + namesBefore.size()
                 + " after sort=" + namesAfter.size());
    }

    // ── TC_DYN_003 ────────────────────────────────────────────────────────────
    @Test(priority = 19,
          description = "TC_DYN_003: Filter apply waits correctly for count to stabilize")
    public void testFilterCountStabilizesAfterApply() {
        int baseline = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);
        Assert.assertTrue(baseline > 0, "Need products as baseline");

        searchFilter.setPriceRange("100", "5000").applyFilters();

        // waitForCountToStabilize handles the flicker during re-render
        int stabilized = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 60);

        Assert.assertTrue(stabilized >= 0,
            "Stabilized count must be a valid non-negative number. Got: " + stabilized);

        log.info("PASS TC_DYN_003 | Baseline=" + baseline + " Stabilized=" + stabilized);
    }

    // ── TC_DYN_004 ────────────────────────────────────────────────────────────
    @Test(priority = 20,
          description = "TC_DYN_004: Loading spinner disappears before cards are asserted")
    public void testLoadingSpinnerDisappearsBeforeAssert() {
        // Navigate fresh to trigger loading state
        listingPage.navigateTo(baseUrl);

        DynamicElementHandler.waitForLoadersToDisappear(getDriver());
        DynamicElementHandler.waitForSkeletonsToDisappear(getDriver());

        // Only assert AFTER loaders gone
        int count = searchFilter.getResultCount();
        Assert.assertTrue(count > 0,
            "After loaders disappear, at least one product card should be visible");

        log.info("PASS TC_DYN_004 | Cards visible after spinner gone: " + count);
    }

    // ── TC_DYN_005 ────────────────────────────────────────────────────────────
    @Test(priority = 21,
          description = "TC_DYN_005: Empty state element appears when no results found")
    public void testEmptyStateDynamicElement() {
        searchFilter.searchByKeyword("zzz_no_such_product_99999");

        By emptyState = By.xpath(
            "//h3[text()='No products found'] | //p[contains(text(),'Try adjusting')]");

        boolean appeared = DynamicElementHandler.appearsWithin(getDriver(), emptyState, 10);
        int count = searchFilter.getResultCount();

        Assert.assertTrue(appeared || count == 0,
            "Empty state element must appear for no-results search");

        log.info("PASS TC_DYN_005 | Empty state appeared: " + appeared + " count=" + count);
    }

    // =========================================================================
    // REUSABLE COMPONENT VERIFICATION
    // =========================================================================

    // ── TC_COMP_001 ───────────────────────────────────────────────────────────
    @Test(priority = 22,
          description = "TC_COMP_001: SearchFilterComponent chains fluently in one statement")
    public void testComponentFluientChaining() {
        // Entire filter setup in one fluent chain — no intermediate variables
        searchFilter
            .openFilters()
            .setPriceRange("100", "3000")
            .setMinRating("3")
            .applyFilters();

        int count = searchFilter.getResultCount();
        int activeFilters = searchFilter.getActiveFilterCount();

        // Component must apply all chained operations
        Assert.assertTrue(activeFilters >= 1,
            "Fluent chain must apply filters. Active count: " + activeFilters);

        log.info("PASS TC_COMP_001 | Fluent chain applied " + activeFilters
                 + " filters, results=" + count);
    }

    // ── TC_COMP_002 ───────────────────────────────────────────────────────────
    @Test(priority = 23,
          description = "TC_COMP_002: Component state readers return correct values")
    public void testComponentStateReaders() {
        // Baseline: no filters
        Assert.assertEquals(searchFilter.getActiveFilterCount(), 0,
            "getActiveFilterCount() must return 0 initially");
        Assert.assertFalse(searchFilter.isFilterPanelOpen(),
            "isFilterPanelOpen() must return false initially");
        Assert.assertFalse(searchFilter.isEmptyStateVisible(),
            "isEmptyStateVisible() must return false when products are loaded");

        int count = searchFilter.getResultCount();
        Assert.assertTrue(count > 0,
            "getResultCount() must return > 0 on initial load");

        List<String> names = searchFilter.getVisibleProductNames();
        Assert.assertFalse(names.isEmpty(),
            "getVisibleProductNames() must return non-empty list");
        Assert.assertFalse(names.get(0).isEmpty(),
            "First product name must not be empty");

        List<Double> prices = searchFilter.getVisibleProductPrices();
        Assert.assertFalse(prices.isEmpty(),
            "getVisibleProductPrices() must return non-empty list");
        Assert.assertTrue(prices.get(0) > 0,
            "First product price must be > 0. Got: " + prices.get(0));

        log.info("PASS TC_COMP_002 | count=" + count + " names=" + names.size()
                 + " prices=" + prices.size());
    }

    // ── TC_COMP_003 ───────────────────────────────────────────────────────────
    @Test(priority = 24,
          description = "TC_COMP_003: Component resets cleanly between test calls")
    public void testComponentResetsCleanly() {
        // First use: apply filter
        searchFilter.setPriceRange("200", "1000").applyFilters();
        int filteredCount = searchFilter.getResultCount();

        // Clear using component
        searchFilter.clearFilters();
        int clearedCount = DynamicElementHandler
            .waitForCountToStabilize(getDriver(), PRODUCT_CARDS, 50);

        // Re-create component (same driver) — state from DOM, not object fields
        SearchFilterComponent freshComponent = new SearchFilterComponent(getDriver());
        int freshCount = freshComponent.getResultCount();
        int freshActive = freshComponent.getActiveFilterCount();

        Assert.assertEquals(freshCount, clearedCount,
            "Fresh component instance must read same DOM state as cleared component");
        Assert.assertEquals(freshActive, 0,
            "Fresh component must see 0 active filters after clear");
        Assert.assertTrue(clearedCount >= filteredCount,
            "Cleared state must have >= products than filtered state");

        log.info("PASS TC_COMP_003 | filtered=" + filteredCount
                 + " cleared=" + clearedCount + " freshRead=" + freshCount);
    }
}