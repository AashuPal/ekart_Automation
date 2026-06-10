package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.LoginPage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Logout Tests — uses reusable loginAs utility from BaseTest
 * Automate logout functionality + validate session handling
 */
public class LogoutTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeMethod
    public void initLoginPage() {
        loginPage = new LoginPage(getDriver());
    }

    // ===== TC_LOGOUT_001: Basic Logout Flow =====
    @Test(priority = 1, description = "TC_LOGOUT_001: Verify user can logout successfully")
    public void testLogout() {
        loginAsValidUser();

        Assert.assertTrue(
            !getDriver().getCurrentUrl().contains("/login"),
            "User should be logged in before logout test"
        );
        log.info("User logged in at: " + getDriver().getCurrentUrl());

        logoutCurrentUser();

        String urlAfterLogout = getDriver().getCurrentUrl();
        Assert.assertTrue(
            urlAfterLogout.contains("login") ||
            urlAfterLogout.equals(baseUrl + "/") ||
            urlAfterLogout.equals(baseUrl),
            "After logout should redirect to login or home. URL: " + urlAfterLogout
        );
        log.info("PASS: Logout successful | URL: " + urlAfterLogout);
    }

    // ===== TC_LOGOUT_002: Session token cleared after logout =====
    @Test(priority = 2, description = "TC_LOGOUT_002: Verify session token removed after logout")
    public void testSessionTokenClearedAfterLogout() {
        loginAsValidUser();

        String tokenBeforeLogout = loginPage.getSessionToken();

        Assert.assertNotNull(tokenBeforeLogout,
            "Token should exist after login");
        Assert.assertFalse(tokenBeforeLogout.isEmpty(),
            "Token should not be empty after login");

        String tokenPreview = tokenBeforeLogout.length() >= 10
            ? tokenBeforeLogout.substring(0, 10) + "..."
            : tokenBeforeLogout;
        log.info("Token present before logout: " + tokenPreview);

        logoutCurrentUser();

        String tokenAfterLogout = loginPage.getSessionToken();
        Assert.assertTrue(
            tokenAfterLogout == null ||
            tokenAfterLogout.isEmpty() ||
            tokenAfterLogout.equals("null"),
            "Session token should be cleared after logout"
        );
        log.info("PASS: Session token cleared after logout");
    }

    // ===== TC_LOGOUT_003: Cannot access protected page after logout =====
    @Test(priority = 3, description = "TC_LOGOUT_003: Protected page redirects to login after logout")
    public void testProtectedPageAfterLogout() {
        loginAsValidUser();

        logoutCurrentUser();

        loginPage.navigateToProtectedPage(baseUrl);

        try {
            WaitUtils.waitForUrl(getDriver(), "login");
        } catch (Exception e) {
            log.warn("Wait for login redirect timed out");
        }

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains("login") || currentUrl.contains("register"),
            "Should redirect to login when accessing protected page after logout. URL: " + currentUrl
        );
        log.info("PASS: Protected page redirects after logout | URL: " + currentUrl);
    }

    // ===== TC_LOGOUT_004: Login again after logout =====
    @Test(priority = 4, description = "TC_LOGOUT_004: User can login again after logout")
    public void testLoginAgainAfterLogout() {
        loginAsValidUser();

        logoutCurrentUser();

        loginAsValidUser();

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertFalse(
            currentUrl.endsWith("/login"),
            "User should be able to login again after logout. URL: " + currentUrl
        );
        log.info("PASS: Login again after logout works | URL: " + currentUrl);
    }
}