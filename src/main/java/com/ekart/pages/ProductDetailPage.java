package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;

/**
 * Page Object: Product Detail Page (https://ekartms.netlify.app/product/<id>)
 *
 * Covers:
 *  - Product name, brand, price, discount badge
 *  - Image gallery
 *  - Quantity selector
 *  - Add to Cart / Buy Now buttons
 *  - Stock status
 *  - Reviews section
 */
public class ProductDetailPage extends BasePage {

    // ── Product info ─────────────────────────────────────────────────────────
    private static final By PRODUCT_NAME =
        By.cssSelector("h1");

    private static final By BRAND_LABEL =
        By.xpath("//span[contains(@class,'text-indigo-600') and contains(@class,'uppercase')]");

    private static final By SELLING_PRICE =
        By.xpath("//span[contains(@class,'text-4xl') and contains(@class,'font-extrabold')]");

    private static final By BASE_PRICE =
        By.xpath("//span[contains(@class,'line-through')]");

    private static final By DISCOUNT_BADGE =
        By.xpath("//span[contains(@class,'badge-danger')]");

    private static final By DESCRIPTION =
        By.cssSelector("p.text-gray-600.leading-relaxed");

    private static final By CATEGORY_BREADCRUMB =
        By.xpath("//nav//span[@class='text-gray-400']");

    // ── Main product image ────────────────────────────────────────────────────
    private static final By MAIN_IMAGE =
        By.cssSelector("div.aspect-square img");

    // ── Stock status ──────────────────────────────────────────────────────────
    private static final By IN_STOCK_BADGE =
        By.xpath("//span[text()='In Stock' or contains(text(),'available')]");

    private static final By OUT_OF_STOCK_BADGE =
        By.xpath("//span[text()='Out of Stock']");

    private static final By LOW_STOCK_BADGE =
        By.xpath("//span[contains(text(),'left in stock')]");

    // ── Quantity selector ─────────────────────────────────────────────────────
    private static final By QUANTITY_VALUE =
        By.xpath("//span[contains(@class,'font-bold') and contains(@class,'min-w')]");

    private static final By QUANTITY_PLUS =
        By.xpath("//button[.//*[name()='svg' and .//*[@d and contains(@d,'M12 5v14')]]]");

    private static final By QUANTITY_MINUS =
        By.xpath("//button[.//*[name()='svg' and .//*[@d and contains(@d,'M5 12h14')]]]");

    // ── Action buttons ────────────────────────────────────────────────────────
    private static final By ADD_TO_CART_BUTTON =
        By.xpath("//button[.//span[text()='Add to Cart']]");

    private static final By BUY_NOW_BUTTON =
        By.xpath("//button[text()='Buy Now']");

    // ── Reviews ───────────────────────────────────────────────────────────────
    private static final By REVIEWS_HEADING =
        By.xpath("//h2[text()='Customer Reviews']");

    private static final By WRITE_REVIEW_BUTTON =
        By.xpath("//button[.//span[text()='Write a Review']]");

    private static final By REVIEW_ITEMS =
        By.cssSelector("div.border-b.border-gray-100");

    // ── Error / not-found state ───────────────────────────────────────────────
    private static final By NOT_FOUND_HEADING =
        By.xpath("//h2[text()='Product Not Found']");

    private static final By BACK_TO_HOME =
        By.xpath("//button[.//span[text()='Back to Home']]");

    // ── Breadcrumb back button ────────────────────────────────────────────────
    private static final By BREADCRUMB_HOME =
        By.xpath("//nav//button[text()='Home']");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation
    // ────────────────────────────────────────────────────────────────────────

    public void waitForLoad() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d ->
            !d.findElements(PRODUCT_NAME).isEmpty() ||
            !d.findElements(NOT_FOUND_HEADING).isEmpty()
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // Product info getters
    // ────────────────────────────────────────────────────────────────────────

    public String getProductName() {
        try { return WaitUtils.waitForVisible(driver, PRODUCT_NAME).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getBrandName() {
        try { return driver.findElement(BRAND_LABEL).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getSellingPrice() {
        try { return driver.findElement(SELLING_PRICE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getBasePrice() {
        try { return driver.findElement(BASE_PRICE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getDiscountBadgeText() {
        try { return driver.findElement(DISCOUNT_BADGE).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getDescription() {
        try { return driver.findElement(DESCRIPTION).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Image
    // ────────────────────────────────────────────────────────────────────────

    public boolean isMainImageVisible() {
        try {
            WebElement img = driver.findElement(MAIN_IMAGE);
            return img.isDisplayed() && !img.getAttribute("src").isEmpty();
        } catch (Exception e) { return false; }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Stock status
    // ────────────────────────────────────────────────────────────────────────

    public boolean isInStock() { return isDisplayed(IN_STOCK_BADGE); }
    public boolean isOutOfStock() { return isDisplayed(OUT_OF_STOCK_BADGE); }
    public boolean isLowStock() { return isDisplayed(LOW_STOCK_BADGE); }

    public boolean isAnyStockStatusVisible() {
        return isInStock() || isOutOfStock() || isLowStock();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Quantity selector
    // ────────────────────────────────────────────────────────────────────────

    public int getQuantity() {
        try { return Integer.parseInt(driver.findElement(QUANTITY_VALUE).getText().trim()); }
        catch (Exception e) { return -1; }
    }

    public void increaseQuantity() { click(QUANTITY_PLUS); }
    public void decreaseQuantity() { click(QUANTITY_MINUS); }

    // ────────────────────────────────────────────────────────────────────────
    // Actions
    // ────────────────────────────────────────────────────────────────────────

    public void clickAddToCart() {
        WaitUtils.waitForClickable(driver, ADD_TO_CART_BUTTON).click();
    }

    public void clickBuyNow() {
        WaitUtils.waitForClickable(driver, BUY_NOW_BUTTON).click();
    }

    public boolean isAddToCartButtonVisible() { return isDisplayed(ADD_TO_CART_BUTTON); }
    public boolean isBuyNowButtonVisible()    { return isDisplayed(BUY_NOW_BUTTON); }

    // ────────────────────────────────────────────────────────────────────────
    // Reviews
    // ────────────────────────────────────────────────────────────────────────

    public boolean isReviewsSectionVisible() { return isDisplayed(REVIEWS_HEADING); }

    public int getReviewCount() {
        return driver.findElements(REVIEW_ITEMS).size();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Error state
    // ────────────────────────────────────────────────────────────────────────

    public boolean isProductNotFound() { return isDisplayed(NOT_FOUND_HEADING); }

    // ────────────────────────────────────────────────────────────────────────
    // URL helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Returns the product UUID from the current URL. */
    public String getProductIdFromUrl() {
        String url = driver.getCurrentUrl();
        if (url.contains("/product/")) {
            return url.substring(url.lastIndexOf("/product/") + 9);
        }
        return "";
    }

    public boolean isOnProductDetailPage() {
        return driver.getCurrentUrl().contains("/product/");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation
    // ────────────────────────────────────────────────────────────────────────

    public void goBackToHome() {
        if (isDisplayed(BREADCRUMB_HOME)) click(BREADCRUMB_HOME);
        else if (isDisplayed(BACK_TO_HOME)) click(BACK_TO_HOME);
    }
}