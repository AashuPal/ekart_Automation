package com.ekart.base;

import com.ekart.config.ConfigReader;
import com.ekart.pages.LoginPage;
import com.ekart.utils.DriverManager;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * BaseTest — Parent class for all test classes
 * Contains reusable login/logout utilities (POM refactor)
 */
public class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);
    protected String baseUrl;

    @BeforeClass
    public void setUp() {
        String browser = ConfigReader.getBrowser();
        baseUrl = ConfigReader.getBaseUrl();
        log.info("Starting | Browser: " + browser + " | URL: " + baseUrl);
        DriverManager.initDriver(browser);

        // Set timeouts once after driver init
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60))
            .implicitlyWait(java.time.Duration.ofSeconds(0)); // keep 0 — use explicit waits
    }

    @AfterClass
    public void tearDown() {
        log.info("Tests complete. Closing browser.");
        DriverManager.quitDriver();
    }

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    // =========================================================
    // ===== REUSABLE LOGIN UTILITY — used across all tests =====
    // =========================================================

    /**
     * Login with valid credentials.
     * Idempotent — skips login if session already active.
     * Call from @BeforeClass so login happens ONCE per suite.
     */
    protected void loginAsValidUser() {
        // Skip if already authenticated
        try {
            String url = getDriver().getCurrentUrl();
            if (!url.contains("/login") && !url.contains("/register")
                    && !url.equals("data:,") && !url.isEmpty()) {
                log.info("Session active — skipping login | URL: " + url);
                return;
            }
        } catch (Exception ignored) {}

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.navigateTo(baseUrl);   // LoginPage.navigateTo() appends /login internally
        loginPage.login(
            ConfigReader.getValidEmail(),
            ConfigReader.getValidPassword()
        );
        try {
            WaitUtils.waitForUrlNotContains(getDriver(), "/login");
        } catch (Exception e) {
            log.warn("Login redirect timed out");
        }
        String afterUrl = getDriver().getCurrentUrl();
        if (afterUrl.contains("/login")) {
            log.error("Login may have failed — still on: " + afterUrl);
        } else {
            log.info("Login successful | URL: " + afterUrl);

            // Refresh once after login so React re-hydrates
            // and localStorage session token is stable before tests begin
            getDriver().navigate().refresh();
            WaitUtils.waitForPageReady(getDriver());
            log.info("Post-login refresh done | URL: " + getDriver().getCurrentUrl());
        }
    }

    /**
     * Login with custom credentials
     * Use this for invalid login tests
     */
    protected void loginAs(String email, String password) {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.navigateTo(baseUrl);
        loginPage.login(email, password);
        log.info("Login attempted as: " + email);
    }

    /**
     * Logout utility — use in any test that needs to log out
     */
    protected void logoutCurrentUser() {
        LoginPage loginPage = new LoginPage(getDriver());
        boolean loggedOut = loginPage.logout();
        if (loggedOut) {
            try {
                WaitUtils.waitForUrl(getDriver(), "login");
            } catch (Exception e) {
                log.warn("Logout redirect wait timed out");
            }
            log.info("Logged out successfully");
        } else {
            log.warn("Could not find logout button — user may not be logged in");
        }
    }

    /**
     * Clear browser session (localStorage + sessionStorage)
     * Simulates session expiry
     */
    protected void clearBrowserSession() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.clearSession();
        log.info("Browser session cleared");
    }

    /**
     * Check if currently logged in
     */
    protected boolean isUserLoggedIn() {
        LoginPage loginPage = new LoginPage(getDriver());
        return loginPage.isLoggedIn();
    }
}