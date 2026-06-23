package com.ekart.base;

import com.ekart.config.ConfigReader;
import com.ekart.pages.LoginPage;
import com.ekart.utils.DriverManager;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

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
            .pageLoadTimeout(Duration.ofSeconds(60))
            .implicitlyWait(Duration.ofSeconds(0)); // keep 0 — use explicit waits
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
     * Retries up to MAX_LOGIN_ATTEMPTS times before throwing.
     * Call from @BeforeClass so login happens ONCE per suite.
     *
     * @throws IllegalStateException if login cannot be completed after all retries
     */
    protected void loginAsValidUser() {
        // Skip if already authenticated (check URL and localStorage token)
        if (isSessionActive()) {
            log.info("Session active — skipping login | URL: " + getDriver().getCurrentUrl());
            return;
        }

        final int MAX_LOGIN_ATTEMPTS = 3;

        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            log.info("Login attempt " + attempt + "/" + MAX_LOGIN_ATTEMPTS);

            try {
                LoginPage loginPage = new LoginPage(getDriver());
                loginPage.navigateTo(baseUrl);
                loginPage.login(
                    ConfigReader.getValidEmail(),
                    ConfigReader.getValidPassword()
                );

                // Wait for redirect away from /login — up to 10s
                // The app may use React Router which updates URL before
                // rendering, so we wait for the URL change first.
                boolean redirected = waitForRedirectFromLogin(10);

                if (redirected) {
                    // Extra wait: let React finish rendering + localStorage hydrate
                    WaitUtils.waitForPageReady(getDriver());

                    // Confirm session token is in localStorage
                    if (isSessionActive()) {
                        log.info("Login successful on attempt " + attempt
                            + " | URL: " + getDriver().getCurrentUrl());
                        return;
                    } else {
                        log.warn("Redirected but no session token found — retrying");
                    }
                } else {
                    log.warn("Login attempt " + attempt + " — still on /login after 10s");

                    // Check for error messages on the login page to aid debugging
                    try {
                        String pageText = getDriver().findElement(
                            org.openqa.selenium.By.tagName("body")).getText();
                        if (pageText.contains("Invalid") || pageText.contains("incorrect")
                                || pageText.contains("wrong") || pageText.contains("error")) {
                            log.error("Login page shows error message — check credentials in config. "
                                + "Email: " + ConfigReader.getValidEmail());
                            // No point retrying with same bad credentials
                            break;
                        }
                    } catch (Exception ignored) {}
                }

            } catch (Exception e) {
                log.warn("Login attempt " + attempt + " threw: " + e.getMessage());
            }

            // Brief pause before retry
            if (attempt < MAX_LOGIN_ATTEMPTS) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        }

        // All attempts exhausted — hard fail so tests don't run in a broken state
        String finalUrl = getDriver().getCurrentUrl();
        throw new IllegalStateException(
            "loginAsValidUser() failed after " + MAX_LOGIN_ATTEMPTS + " attempts. "
            + "Current URL: " + finalUrl + ". "
            + "Check: (1) credentials in config, (2) LoginPage.login() form submit, "
            + "(3) network connectivity to " + baseUrl);
    }

    /**
     * Returns true if the user appears to be authenticated.
     * Checks both the current URL (not on /login) and presence of
     * an auth token in localStorage (common pattern for React SPAs).
     */
    protected boolean isSessionActive() {
        try {
            String url = getDriver().getCurrentUrl();

            // Not yet on any page
            if (url == null || url.equals("data:,") || url.isEmpty()) {
                return false;
            }

            // On login/register page = not authenticated
            if (url.contains("/login") || url.contains("/register")) {
                return false;
            }

            // Check localStorage for an auth token — React SPAs typically store
            // token/user under keys like "token", "authToken", "user", "ekart-user"
            JavascriptExecutor js = (JavascriptExecutor) getDriver();
            String[] tokenKeys = {"token", "authToken", "auth_token", "user",
                                  "ekart-user", "ekartUser", "accessToken"};
            for (String key : tokenKeys) {
                Object value = js.executeScript(
                    "return window.localStorage.getItem(arguments[0]);", key);
                if (value != null && !value.toString().isEmpty()) {
                    log.debug("Session token found in localStorage['" + key + "']");
                    return true;
                }
            }

            // URL is not /login and no token found — could still be valid
            // (some apps use httpOnly cookies instead). Trust the URL.
            log.debug("No localStorage token found; trusting URL: " + url);
            return true;

        } catch (Exception e) {
            log.debug("isSessionActive() check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Waits up to {@code timeoutSeconds} for the browser URL to move away
     * from any URL containing "/login".
     * Falls back to a manual polling loop if WaitUtils doesn't expose a
     * Duration-based overload.
     *
     * @return true if redirect happened within the timeout; false otherwise
     */
    private boolean waitForRedirectFromLogin(int timeoutSeconds) {
        // Try WaitUtils first
        try {
            WaitUtils.waitForUrlNotContains(getDriver(), "/login");
            return !getDriver().getCurrentUrl().contains("/login");
        } catch (Exception ignored) {}

        // Manual fallback poll — checks every 500 ms up to timeoutSeconds
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!getDriver().getCurrentUrl().contains("/login")) return true;
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Login with custom credentials.
     * Use this for invalid-login tests.
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