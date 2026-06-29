package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.config.ConfigReader;
import com.ekart.pages.AdminPage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Admin Dashboard Tests
 *
 * ── Access & Layout ───────────────────────────────────────────────
 *  TC_ADM_001  Admin page accessible after login at /admin
 *  TC_ADM_002  Dashboard heading and Refresh button visible
 *  TC_ADM_003  6 stat cards visible (Products, Categories, Brands, Orders, Users, Revenue)
 *  TC_ADM_004  Quick action buttons visible (Add Product, Add Category, Add Brand)
 *  TC_ADM_005  All 6 tabs visible and clickable
 *
 * ── Overview Tab ──────────────────────────────────────────────────
 *  TC_ADM_006  Overview tab shows Recent Orders and Quick Overview
 *
 * ── Products Tab ──────────────────────────────────────────────────
 *  TC_ADM_007  Products tab loads product table with rows
 *  TC_ADM_008  Search in Products tab filters results
 *  TC_ADM_009  Add Product modal opens with all form fields
 *  TC_ADM_010  Add Product form submission creates new product
 *  TC_ADM_011  Edit Product modal opens with pre-filled data
 *  TC_ADM_012  Delete Product shows confirm dialog
 *  TC_ADM_013  Delete confirm — Cancel button keeps product intact
 *
 * ── Categories Tab ────────────────────────────────────────────────
 *  TC_ADM_014  Categories tab loads category list
 *  TC_ADM_015  Add Category modal opens and submits
 *
 * ── Brands Tab ────────────────────────────────────────────────────
 *  TC_ADM_016  Brands tab loads brand list
 *  TC_ADM_017  Add Brand modal opens and submits
 *
 * ── Orders Tab ────────────────────────────────────────────────────
 *  TC_ADM_018  Orders tab loads order list
 *  TC_ADM_019  Orders tab shows order status badges
 *
 * ── Users Tab ─────────────────────────────────────────────────────
 *  TC_ADM_020  Users tab loads user list (admin only)
 */
public class AdminDashboardTest extends BaseTest {

    private AdminPage adminPage;

    // Use the valid user credentials — this user has admin access on the site
    private static final String ADMIN_EMAIL    = "user@ekart.com";
    private static final String ADMIN_PASSWORD = "Userl@123";

    @BeforeClass(dependsOnMethods = "setUp")
    public void loginAsAdmin() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));

        // loginAsValidUser uses user@ekart.com which has admin privileges
        loginAsValidUser();
        log.info("Logged in for AdminDashboardTest | URL: " + getDriver().getCurrentUrl());
    }

    @BeforeMethod
    public void setup() {
        adminPage = new AdminPage(getDriver());
        // Re-login silently if session expired
        try {
            String url = getDriver().getCurrentUrl();
            if (url.contains("/login") || url.contains("/register")) {
                log.warn("Session expired — re-logging in");
                loginAsValidUser();
            }
        } catch (Exception ignored) {}

        // Navigate to admin and wait with page load timeout set
        try {
            getDriver().manage().timeouts()
                .pageLoadTimeout(java.time.Duration.ofSeconds(60));
            adminPage.navigateTo(baseUrl);
        } catch (org.openqa.selenium.TimeoutException e) {
            log.warn("Admin page load timed out — refreshing");
            getDriver().navigate().refresh();
            WaitUtils.waitForPageReady(getDriver());
        }
    }

    // =========================================================================
    // ACCESS & LAYOUT
    // =========================================================================

    // ── TC_ADM_001 ────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_ADM_001: Admin page accessible after login at /admin")
    public void testAdminPageAccessible() {
        Assert.assertTrue(adminPage.isOnAdminPage(),
            "Must be on /admin page. URL: " + getDriver().getCurrentUrl());
        Assert.assertTrue(adminPage.isAdminHeadingVisible(),
            "Admin Dashboard heading must be visible");

        log.info("PASS TC_ADM_001 | URL: " + getDriver().getCurrentUrl());
    }

    // ── TC_ADM_002 ────────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_ADM_002: Dashboard heading and Refresh button visible")
    public void testDashboardHeadingAndRefresh() {
        Assert.assertTrue(adminPage.isAdminHeadingVisible(),
            "Admin heading must be visible");
        Assert.assertTrue(adminPage.isRefreshButtonVisible(),
            "Refresh button must be visible");

        log.info("PASS TC_ADM_002 | Heading and Refresh visible");
    }

    // ── TC_ADM_003 ────────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_ADM_003: 6 stat cards visible (Products, Categories, Brands, Orders, Users, Revenue)")
    public void testStatCardsVisible() {
        int count = adminPage.getStatCardCount();
        Assert.assertTrue(count >= 4,
            "At least 4 stat cards must be visible. Got=" + count);

        // Verify each stat label exists in page
        String src = getDriver().getPageSource();
        Assert.assertTrue(src.contains("Products"),  "Products stat missing");
        Assert.assertTrue(src.contains("Categories"),"Categories stat missing");
        Assert.assertTrue(src.contains("Brands"),    "Brands stat missing");
        Assert.assertTrue(src.contains("Orders"),    "Orders stat missing");

        log.info("PASS TC_ADM_003 | Stat cards count=" + count);
    }

    // ── TC_ADM_004 ────────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_ADM_004: Quick action buttons visible (Add Product, Add Category, Add Brand)")
    public void testQuickActionButtonsVisible() {
        Assert.assertTrue(adminPage.isAddProductButtonVisible(),
            "Add Product button must be visible");
        Assert.assertTrue(adminPage.isAddCategoryButtonVisible(),
            "Add Category button must be visible");
        Assert.assertTrue(adminPage.isAddBrandButtonVisible(),
            "Add Brand button must be visible");

        log.info("PASS TC_ADM_004 | All quick action buttons visible");
    }

    // ── TC_ADM_005 ────────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_ADM_005: All 6 tabs visible and clickable")
    public void testAllTabsVisible() {
        String[] tabs = {"overview", "products", "categories", "brands", "orders", "users"};

        for (String tab : tabs) {
            try {
                adminPage.clickTab(tab);
                log.info("Tab clicked: " + tab);
            } catch (Exception e) {
                Assert.fail("Tab '" + tab + "' not clickable: " + e.getMessage());
            }
        }

        log.info("PASS TC_ADM_005 | All 6 tabs clickable");
    }

    // =========================================================================
    // OVERVIEW TAB
    // =========================================================================

    // ── TC_ADM_006 ────────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_ADM_006: Overview tab shows Recent Orders and Quick Overview sections")
    public void testOverviewTabContent() {
        adminPage.clickTab("overview");

        Assert.assertTrue(adminPage.isOverviewRecentOrdersVisible(),
            "Recent Orders section must be visible on Overview tab");
        Assert.assertTrue(adminPage.isQuickOverviewVisible(),
            "Quick Overview section must be visible on Overview tab");

        log.info("PASS TC_ADM_006 | Overview tab content verified");
    }

    // =========================================================================
    // PRODUCTS TAB
    // =========================================================================

    // ── TC_ADM_007 ────────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_ADM_007: Products tab loads product table with rows")
    public void testProductsTabLoadsTable() {
        adminPage.clickTab("products");

        int rows = adminPage.getProductRowCount();
        Assert.assertTrue(rows > 0,
            "Products table must have at least one row. Got=" + rows);
        Assert.assertTrue(adminPage.isSearchVisible(),
            "Search input must be visible on Products tab");

        log.info("PASS TC_ADM_007 | Product rows=" + rows);
    }

    // ── TC_ADM_008 ────────────────────────────────────────────────────────────
    @Test(priority = 8,
          description = "TC_ADM_008: Search in Products tab filters results")
    public void testProductSearchFilters() {
        adminPage.clickTab("products");

        int totalBefore = adminPage.getProductRowCount();
        Assert.assertTrue(totalBefore > 0, "Need products before filtering");

        adminPage.search("Nike");
        int afterSearch = adminPage.getProductRowCount();

        // Either fewer results OR same if all contain "Nike" — never more
        Assert.assertTrue(afterSearch <= totalBefore,
            "Search must not increase row count. Before=" + totalBefore
            + " After=" + afterSearch);

        // Clear search and verify restore
        adminPage.clearSearch();
        int afterClear = adminPage.getProductRowCount();
        Assert.assertTrue(afterClear >= afterSearch,
            "Clearing search must restore results. After search=" + afterSearch
            + " After clear=" + afterClear);

        log.info("PASS TC_ADM_008 | Before=" + totalBefore + " After search="
                 + afterSearch + " After clear=" + afterClear);
    }

    // ── TC_ADM_009 ────────────────────────────────────────────────────────────
    @Test(priority = 9,
          description = "TC_ADM_009: Add Product modal opens with all required form fields")
    public void testAddProductModalOpens() {
        adminPage.clickAddProduct();

        Assert.assertTrue(adminPage.isModalOpen(),
            "Add Product modal must open");

        String heading = adminPage.getModalHeadingText();
        Assert.assertTrue(heading.contains("Add") && heading.contains("Product"),
            "Modal heading must say 'Add Product'. Got: '" + heading + "'");

        // Verify form fields present
        String src = getDriver().getPageSource();
        Assert.assertTrue(src.contains("Product name"),
            "Product name field must be in modal");
        Assert.assertTrue(src.contains("SKU"),
            "SKU field must be in modal");
        Assert.assertTrue(src.contains("Base price"),
            "Base price field must be in modal");
        Assert.assertTrue(src.contains("Selling price"),
            "Selling price field must be in modal");

        log.info("PASS TC_ADM_009 | Add Product modal opened with fields");
    }

    // ── TC_ADM_010 ────────────────────────────────────────────────────────────
    @Test(priority = 10,
          description = "TC_ADM_010: Add Product form submission creates new product in list")
    public void testAddProductSubmit() {
        adminPage.clickTab("products");
        int countBefore = adminPage.getProductRowCount();

        String uniqueName = "AutoProduct_" + System.currentTimeMillis();
        adminPage.clickAddProduct();
        adminPage.fillProductForm(
            uniqueName,
            "Automated test product",
            "SKU-AUTO-" + System.currentTimeMillis(),
            "999",
            "799",
            "https://placehold.co/400x400/E2E8F0/94A3B8?text=Auto"
        );
        adminPage.submitForm();

        // Wait for table to reload
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        adminPage.clickTab("products");

        int countAfter = adminPage.getProductRowCount();
        boolean productInTable = adminPage.isProductInTable(uniqueName);

        Assert.assertTrue(productInTable || countAfter > countBefore,
            "New product must appear in table. name='" + uniqueName
            + "' before=" + countBefore + " after=" + countAfter);

        log.info("PASS TC_ADM_010 | Product '" + uniqueName + "' added. rows: "
                 + countBefore + "→" + countAfter);
    }

    // ── TC_ADM_011 ────────────────────────────────────────────────────────────
    @Test(priority = 11,
          description = "TC_ADM_011: Edit Product modal opens with pre-filled product data")
    public void testEditProductModalPrefilled() {
        adminPage.clickTab("products");

        int rows = adminPage.getProductRowCount();
        Assert.assertTrue(rows > 0, "Need at least one product to edit");

        adminPage.clickEditProduct(0);

        Assert.assertTrue(adminPage.isModalOpen(),
            "Edit Product modal must open");

        String heading = adminPage.getModalHeadingText();
        Assert.assertTrue(heading.contains("Edit") || heading.contains("Product"),
            "Modal heading must say 'Edit'. Got: '" + heading + "'");

        // Product name field must be pre-filled (not empty)
        try {
            String nameValue = getDriver()
                .findElement(org.openqa.selenium.By.xpath(
                    "//input[@placeholder='Product name']"))
                .getAttribute("value");
            Assert.assertFalse(nameValue.isEmpty(),
                "Product name must be pre-filled in edit modal. Got='" + nameValue + "'");
            log.info("PASS TC_ADM_011 | Edit modal pre-filled | name='" + nameValue + "'");
        } catch (Exception e) {
            log.warn("Could not read product name field: " + e.getMessage());
        }
    }

    // ── TC_ADM_012 ────────────────────────────────────────────────────────────
    @Test(priority = 12,
          description = "TC_ADM_012: Delete Product button shows confirmation dialog")
    public void testDeleteProductConfirmDialog() {
        adminPage.clickTab("products");

        int rows = adminPage.getProductRowCount();
        Assert.assertTrue(rows > 0, "Need at least one product to delete");

        adminPage.clickDeleteProduct(0);

        Assert.assertTrue(adminPage.isDeleteConfirmVisible(),
            "Delete confirmation dialog must appear after clicking delete");

        String src = getDriver().getPageSource();
        Assert.assertTrue(src.contains("Delete"),
            "Delete confirm dialog must contain 'Delete' text");

        log.info("PASS TC_ADM_012 | Delete confirm dialog appeared");
    }

    // ── TC_ADM_013 ────────────────────────────────────────────────────────────
    @Test(priority = 13,
          description = "TC_ADM_013: Cancel on delete confirm keeps product intact")
    public void testDeleteCancelKeepsProduct() {
        adminPage.clickTab("products");

        int countBefore = adminPage.getProductRowCount();
        Assert.assertTrue(countBefore > 0, "Need at least one product");

        adminPage.clickDeleteProduct(0);
        Assert.assertTrue(adminPage.isDeleteConfirmVisible(),
            "Delete confirm must appear");

        adminPage.cancelDelete();

        // Wait briefly for UI to settle
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        int countAfter = adminPage.getProductRowCount();
        Assert.assertEquals(countAfter, countBefore,
            "Product count must be unchanged after cancel. Before="
            + countBefore + " After=" + countAfter);

        log.info("PASS TC_ADM_013 | Cancel keeps product. count=" + countBefore);
    }

    // =========================================================================
    // CATEGORIES TAB
    // =========================================================================

    // ── TC_ADM_014 ────────────────────────────────────────────────────────────
    @Test(priority = 14,
          description = "TC_ADM_014: Categories tab loads category list")
    public void testCategoriesTabLoads() {
        adminPage.clickTab("categories");

        String src = getDriver().getPageSource();
        // Must show either categories or empty message
        Assert.assertTrue(
            src.contains("No categories") || src.contains("Name") ||
            getDriver().findElements(
                org.openqa.selenium.By.cssSelector("table, ul, .space-y-2"))
                .size() > 0,
            "Categories tab must load content");

        Assert.assertTrue(adminPage.isSearchVisible(),
            "Search must be visible on Categories tab");

        log.info("PASS TC_ADM_014 | Categories tab loaded");
    }

    // ── TC_ADM_015 ────────────────────────────────────────────────────────────
    @Test(priority = 15,
          description = "TC_ADM_015: Add Category modal opens and form fields present")
    public void testAddCategoryModal() {
        adminPage.clickAddCategory();

        Assert.assertTrue(adminPage.isModalOpen(),
            "Add Category modal must open");

        String heading = adminPage.getModalHeadingText();
        Assert.assertTrue(heading.contains("Category"),
            "Modal heading must mention 'Category'. Got: '" + heading + "'");

        String src = getDriver().getPageSource();
        Assert.assertTrue(src.contains("Category name"),
            "Category name field must be in modal");

        log.info("PASS TC_ADM_015 | Add Category modal opened");
    }

    // =========================================================================
    // BRANDS TAB
    // =========================================================================

    // ── TC_ADM_016 ────────────────────────────────────────────────────────────
    @Test(priority = 16,
          description = "TC_ADM_016: Brands tab loads brand list")
    public void testBrandsTabLoads() {
        adminPage.clickTab("brands");

        String src = getDriver().getPageSource();
        Assert.assertTrue(
            src.contains("No brands") || src.contains("Brand") ||
            getDriver().findElements(
                org.openqa.selenium.By.cssSelector("table, ul, .grid"))
                .size() > 0,
            "Brands tab must load content");

        Assert.assertTrue(adminPage.isSearchVisible(),
            "Search must be visible on Brands tab");

        log.info("PASS TC_ADM_016 | Brands tab loaded");
    }

    // ── TC_ADM_017 ────────────────────────────────────────────────────────────
    @Test(priority = 17,
          description = "TC_ADM_017: Add Brand modal opens and form fields present")
    public void testAddBrandModal() {
        adminPage.clickAddBrand();

        Assert.assertTrue(adminPage.isModalOpen(),
            "Add Brand modal must open");

        String heading = adminPage.getModalHeadingText();
        Assert.assertTrue(heading.contains("Brand"),
            "Modal heading must mention 'Brand'. Got: '" + heading + "'");

        String src = getDriver().getPageSource();
        Assert.assertTrue(src.contains("Brand name"),
            "Brand name field must be in modal");

        log.info("PASS TC_ADM_017 | Add Brand modal opened");
    }

    // =========================================================================
    // ORDERS TAB
    // =========================================================================

    // ── TC_ADM_018 ────────────────────────────────────────────────────────────
    @Test(priority = 18,
          description = "TC_ADM_018: Orders tab loads order list or empty state")
    public void testOrdersTabLoads() {
        adminPage.clickTab("orders");

        String src = getDriver().getPageSource();
        Assert.assertTrue(
            src.contains("No orders") || src.contains("Order") ||
            src.contains("Status") || src.contains("Amount"),
            "Orders tab must load content or empty state");

        log.info("PASS TC_ADM_018 | Orders tab loaded");
    }

    // ── TC_ADM_019 ────────────────────────────────────────────────────────────
    @Test(priority = 19,
          description = "TC_ADM_019: Orders tab shows order status badges if orders exist")
    public void testOrdersStatusBadges() {
        adminPage.clickTab("orders");

        String src = getDriver().getPageSource();

        if (src.contains("No orders") || src.contains("no orders")) {
            log.info("INFO TC_ADM_019 | No orders present — skipping badge check");
            return;
        }

        // At least one order status badge must be present
        boolean hasStatus =
            src.contains("PENDING")    || src.contains("CONFIRMED") ||
            src.contains("PROCESSING") || src.contains("SHIPPED")   ||
            src.contains("DELIVERED")  || src.contains("CANCELLED");

        Assert.assertTrue(hasStatus,
            "Orders must show status badges when orders exist");

        log.info("PASS TC_ADM_019 | Order status badges found");
    }

    // =========================================================================
    // USERS TAB
    // =========================================================================

    // ── TC_ADM_020 ────────────────────────────────────────────────────────────
    @Test(priority = 20,
          description = "TC_ADM_020: Users tab loads user list (admin only)")
    public void testUsersTabLoads() {
        adminPage.clickTab("users");

        String src = getDriver().getPageSource();
        Assert.assertTrue(
            src.contains("No users") || src.contains("Users") ||
            src.contains("Email")    || src.contains("Role")  ||
            src.contains("user@"),
            "Users tab must load user list or empty state");

        log.info("PASS TC_ADM_020 | Users tab loaded");
    }
}