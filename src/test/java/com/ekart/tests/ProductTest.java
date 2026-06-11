package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.ProductPage;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

/**
 * Product Listing Page Tests
 * - Automate product listing page
 * - Validate product visibility
 */
@Test(groups = "product")
public class ProductTest extends BaseTest {

    private ProductPage productPage;

    @BeforeMethod(alwaysRun = true)
    public void goToProductsPage() {
        productPage = new ProductPage(getDriver());
        productPage.navigateTo(baseUrl);
    }

    @AfterMethod(alwaysRun = true)
    public void clearState() {
        clearBrowserSession();
    }

    @Test(
        priority = 1,
        groups = {"product", "smoke"},
        description = "TC_PROD_001: Verify products page loads successfully"
    )
    public void testProductPageLoads() {
        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains("product") || currentUrl.contains("shop"),
            "URL should contain 'product' or 'shop'. Got: " + currentUrl
        );
        log.info("PASS: Products page loaded | URL: " + currentUrl);
    }

    @Test(
        priority = 2,
        groups = {"product", "smoke"},
        description = "TC_PROD_002: Verify product grid is displayed"
    )
    public void testProductGridDisplayed() {
        Assert.assertTrue(
            productPage.isProductGridDisplayed(),
            "Product grid should be visible on products page"
        );
        log.info("PASS: Product grid is displayed");
    }

    @Test(
        priority = 3,
        groups = {"product", "smoke"},
        description = "TC_PROD_003: Verify products are listed on the page"
    )
    public void testProductsAreListed() {
        int count = productPage.getProductCount();
        Assert.assertTrue(count > 0,
            "At least one product should be listed. Found: " + count
        );
        log.info("PASS: Products listed | Count: " + count);
    }

    @Test(
        priority = 4,
        groups = {"product", "smoke"},
        description = "TC_PROD_004: Verify product names are visible on cards"
    )
    public void testProductNamesVisible() {
        List<WebElement> cards = productPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Product cards should be present");

        int checked = 0;
        for (WebElement card : cards) {
            String name = productPage.getProductName(card);
            Assert.assertFalse(name.isEmpty(),
                "Product name should not be empty on card " + (checked + 1));
            checked++;
            if (checked >= 5) break;
        }
        log.info("PASS: Product names visible | Checked: " + checked + " cards");
    }

    @Test(
        priority = 5,
        groups = {"product", "smoke"},
        description = "TC_PROD_005: Verify product prices are visible on cards"
    )
    public void testProductPricesVisible() {
        List<WebElement> cards = productPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Product cards should be present");

        int checked = 0;
        for (WebElement card : cards) {
            String price = productPage.getProductPrice(card);
            Assert.assertFalse(price.isEmpty(),
                "Product price should not be empty on card " + (checked + 1));
            Assert.assertTrue(
                price.contains("₹") || price.matches(".*\\d+.*"),
                "Price should contain ₹ or number. Got: " + price
            );
            checked++;
            if (checked >= 5) break;
        }
        log.info("PASS: Product prices visible | Checked: " + checked + " cards");
    }

    @Test(
        priority = 6,
        groups = {"product", "smoke"},
        description = "TC_PROD_006: Verify product images are visible on cards"
    )
    public void testProductImagesVisible() {
        List<WebElement> cards = productPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Product cards should be present");

        int checked = 0;
        for (WebElement card : cards) {
            Assert.assertTrue(
                productPage.isProductImageDisplayed(card),
                "Product image should be visible on card " + (checked + 1)
            );
            checked++;
            if (checked >= 5) break;
        }
        log.info("PASS: Product images visible | Checked: " + checked + " cards");
    }

    @Test(
        priority = 7,
        groups = {"product", "regression"},
        description = "TC_PROD_007: Verify Add to Cart button exists on product cards"
    )
    public void testAddToCartButtonExists() {
        List<WebElement> cards = productPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(), "Product cards should be present");

        int checked = 0;
        for (WebElement card : cards) {
            Assert.assertTrue(
                productPage.hasAddToCartButton(card),
                "Add to Cart button should exist on card " + (checked + 1)
            );
            checked++;
            if (checked >= 3) break;
        }
        log.info("PASS: Add to Cart buttons exist | Checked: " + checked + " cards");
    }

    @Test(
        priority = 8,
        groups = {"product", "regression"},
        description = "TC_PROD_008: Verify all products have name, price, and image"
    )
    public void testAllProductsComplete() {
        Assert.assertTrue(
            productPage.areAllProductsComplete(),
            "All product cards should have name, price and image"
        );
        log.info("PASS: All product cards are complete");
    }

    @Test(
        priority = 9,
        groups = {"product", "regression"},
        description = "TC_PROD_009: Verify product search functionality"
    )
    public void testProductSearch() {
        Assert.assertTrue(
            productPage.isSearchInputDisplayed(),
            "Search input should be present"
        );

        int totalBefore = productPage.getProductCount();
        productPage.searchProduct("Headphone");
        int countAfter = productPage.getProductCount();

        Assert.assertTrue(
            countAfter >= 0 &&
            (countAfter <= totalBefore || productPage.isNoResultsMessageDisplayed()),
            "Search should filter products or show no results"
        );
        log.info("PASS: Search works | Before: " + totalBefore + " | After: " + countAfter);
    }

    @Test(
        priority = 10,
        groups = {"product", "regression"},
        description = "TC_PROD_010: Search with no matching results shows message"
    )
    public void testSearchNoResults() {
        productPage.searchProduct("xyznonexistentproduct123");

        boolean noResults = productPage.isNoResultsMessageDisplayed();
        boolean zeroCards = productPage.getProductCount() == 0;

        Assert.assertTrue(noResults || zeroCards,
            "Should show no results message or 0 products for invalid search"
        );
        log.info("PASS: No results handled correctly");
    }

    @Test(
        priority = 11,
        groups = {"product", "regression"},
        description = "TC_PROD_011: Verify product names list is populated"
    )
    public void testProductNamesList() {
        List<String> names = productPage.getAllProductNames();
        Assert.assertFalse(names.isEmpty(),
            "Product names list should not be empty"
        );
        for (String name : names) {
            Assert.assertFalse(name.isEmpty(),
                "Each product name should not be blank"
            );
        }
        // Bug 4 Fix: guard against empty list before get(0)
        log.info("PASS: Product names | Count: " + names.size()
            + " | First: " + (!names.isEmpty() ? names.get(0) : "N/A"));
    }

    @Test(
        priority = 12,
        groups = {"product", "smoke"},
        description = "TC_PROD_012: Verify products are visible on home page"
    )
    public void testProductsVisibleOnHomePage() {
        productPage.navigateToHome(baseUrl);

        int count = productPage.getProductCount();
        Assert.assertTrue(count > 0,
            "Home page should show at least one product. Found: " + count
        );
        log.info("PASS: Products visible on home page | Count: " + count);
    }

    @Test(
        priority = 13,
        groups = {"product", "regression"},
        description = "TC_PROD_013: Clicking a product opens product detail page"
    )
    // Bug 4 Fix: Thread.sleep needs throws InterruptedException
    public void testClickProductOpensDetail() throws InterruptedException {
        List<WebElement> cards = productPage.getAllProductCards();
        Assert.assertFalse(cards.isEmpty(),
            "At least one product card should be present"
        );

        String urlBefore = getDriver().getCurrentUrl();
        productPage.clickFirstProduct();
        Thread.sleep(1500);

        String urlAfter = getDriver().getCurrentUrl();
        Assert.assertNotEquals(urlBefore, urlAfter,
            "Clicking a product should navigate to product detail page"
        );
        log.info("PASS: Product detail page opened | URL: " + urlAfter);
    }
}