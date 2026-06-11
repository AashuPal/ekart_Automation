package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.List;

/**
 * Page Object: Admin Dashboard — Products Tab (https://ekartms.netlify.app/admin)
 *
 * Covers:
 *  - Navigating to /admin and clicking the Products tab
 *  - Opening Add Product modal via "Add Product" button
 *  - Filling and submitting the product form
 *  - Verifying newly added product appears in the admin table
 */
public class AdminProductPage extends BasePage {

    // ── Admin nav tabs ────────────────────────────────────────────────────────
    private static final By PRODUCTS_TAB =
        By.xpath("//button[.//span[text()='Products']]");

    private static final By DASHBOARD_TAB =
        By.xpath("//button[.//span[text()='Dashboard']]");

    // ── Add Product button ────────────────────────────────────────────────────
    private static final By ADD_PRODUCT_BUTTON =
        By.xpath("//button[.//span[text()='Add Product']]");

    // ── Product modal form ────────────────────────────────────────────────────
    private static final By MODAL_CONTAINER =
        By.xpath("//div[contains(@class,'fixed') and contains(@class,'inset-0')]");

    private static final By MODAL_TITLE =
        By.xpath("//h2[contains(text(),'Add Product') or contains(text(),'Edit Product')]");

    private static final By FIELD_NAME =
        By.xpath("//input[@placeholder='Product name']");

    private static final By FIELD_DESCRIPTION =
        By.xpath("//textarea[@placeholder='Product description']");

    private static final By FIELD_SKU =
        By.xpath("//input[@placeholder='SKU']");

    private static final By FIELD_BASE_PRICE =
        By.xpath("//input[@placeholder='Base price']");

    private static final By FIELD_SELLING_PRICE =
        By.xpath("//input[@placeholder='Selling price']");

    private static final By FIELD_DISCOUNT =
        By.xpath("//input[@placeholder='Discount %']");

    private static final By FIELD_THUMBNAIL =
        By.xpath("//input[@placeholder='Thumbnail URL']");

    private static final By FIELD_INITIAL_STOCK =
        By.xpath("//input[@placeholder='Initial stock']");

    private static final By SELECT_CATEGORY =
        By.xpath("//select[option[text()='Select Category']]");

    private static final By SELECT_BRAND =
        By.xpath("//select[option[text()='Select Brand']]");

    private static final By SELECT_STATUS =
        By.xpath("//select[option[text()='ACTIVE']]");

    private static final By SUBMIT_BUTTON =
        By.xpath("//button[@type='submit' and (contains(text(),'Add Product') or contains(text(),'Update'))]");

    private static final By CANCEL_BUTTON =
        By.xpath("//button[text()='Cancel']");

    private static final By CLOSE_MODAL_BUTTON =
        By.xpath("//button[.//*[name()='svg' and @data-testid='FiX']]");

    // ── Product table / list ──────────────────────────────────────────────────
    private static final By PRODUCT_TABLE_ROWS =
        By.cssSelector("div.overflow-x-auto table tbody tr, " +
                       "div.space-y-3 > div");      // card layout fallback

    private static final By PRODUCT_SEARCH_INPUT =
        By.cssSelector("input[placeholder*='Search']");

    // ── Loading / empty ───────────────────────────────────────────────────────
    private static final By LOADING_SPINNER =
        By.cssSelector(".animate-spin");

    private static final By EMPTY_TABLE_MSG =
        By.xpath("//*[contains(text(),'No products')]");

    public AdminProductPage(WebDriver driver) {
        super(driver);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation
    // ────────────────────────────────────────────────────────────────────────

    public void navigateToAdmin(String baseUrl) {
        driver.get(baseUrl + "/admin");
        waitForAdminLoad();
    }

    public void waitForAdminLoad() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.until(d -> !d.findElements(PRODUCTS_TAB).isEmpty() ||
                        !d.findElements(DASHBOARD_TAB).isEmpty());
    }

    /** Click the Products tab in the admin sidebar. */
    public void openProductsTab() {
        WaitUtils.waitForClickable(driver, PRODUCTS_TAB).click();
        waitForProductsTabLoad();
    }

    private void waitForProductsTabLoad() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        // Wait until Add Product button appears OR table has rows OR empty message
        wait.until(d ->
            !d.findElements(ADD_PRODUCT_BUTTON).isEmpty() ||
            !d.findElements(EMPTY_TABLE_MSG).isEmpty()
        );
        // Also wait for spinner to disappear
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(LOADING_SPINNER).isEmpty());
        } catch (TimeoutException ignored) {}
    }

    // ────────────────────────────────────────────────────────────────────────
    // Add Product
    // ────────────────────────────────────────────────────────────────────────

    /** Click the "Add Product" button to open the modal. */
    public void clickAddProduct() {
        WaitUtils.waitForClickable(driver, ADD_PRODUCT_BUTTON).click();
        WaitUtils.waitForVisible(driver, MODAL_TITLE);
    }

    /**
     * Fill and submit the Add Product form.
     *
     * @param name         Product display name
     * @param description  Product description
     * @param sku          Stock-keeping unit (e.g. "PROD-001")
     * @param basePrice    Original price (e.g. "999")
     * @param sellingPrice Discounted price (e.g. "799")
     * @param thumbnailUrl Image URL (can be empty to skip)
     * @param initialStock Stock quantity (e.g. "50")
     */
    public void fillProductForm(String name, String description, String sku,
                                String basePrice, String sellingPrice,
                                String thumbnailUrl, String initialStock) {
        type(FIELD_NAME,         name);
        type(FIELD_DESCRIPTION,  description);
        type(FIELD_SKU,          sku);
        type(FIELD_BASE_PRICE,   basePrice);
        type(FIELD_SELLING_PRICE, sellingPrice);

        if (!thumbnailUrl.isEmpty()) {
            type(FIELD_THUMBNAIL, thumbnailUrl);
        }
        if (!initialStock.isEmpty()) {
            try { type(FIELD_INITIAL_STOCK, initialStock); }
            catch (Exception ignored) {}
        }
    }

    /** Select category by visible text if the dropdown is present. */
    public void selectCategory(String categoryName) {
        try {
            WebElement sel = WaitUtils.waitForVisible(driver, SELECT_CATEGORY);
            new Select(sel).selectByVisibleText(categoryName);
        } catch (Exception e) {
            log.warn("Could not select category '" + categoryName + "': " + e.getMessage());
        }
    }

    /** Select brand by visible text if the dropdown is present. */
    public void selectBrand(String brandName) {
        try {
            WebElement sel = WaitUtils.waitForVisible(driver, SELECT_BRAND);
            new Select(sel).selectByVisibleText(brandName);
        } catch (Exception e) {
            log.warn("Could not select brand '" + brandName + "': " + e.getMessage());
        }
    }

    /** Submit the product form (clicks the "Add Product" submit button). */
    public void submitProductForm() {
        WaitUtils.waitForClickable(driver, SUBMIT_BUTTON).click();
        // Wait for modal to close
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(MODAL_TITLE).isEmpty());
        } catch (TimeoutException ignored) {}
        waitForProductsTabLoad();
    }

    public void cancelProductForm() {
        if (isDisplayed(CANCEL_BUTTON)) click(CANCEL_BUTTON);
    }

    // ────────────────────────────────────────────────────────────────────────
    // State checks
    // ────────────────────────────────────────────────────────────────────────

    public boolean isAddProductModalOpen() {
        return isDisplayed(MODAL_TITLE);
    }

    public boolean isAddProductButtonVisible() {
        return isDisplayed(ADD_PRODUCT_BUTTON);
    }

    /** Returns true if a product with the given name appears in the admin table. */
    public boolean isProductInTable(String productName) {
        try {
            // Quick search if search bar exists
            if (isDisplayed(PRODUCT_SEARCH_INPUT)) {
                type(PRODUCT_SEARCH_INPUT, productName);
                try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            }
            List<WebElement> rows = driver.findElements(PRODUCT_TABLE_ROWS);
            for (WebElement row : rows) {
                if (row.getText().toLowerCase().contains(productName.toLowerCase())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("isProductInTable error: " + e.getMessage());
        }
        return false;
    }

    public int getProductRowCount() {
        return driver.findElements(PRODUCT_TABLE_ROWS).size();
    }

    public boolean isOnAdminPage() {
        return driver.getCurrentUrl().contains("/admin");
    }
}