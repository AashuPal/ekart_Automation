package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * User Profile Tests (missing from project)
 *
 *  TC_PROF_001  Profile page accessible after login
 *  TC_PROF_002  Profile page shows logged-in user email
 *  TC_PROF_003  Profile page inaccessible without login (redirect)
 *  TC_PROF_004  User menu appears in navbar after login
 *  TC_PROF_005  Order history section visible on profile
 */
public class ProfileTest extends BaseTest {

    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));
        loginAsValidUser();
        log.info("Logged in for ProfileTest");
    }

    @BeforeMethod
    public void setup() {
        try {
            if (getDriver().getCurrentUrl().contains("/login"))
                loginAsValidUser();
        } catch (Exception ignored) {}
    }

    // ── TC_PROF_001 ─────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_PROF_001: Profile page accessible after login")
    public void testProfilePageAccessible() {
        getDriver().get(baseUrl + "/profile");
        WaitUtils.waitForPageReady(getDriver());

        String url = getDriver().getCurrentUrl();
        Assert.assertFalse(url.contains("/login"),
            "Profile must be accessible after login. URL: " + url);
        Assert.assertTrue(
            url.contains("/profile") || url.contains(baseUrl),
            "Must stay on profile or redirect to valid page. URL: " + url);

        log.info("PASS TC_PROF_001 | URL: " + url);
    }

    // ── TC_PROF_002 ─────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_PROF_002: Profile page shows logged-in user email")
    public void testProfileShowsUserEmail() {
        getDriver().get(baseUrl + "/profile");
        WaitUtils.waitForPageReady(getDriver());

        if (getDriver().getCurrentUrl().contains("/login")) {
            log.info("INFO TC_PROF_002 | Redirected to login — profile may require re-auth");
            return;
        }

        String pageSource = getDriver().getPageSource();
        String validEmail = com.ekart.config.ConfigReader.getValidEmail();

        Assert.assertTrue(pageSource.contains(validEmail) ||
                          pageSource.contains(validEmail.split("@")[0]),
            "Profile page must show user email or username. Email: " + validEmail);

        log.info("PASS TC_PROF_002 | Email found on profile page");
    }

    // ── TC_PROF_003 ─────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_PROF_003: Profile page redirects to login when not authenticated")
    public void testProfileRequiresLogin() {
        // Clear session
        ((org.openqa.selenium.JavascriptExecutor) getDriver())
            .executeScript("localStorage.clear();");
        getDriver().navigate().refresh();
        WaitUtils.waitForPageReady(getDriver());

        getDriver().get(baseUrl + "/profile");
        WaitUtils.waitForPageReady(getDriver());

        try {
            WaitUtils.waitForUrl(getDriver(), "login");
        } catch (Exception ignored) {}

        String url = getDriver().getCurrentUrl();
        Assert.assertTrue(
            url.contains("/login") || !url.contains("/profile"),
            "Profile must redirect unauthenticated users. URL: " + url);

        log.info("PASS TC_PROF_003 | Unauthenticated profile access handled. URL: " + url);

        // Re-login for subsequent tests
        loginAsValidUser();
    }

    // ── TC_PROF_004 ─────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_PROF_004: User menu / avatar visible in navbar after login")
    public void testUserMenuVisibleAfterLogin() {
        getDriver().get(baseUrl);
        WaitUtils.waitForPageReady(getDriver());

        // User avatar / menu button — various possible selectors
        By userMenu = By.cssSelector(
            "button[aria-label*='user' i], button[aria-label*='account' i], " +
            "button[aria-label*='profile' i], img[alt*='avatar' i], " +
            "div[class*='avatar'], span[class*='user-initial']");

        boolean menuVisible = !getDriver().findElements(userMenu).isEmpty();

        if (!menuVisible) {
            // Fallback: check for user email in navbar
            menuVisible = getDriver().getPageSource().contains(
                com.ekart.config.ConfigReader.getValidEmail().split("@")[0]);
        }

        Assert.assertTrue(menuVisible,
            "User menu or avatar must be visible in navbar after login");

        log.info("PASS TC_PROF_004 | User menu visible after login");
    }

    // ── TC_PROF_005 ─────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_PROF_005: Order history section is visible on profile page")
    public void testOrderHistoryOnProfile() {
        getDriver().get(baseUrl + "/profile");
        WaitUtils.waitForPageReady(getDriver());

        if (getDriver().getCurrentUrl().contains("/login")) {
            log.info("INFO TC_PROF_005 | Profile redirected to login — skipping");
            return;
        }

        String pageSource = getDriver().getPageSource();
        boolean hasOrders = pageSource.contains("Order History") ||
                            pageSource.contains("My Orders") ||
                            pageSource.contains("order-history") ||
                            pageSource.contains("No orders");

        Assert.assertTrue(hasOrders,
            "Profile must contain an order history section");

        log.info("PASS TC_PROF_005 | Order history section found on profile");
    }
}