package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;
import java.util.List;

/**
 * Page Object: Admin Dashboard (/admin)
 *
 * Tabs: Overview | Products | Categories | Brands | Orders | Users
 * Actions: Add / Edit / Delete Product, Category, Brand
 *          Update Order Status | Update User Role | Delete User
 */
public class AdminPage extends BasePage {

    protected static final Logger log = LogManager.getLogger(AdminPage.class);

    // ── Page header ───────────────────────────────────────────────────────────
    private static final By ADMIN_HEADING =
        By.xpath("//h1[contains(.,'Admin') or contains(.,'Dashboard')]" +
                 " | //h2[contains(.,'Admin') or contains(.,'Dashboard')]");

    private static final By REFRESH_BUTTON =
        By.xpath("//button[.//span[text()='Refresh']]");

    // ── Stats cards ───────────────────────────────────────────────────────────
    private static final By STAT_CARDS =
        By.cssSelector("div[class*='rounded-2xl'][class*='shadow-lg']");

    // ── Quick action buttons ──────────────────────────────────────────────────
    private static final By BTN_ADD_PRODUCT =
        By.xpath("//button[.//span[text()='Add Product']]");

    private static final By BTN_ADD_CATEGORY =
        By.xpath("//button[.//span[text()='Add Category']]");

    private static final By BTN_ADD_BRAND =
        By.xpath("//button[.//span[text()='Add Brand']]");

    // ── Tabs ──────────────────────────────────────────────────────────────────
    private static final By TAB_OVERVIEW =
        By.xpath("//button[.//span[text()='Overview']]");

    private static final By TAB_PRODUCTS =
        By.xpath("//button[.//span[text()='Products']]");

    private static final By TAB_CATEGORIES =
        By.xpath("//button[.//span[text()='Categories']]");

    private static final By TAB_BRANDS =
        By.xpath("//button[.//span[text()='Brands']]");

    private static final By TAB_ORDERS =
        By.xpath("//button[.//span[text()='Orders']]");

    private static final By TAB_USERS =
        By.xpath("//button[.//span[text()='Users']]");

    // ── Search ────────────────────────────────────────────────────────────────
    private static final By SEARCH_INPUT =
        By.cssSelector("input[placeholder*='Search']");

    // ── Product table ─────────────────────────────────────────────────────────
    private static final By PRODUCT_TABLE_ROWS =
        By.cssSelector("table tbody tr");

    private static final By PRODUCT_EDIT_BUTTONS =
        By.cssSelector("button[class*='text-indigo-600'] svg");    // FiEdit2

    private static final By PRODUCT_DELETE_BUTTONS =
        By.cssSelector("button[class*='text-red-500'] svg");       // FiTrash2

    // ── Modal ─────────────────────────────────────────────────────────────────
    private static final By MODAL_HEADING =
        By.xpath("//h3[contains(.,'Add') or contains(.,'Edit')]");

    private static final By MODAL_CLOSE =
        By.xpath("//button[.//*[name()='svg' and contains(@class,'Fi')]][@class[contains(.,'text-gray')]]");

    // Product form fields
    private static final By FORM_PRODUCT_NAME =
        By.xpath("//input[@placeholder='Product name']");

    private static final By FORM_DESCRIPTION =
        By.xpath("//textarea[@placeholder='Product description']");

    private static final By FORM_SKU =
        By.xpath("//input[@placeholder='SKU']");

    private static final By FORM_BASE_PRICE =
        By.xpath("//input[@placeholder='Base price']");

    private static final By FORM_SELLING_PRICE =
        By.xpath("//input[@placeholder='Selling price']");

    private static final By FORM_THUMBNAIL =
        By.xpath("//input[@placeholder='Thumbnail URL']");

    private static final By FORM_CATEGORY =
        By.xpath("//select[option[text()='Select Category']]");

    private static final By FORM_BRAND =
        By.xpath("//select[option[text()='Select Brand']]");

    private static final By FORM_STATUS =
        By.xpath("//select[option[@value='ACTIVE']]");

    // Category form fields
    private static final By FORM_CATEGORY_NAME =
        By.xpath("//input[@placeholder='Category name']");

    private static final By FORM_CATEGORY_DESC =
        By.xpath("//textarea[@placeholder='Category description']");

    // Brand form fields
    private static final By FORM_BRAND_NAME =
        By.xpath("//input[@placeholder='Brand name']");

    // Form submit
    private static final By FORM_SUBMIT_BUTTON =
        By.cssSelector("button[type='submit']");

    // ── Delete confirm modal ──────────────────────────────────────────────────
    private static final By DELETE_CONFIRM_HEADING =
        By.xpath("//h3[contains(.,'Delete')]");

    private static final By DELETE_CONFIRM_BTN =
        By.xpath("//button[text()='Delete']");

    private static final By DELETE_CANCEL_BTN =
        By.xpath("//button[text()='Cancel']");

    // ── Order status modal ────────────────────────────────────────────────────
    private static final By ORDER_STATUS_SELECT =
        By.xpath("//select[option[@value='PENDING']]");

    private static final By ORDER_TRACKING_INPUT =
        By.xpath("//input[@placeholder='Tracking number']");

    private static final By ORDER_STATUS_SUBMIT =
        By.xpath("//button[text()='Update Status']");

    // ── User role modal ───────────────────────────────────────────────────────
    private static final By USER_ROLE_SELECT =
        By.xpath("//select[option[@value='USER']]");

    private static final By USER_ROLE_SUBMIT =
        By.xpath("//button[text()='Update Role']");

    // ── Overview section ──────────────────────────────────────────────────────
    private static final By OVERVIEW_RECENT_ORDERS =
        By.xpath("//h3[text()='Recent Orders']");

    private static final By OVERVIEW_QUICK_OVERVIEW =
        By.xpath("//h3[text()='Quick Overview']");

    // ── Loading skeleton ──────────────────────────────────────────────────────
    private static final By LOADING_SKELETON =
        By.cssSelector(".skeleton");

    // ── Empty states ──────────────────────────────────────────────────────────
    private static final By NO_PRODUCTS_MSG =
        By.xpath("//*[contains(text(),'No products')]");

    public AdminPage(WebDriver driver) {
        super(driver);
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/admin");
        waitForDashboardLoad();
    }

    public void waitForDashboardLoad() {
        // Step 1: wait for page to be ready
        WaitUtils.waitForPageReady(driver);
        // Step 2: wait for ANY admin content OR login redirect
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(d -> {
                    String url = d.getCurrentUrl();
                    String src = d.getPageSource();
                    return url.contains("/admin") && (
                        src.contains("Admin") ||
                        src.contains("Dashboard") ||
                        src.contains("Products") ||
                        !d.findElements(LOADING_SKELETON).isEmpty()
                    );
                });
        } catch (TimeoutException e) {
            log.warn("Admin dashboard load wait timed out — proceeding anyway");
        }
        // Step 3: wait for skeletons to clear
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(LOADING_SKELETON).isEmpty());
        } catch (TimeoutException ignored) {}
        log.info("Admin dashboard loaded | URL: " + driver.getCurrentUrl());
    }

    public boolean isOnAdminPage() {
        return driver.getCurrentUrl().contains("/admin");
    }

    public boolean isAdminHeadingVisible() {
        if (isDisplayed(ADMIN_HEADING)) return true;
        // Fallback: check page source for admin keywords
        String src = driver.getPageSource();
        return src.contains("Admin Dashboard") || src.contains("admin") &&
               driver.getCurrentUrl().contains("/admin");
    }

    // =========================================================================
    // Tabs
    // =========================================================================

    public void clickTab(String tabName) {
        By tab;
        switch (tabName.toLowerCase()) {
            case "products":    tab = TAB_PRODUCTS;    break;
            case "categories":  tab = TAB_CATEGORIES;  break;
            case "brands":      tab = TAB_BRANDS;      break;
            case "orders":      tab = TAB_ORDERS;      break;
            case "users":       tab = TAB_USERS;       break;
            default:            tab = TAB_OVERVIEW;
        }
        WaitUtils.waitForClickable(driver, tab).click();
        waitForTabLoad();
        log.info("Clicked tab: " + tabName);
    }

    private void waitForTabLoad() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(LOADING_SKELETON).isEmpty());
        } catch (TimeoutException ignored) {}
    }

    // =========================================================================
    // Stats
    // =========================================================================

    public int getStatCardCount() {
        return driver.findElements(STAT_CARDS).size();
    }

    public boolean isOverviewRecentOrdersVisible() {
        return isDisplayed(OVERVIEW_RECENT_ORDERS);
    }

    public boolean isQuickOverviewVisible() {
        return isDisplayed(OVERVIEW_QUICK_OVERVIEW);
    }

    // =========================================================================
    // Search
    // =========================================================================

    public void search(String term) {
        WebElement input = WaitUtils.waitForVisible(driver, SEARCH_INPUT);
        input.clear();
        input.sendKeys(term);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    public void clearSearch() {
        try {
            WebElement input = driver.findElement(SEARCH_INPUT);
            input.clear();
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // Quick action buttons
    // =========================================================================

    public void clickAddProduct() {
        WaitUtils.waitForClickable(driver, BTN_ADD_PRODUCT).click();
        WaitUtils.waitForVisible(driver, MODAL_HEADING);
    }

    public void clickAddCategory() {
        WaitUtils.waitForClickable(driver, BTN_ADD_CATEGORY).click();
        WaitUtils.waitForVisible(driver, MODAL_HEADING);
    }

    public void clickAddBrand() {
        WaitUtils.waitForClickable(driver, BTN_ADD_BRAND).click();
        WaitUtils.waitForVisible(driver, MODAL_HEADING);
    }

    // =========================================================================
    // Product table
    // =========================================================================

    public int getProductRowCount() {
        return driver.findElements(PRODUCT_TABLE_ROWS).size();
    }

    public boolean isProductInTable(String name) {
        return driver.findElements(PRODUCT_TABLE_ROWS).stream()
            .anyMatch(r -> r.getText().toLowerCase().contains(name.toLowerCase()));
    }

    public void clickEditProduct(int rowIndex) {
        List<WebElement> rows = driver.findElements(PRODUCT_TABLE_ROWS);
        rows.get(rowIndex)
            .findElement(By.cssSelector("button[class*='text-indigo-600']"))
            .click();
        WaitUtils.waitForVisible(driver, MODAL_HEADING);
    }

    public void clickDeleteProduct(int rowIndex) {
        List<WebElement> rows = driver.findElements(PRODUCT_TABLE_ROWS);
        rows.get(rowIndex)
            .findElement(By.cssSelector("button[class*='text-red-500']"))
            .click();
        WaitUtils.waitForVisible(driver, DELETE_CONFIRM_HEADING);
    }

    // =========================================================================
    // Product Form
    // =========================================================================

    public void fillProductForm(String name, String description, String sku,
                                 String basePrice, String sellingPrice,
                                 String thumbnailUrl) {
        fillField(FORM_PRODUCT_NAME,   name);
        fillField(FORM_DESCRIPTION,    description);
        fillField(FORM_SKU,            sku);
        fillField(FORM_BASE_PRICE,     basePrice);
        fillField(FORM_SELLING_PRICE,  sellingPrice);
        if (!thumbnailUrl.isEmpty()) fillField(FORM_THUMBNAIL, thumbnailUrl);
        log.info("Product form filled: " + name);
    }

    public void selectProductCategory(String categoryName) {
        selectFromDropdown(FORM_CATEGORY, categoryName);
    }

    public void selectProductBrand(String brandName) {
        selectFromDropdown(FORM_BRAND, brandName);
    }

    public void submitForm() {
        WaitUtils.waitForClickable(driver, FORM_SUBMIT_BUTTON).click();
        // Wait for modal to close
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(MODAL_HEADING).isEmpty());
        } catch (TimeoutException ignored) {}
        waitForTabLoad();
    }

    public boolean isModalOpen() {
        return isDisplayed(MODAL_HEADING);
    }

    public String getModalHeadingText() {
        try { return driver.findElement(MODAL_HEADING).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    // =========================================================================
    // Category Form
    // =========================================================================

    public void fillCategoryForm(String name, String description) {
        fillField(FORM_CATEGORY_NAME, name);
        fillField(FORM_CATEGORY_DESC, description);
        log.info("Category form filled: " + name);
    }

    // =========================================================================
    // Brand Form
    // =========================================================================

    public void fillBrandForm(String name, String description) {
        fillField(FORM_BRAND_NAME, name);
        // Description field may share textarea locator
        try {
            List<WebElement> textareas = driver.findElements(
                By.xpath("//textarea[contains(@placeholder,'description')]"));
            if (!textareas.isEmpty()) {
                textareas.get(0).clear();
                textareas.get(0).sendKeys(description);
            }
        } catch (Exception ignored) {}
        log.info("Brand form filled: " + name);
    }

    // =========================================================================
    // Delete Confirm
    // =========================================================================

    public boolean isDeleteConfirmVisible() {
        return isDisplayed(DELETE_CONFIRM_HEADING);
    }

    public void confirmDelete() {
        WaitUtils.waitForClickable(driver, DELETE_CONFIRM_BTN).click();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(DELETE_CONFIRM_HEADING).isEmpty());
        } catch (TimeoutException ignored) {}
        waitForTabLoad();
    }

    public void cancelDelete() {
        click(DELETE_CANCEL_BTN);
    }

    // =========================================================================
    // Orders Tab
    // =========================================================================

    public void clickEditOrderStatus(int rowIndex) {
        List<WebElement> editBtns = driver.findElements(
            By.cssSelector("table tbody tr button[class*='text-indigo-600']"));
        if (rowIndex < editBtns.size()) {
            editBtns.get(rowIndex).click();
        }
    }

    public void selectOrderStatus(String status) {
        selectFromDropdown(ORDER_STATUS_SELECT, status);
    }

    public void submitOrderStatus() {
        WaitUtils.waitForClickable(driver, ORDER_STATUS_SUBMIT).click();
        waitForTabLoad();
    }

    // =========================================================================
    // Visibility checks
    // =========================================================================

    public boolean isAddProductButtonVisible()  { return isDisplayed(BTN_ADD_PRODUCT); }
    public boolean isAddCategoryButtonVisible() { return isDisplayed(BTN_ADD_CATEGORY); }
    public boolean isAddBrandButtonVisible()    { return isDisplayed(BTN_ADD_BRAND); }
    public boolean isSearchVisible()            { return isDisplayed(SEARCH_INPUT); }
    public boolean isRefreshButtonVisible()     { return isDisplayed(REFRESH_BUTTON); }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void fillField(By locator, String value) {
        if (value == null || value.isEmpty()) return;
        try {
            WebElement el = WaitUtils.waitForVisible(driver, locator);
            el.clear();
            el.sendKeys(value);
        } catch (Exception e) {
            log.warn("fillField failed for " + locator + ": " + e.getMessage());
        }
    }

    private void selectFromDropdown(By locator, String visibleText) {
        try {
            new Select(WaitUtils.waitForVisible(driver, locator))
                .selectByVisibleText(visibleText);
        } catch (Exception e) {
            log.warn("selectFromDropdown failed: " + e.getMessage());
        }
    }
}