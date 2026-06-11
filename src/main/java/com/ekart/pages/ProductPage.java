package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;
import java.util.ArrayList;
import java.util.List;

/**
 * Page Object Model — Product Listing Page
 * Covers: product visibility, search, filter, sort, card details
 */
public class ProductPage extends BasePage {

    // ===== Locators =====
    private final By productsGrid   = By.xpath("//div[contains(@class,'grid')]");
    private final By productCards   = By.xpath("//div[contains(@class,'grid')]//div[contains(@class,'rounded') and .//img]");

    // Relative locators used with card.findElement() — no leading //
    private final By cardName       = By.xpath(".//h3 | .//h2 | .//p[contains(@class,'font')]");
    private final By cardPrice      = By.xpath(".//*[contains(text(),'₹') or contains(@class,'price')]");
    private final By cardImage      = By.xpath(".//img");
    // Bug 1 Fix: 'Add' alone matches too broadly — use more specific text
    private final By cardCartBtn    = By.xpath(".//button[contains(.,'Add to Cart') or contains(.,'Add to cart')]");

    // Search
    private final By searchInput    = By.xpath("//input[@placeholder='Search products...' or @placeholder='Search' or @type='search']");

    // Sort — Bug 2 Fix: removed unused categoryFilter / filterButtons fields
    private final By sortDropdown   = By.xpath("//select[contains(@class,'sort') or contains(@id,'sort')]");

    // No results
    private final By noResultsMsg   = By.xpath("//*[contains(text(),'No products') or contains(text(),'not found') or contains(text(),'empty')]");

    // Loading spinner
    private final By loadingSpinner = By.xpath("//*[contains(@class,'animate-spin') or contains(@class,'loading')]");

    // Page heading
    private final By pageHeading    = By.xpath("//h1 | //h2[contains(text(),'Product') or contains(text(),'Shop')]");

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    // ===== Navigation =====
    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/products");
        log.info("Navigated to Products page");
        waitForProductsToLoad();
    }

    public void navigateToHome(String baseUrl) {
        driver.get(baseUrl);
        log.info("Navigated to Home page");
        waitForProductsToLoad();
    }

    // ===== Wait for load =====
    private void waitForProductsToLoad() {
        try {
            WaitUtils.waitForInvisibleSafe(driver, loadingSpinner);
        } catch (Exception e) {
            // No spinner — already loaded
        }
        try {
            WaitUtils.waitForVisible(driver, productsGrid);
        } catch (Exception e) {
            log.warn("Products grid not found within timeout");
        }
    }

    // ===== Product Card Methods =====

    public List<WebElement> getAllProductCards() {
        try {
            return driver.findElements(productCards);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public int getProductCount() {
        return getAllProductCards().size();
    }

    public boolean isProductGridDisplayed() {
        return isDisplayed(productsGrid);
    }

    public String getProductName(WebElement card) {
        try {
            return card.findElement(cardName).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getProductPrice(WebElement card) {
        try {
            return card.findElement(cardPrice).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isProductImageDisplayed(WebElement card) {
        try {
            WebElement img = card.findElement(cardImage);
            String src = img.getAttribute("src");
            return img.isDisplayed() && src != null && !src.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAddToCartButton(WebElement card) {
        try {
            return card.findElement(cardCartBtn).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickFirstProduct() {
        List<WebElement> cards = getAllProductCards();
        if (!cards.isEmpty()) {
            cards.get(0).click();
            log.info("Clicked first product card");
        }
    }

    // Bug 3 Fix: JavascriptExecutor import was missing — cast inline
    public void clickAddToCartOnFirst() {
        List<WebElement> cards = getAllProductCards();
        if (!cards.isEmpty()) {
            try {
                WebElement btn = cards.get(0).findElement(cardCartBtn);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                log.info("Clicked Add to Cart on first product");
            } catch (Exception e) {
                log.warn("Add to Cart button not found: " + e.getMessage());
            }
        }
    }

    // ===== Search =====
    public void searchProduct(String keyword) {
        try {
            type(searchInput, keyword);
            log.info("Searched for: " + keyword);
            waitForProductsToLoad();
        } catch (Exception e) {
            log.warn("Search input not found: " + e.getMessage());
        }
    }

    public void clearSearch() {
        try {
            driver.findElement(searchInput).clear();
            waitForProductsToLoad();
        } catch (Exception e) {
            log.warn("Could not clear search");
        }
    }

    public boolean isSearchInputDisplayed() {
        return isDisplayed(searchInput);
    }

    // ===== Filter =====
    public void clickCategoryFilter(String categoryName) {
        try {
            By btn = By.xpath("//button[contains(text(),'" + categoryName + "')]");
            click(btn);
            log.info("Clicked category: " + categoryName);
            waitForProductsToLoad();
        } catch (Exception e) {
            log.warn("Category filter not found: " + categoryName);
        }
    }

    // ===== Sort =====
    public void sortBy(String option) {
        try {
            Select select = new Select(driver.findElement(sortDropdown));
            select.selectByVisibleText(option);
            log.info("Sorted by: " + option);
            waitForProductsToLoad();
        } catch (Exception e) {
            log.warn("Sort dropdown not found or option invalid: " + option);
        }
    }

    // ===== Visibility Checks =====
    public boolean isNoResultsMessageDisplayed() {
        return isDisplayed(noResultsMsg);
    }

    public boolean isPageHeadingDisplayed() {
        return isDisplayed(pageHeading);
    }

    public String getPageHeadingText() {
        try { return getText(pageHeading); }
        catch (Exception e) { return ""; }
    }

    public boolean areAllProductsComplete() {
        List<WebElement> cards = getAllProductCards();
        if (cards.isEmpty()) return false;
        for (WebElement card : cards) {
            if (getProductName(card).isEmpty())       return false;
            if (getProductPrice(card).isEmpty())      return false;
            if (!isProductImageDisplayed(card))       return false;
        }
        return true;
    }

    public List<String> getAllProductNames() {
        List<String> names = new ArrayList<>();
        for (WebElement card : getAllProductCards()) {
            String name = getProductName(card);
            if (!name.isEmpty()) names.add(name);
        }
        return names;
    }

    public List<String> getAllProductPrices() {
        List<String> prices = new ArrayList<>();
        for (WebElement card : getAllProductCards()) {
            String price = getProductPrice(card);
            if (!price.isEmpty()) prices.add(price);
        }
        return prices;
    }
}