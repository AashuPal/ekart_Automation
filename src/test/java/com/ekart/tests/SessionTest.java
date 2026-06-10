package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.LoginPage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Session Handling Tests
 * Validates session persistence, expiry, and redirect behavior
 */
public class SessionTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeMethod
    public void initLoginPage() {
        loginPage = new LoginPage(getDriver());
    }

    // ===== TC_SESSION_001: Session persists after page refresh =====
    @Test(priority = 1, description = "TC_SESSION_001: Session persists after page refresh")
    public void testSessionPersistsAfterRefresh() throws InterruptedException {
        loginAsValidUser();

        String tokenBefore = loginPage.getSessionToken();

        getDriver().navigate().refresh();
        Thread.sleep(2000);

        String tokenAfter = loginPage.getSessionToken();
        String urlAfter   = getDriver().getCurrentUrl();

        Assert.assertNotNull(tokenAfter,
            "Session token should persist after refresh");
        Assert.assertEquals(tokenBefore, tokenAfter,
            "Session token should be same after refresh");
        Assert.assertFalse(urlAfter.contains("/login"),
            "Should not redirect to login after refresh. URL: " + urlAfter);

        log.info("PASS: Session persists after refresh | URL: " + urlAfter);
    }

    // ===== TC_SESSION_002: Session persists across navigation =====
    @Test(priority = 2, description = "TC_SESSION_002: Session persists across page navigation")
    public void testSessionPersistsAcrossNavigation() throws InterruptedException {
        loginAsValidUser();

        String tokenBefore = loginPage.getSessionToken();

        getDriver().get(baseUrl + "/products");
        Thread.sleep(1000);
        String tokenAfterProducts = loginPage.getSessionToken();

        getDriver().get(baseUrl + "/cart");
        Thread.sleep(1000);
        String tokenAfterCart = loginPage.getSessionToken();

        Assert.assertEquals(tokenBefore, tokenAfterProducts,
            "Token should persist after navigating to products");
        Assert.assertEquals(tokenBefore, tokenAfterCart,
            "Token should persist after navigating to cart");

        log.info("PASS: Session persists across navigation");
    }

    // ===== TC_SESSION_003: Unauthenticated user redirected to login =====
    @Test(priority = 3, description = "TC_SESSION_003: Unauthenticated access redirects to login")
    public void testUnauthenticatedRedirectToLogin() throws InterruptedException {
        getDriver().get(baseUrl);
        clearBrowserSession();
        getDriver().navigate().refresh();
        Thread.sleep(1000);

        loginPage.navigateToProtectedPage(baseUrl);

        try {
            WaitUtils.waitForUrl(getDriver(), "login");
        } catch (Exception e) {
            log.warn("Redirect wait timed out");
        }

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains("login") ||
            currentUrl.contains("register") ||
            !currentUrl.contains("profile"),
            "Unauthenticated user should be redirected. URL: " + currentUrl
        );
        log.info("PASS: Unauthenticated access redirected | URL: " + currentUrl);
    }

    // ===== TC_SESSION_004: Session cleared on logout =====
    @Test(priority = 4, description = "TC_SESSION_004: localStorage cleared after logout")
    public void testLocalStorageClearedOnLogout() {
        loginAsValidUser();

        Assert.assertTrue(
            loginPage.isSessionTokenPresent(),
            "Token should be in localStorage after login"
        );

        logoutCurrentUser();

        String token = loginPage.getSessionToken();
        Assert.assertTrue(
            token == null || token.isEmpty() || token.equals("null"),
            "localStorage token should be cleared after logout"
        );
        log.info("PASS: localStorage cleared after logout");
    }

    // ===== TC_SESSION_005: Manual session clear redirects to login =====
    @Test(priority = 5, description = "TC_SESSION_005: Clearing session manually redirects to login")
    public void testManualSessionClearRedirectsToLogin() {
        loginAsValidUser();

        clearBrowserSession();
        log.info("Session manually cleared");

        loginPage.navigateToProtectedPage(baseUrl);

        try {
            WaitUtils.waitForUrl(getDriver(), "login");
        } catch (Exception e) {
            log.warn("Redirect timed out");
        }

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains("login") || !currentUrl.contains("profile"),
            "Should redirect after session cleared. URL: " + currentUrl
        );
        log.info("PASS: Manual session clear handled correctly | URL: " + currentUrl);
    }

    // ===== TC_SESSION_006: Login page accessible when not logged in =====
    @Test(priority = 6, description = "TC_SESSION_006: Login page accessible when not logged in")
    public void testLoginPageAccessibleWhenNotLoggedIn() {
        clearBrowserSession();

        getDriver().get(baseUrl + "/login");

        Assert.assertTrue(
            getDriver().getCurrentUrl().contains("login"),
            "Login page should be accessible when not logged in"
        );
        Assert.assertFalse(
            loginPage.isErrorDisplayed(),
            "No error should show on fresh login page load"
        );
        log.info("PASS: Login page accessible when not logged in");
    }
}