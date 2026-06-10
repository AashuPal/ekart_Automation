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
    }

    // alwaysRun=true ensures browser is closed even when tests fail
    @AfterClass(alwaysRun = true)
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
     * Login with valid credentials from config.properties.
     * Waits for redirect away from the login page.
     */
    protected void loginAsValidUser() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.navigateTo(baseUrl);
        loginPage.login(
            ConfigReader.getValidEmail(),
            ConfigReader.getValidPassword()
        );
        // Wait for redirect away from login (not just for "/" which is in every URL)
        try {
            WaitUtils.waitForUrlNotContains(getDriver(), "/login");
        } catch (Exception e) {
            log.warn("Redirect wait timed out after login");
        }
        log.info("Logged in as: " + ConfigReader.getValidEmail());
    }

    /**
     * Login with custom credentials.
     * Use this for invalid login tests.
     */
    protected void loginAs(String email, String password) {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.navigateTo(baseUrl);
        loginPage.login(email, password);
        log.info("Login attempted as: " + email);
    }

    /**
     * Logout utility — use in any test that needs to log out.
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
     * Clear browser session (localStorage + sessionStorage).
     * Simulates session expiry.
     */
    protected void clearBrowserSession() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.clearSession();
        log.info("Browser session cleared");
    }

    /**
     * Check if currently logged in.
     */
    protected boolean isUserLoggedIn() {
        LoginPage loginPage = new LoginPage(getDriver());
        return loginPage.isLoggedIn();
    }
}