package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

/**
 * LogoutPage — Page Object for Logout and Session Handling
 *
 * Covers:
 *  - Performing logout via profile/avatar menu
 *  - Verifying post-logout state (URL, UI elements)
 *  - Session validation (cookies, localStorage, protected routes)
 *  - Session persistence across page refresh
 *  - Expired session / direct URL access after logout
 */
public class LogoutPage extends BasePage {

    // ── Profile / User menu ────────────────────────────────────────────────
    // Multiple fallback locators to handle React apps that vary class names
    private final By userAvatarOrName = By.xpath(
        "//*[contains(@class,'avatar') or contains(@class,'user-icon') " +
        "or contains(@class,'user-menu') or contains(@class,'profile') " +
        "or contains(@class,'account') or @data-testid='user-menu']"
    );

    // ── Logout trigger ─────────────────────────────────────────────────────
    private final By logoutMenuItem = By.xpath(
        "//button[normalize-space()='Logout' or normalize-space()='Log Out' " +
        "or normalize-space()='Sign Out' or normalize-space()='Signout'] " +
        "| //a[normalize-space()='Logout' or normalize-space()='Log Out' " +
        "or normalize-space()='Sign Out']"
    );

    // ── Post-logout "logged-out" indicators ───────────────────────────────
    private final By loginLink = By.xpath(
        "//a[contains(text(),'Login') or contains(text(),'Sign In')] " +
        "| //button[contains(text(),'Login') or contains(text(),'Sign In')]"
    );

    private final By registerLink = By.xpath(
        "//a[contains(text(),'Register') or contains(text(),'Sign Up')] " +
        "| //button[contains(text(),'Register') or contains(text(),'Sign Up')]"
    );

    // Login page heading — confirms redirect landed on /login
    private final By loginHeading = By.xpath(
        "//h1[contains(text(),'Login') or contains(text(),'Sign In')] " +
        "| //h2[contains(text(),'Login') or contains(text(),'Sign In') or contains(text(),'Welcome Back')]"
    );

    // ── Toast / confirmation ───────────────────────────────────────────────
    private final By logoutToast = By.xpath(
        "//*[contains(@class,'toast') or contains(@class,'notification') or " +
        "contains(@class,'alert')][contains(text(),'logout') or " +
        "contains(text(),'signed out') or contains(text(),'Logout')]"
    );

    // ── Cart badge — should clear after logout ─────────────────────────────
    private final By cartBadge = By.cssSelector(
        "[class*='cart-count'], [class*='cart-badge'], [data-testid='cart-count']"
    );

    // ── Protected routes check ─────────────────────────────────────────────
    private static final String PROFILE_PATH   = "/profile";
    private static final String ORDERS_PATH    = "/orders";
    private static final String CHECKOUT_PATH  = "/checkout";
    private static final String DASHBOARD_PATH = "/dashboard";

    public LogoutPage(WebDriver driver) {
        super(driver);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ACTIONS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Perform logout — opens user menu then clicks the logout item.
     * Returns true if logout was triggered, false if the menu/button wasn't found.
     */
    public boolean performLogout() {
        log.info("Attempting logout…");

        // Step 1: try clicking the user-avatar / profile icon to open dropdown
        if (isDisplayed(userAvatarOrName)) {
            log.info("  → User avatar found; clicking to open menu");
            click(userAvatarOrName);
            pause(400);
        } else {
            log.warn("  → User avatar not found — may already be on logout trigger or using inline nav");
        }

        // Step 2: click the logout item
        if (isDisplayed(logoutMenuItem)) {
            log.info("  → Logout menu item found; clicking");
            click(logoutMenuItem);
            pause(1000); // allow redirect / state clear
            log.info("  → Logout clicked. Current URL: " + getCurrentUrl());
            return true;
        }

        log.error("  → Logout menu item NOT found");
        return false;
    }

    /**
     * Navigate to the login page directly and perform login,
     * then logout — a full round-trip helper used by tests.
     */
    public void loginAndLogout(String baseUrl, String email, String password) {
        driver.get(baseUrl + "/login");
        pause(800);

        // Enter credentials (reuse LoginPage logic inline)
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(email, password);
        pause(2000);

        log.info("Login complete. URL after login: " + getCurrentUrl());

        // Now logout
        performLogout();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  VERIFICATION HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns true if the browser URL contains "/login" after logout,
     * indicating the app redirected the user to the login page.
     */
    public boolean isRedirectedToLogin() {
        String url = getCurrentUrl();
        boolean redirected = url.contains("/login") || url.contains("login");
        log.info("isRedirectedToLogin → " + redirected + " | URL: " + url);
        return redirected;
    }

    /**
     * Returns true if the Login link/button is visible in the header/nav —
     * the typical indicator of a logged-out state.
     */
    public boolean isLoginLinkVisible() {
        boolean visible = isDisplayed(loginLink);
        log.info("isLoginLinkVisible → " + visible);
        return visible;
    }

    /** Returns true if the Register link is visible (another logged-out indicator). */
    public boolean isRegisterLinkVisible() {
        return isDisplayed(registerLink);
    }

    /** Returns true if a logout toast/notification appeared. */
    public boolean isLogoutToastVisible() {
        return isDisplayed(logoutToast);
    }

    /**
     * Returns true if the user-profile avatar is NO LONGER visible,
     * confirming the session UI was torn down.
     */
    public boolean isUserAvatarGone() {
        boolean gone = !isDisplayed(userAvatarOrName);
        log.info("isUserAvatarGone → " + gone);
        return gone;
    }

    // ── Cookie / Session helpers ───────────────────────────────────────────

    /**
     * Returns the value of a named cookie, or null if it doesn't exist.
     */
    public String getCookieValue(String cookieName) {
        Cookie c = driver.manage().getCookieNamed(cookieName);
        return (c != null) ? c.getValue() : null;
    }

    /**
     * Returns true if NO authentication-related cookies remain after logout.
     * Checks common auth cookie names.
     */
    public boolean areAuthCookiesCleared() {
        String[] authCookieNames = {
            "token", "auth_token", "access_token", "session", "sessionid",
            "jwt", "JSESSIONID", "user_session", "remember_token"
        };
        for (String name : authCookieNames) {
            Cookie c = driver.manage().getCookieNamed(name);
            if (c != null) {
                log.warn("Auth cookie still present after logout: " + name + " = " + c.getValue());
                return false;
            }
        }
        log.info("No auth cookies found — session cookies cleared");
        return true;
    }

    /**
     * Reads a localStorage key via JavaScript.
     * Returns null if key doesn't exist.
     */
    public String getLocalStorageItem(String key) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object val = js.executeScript("return window.localStorage.getItem(arguments[0]);", key);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            log.warn("Could not read localStorage[" + key + "]: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns true if common auth tokens are absent from localStorage.
     */
    public boolean areLocalStorageTokensCleared() {
        String[] tokenKeys = {
            "token", "authToken", "access_token", "user", "userData",
            "jwt", "session", "auth"
        };
        for (String key : tokenKeys) {
            String val = getLocalStorageItem(key);
            if (val != null && !val.equalsIgnoreCase("null") && !val.isEmpty()) {
                log.warn("localStorage token still present: " + key + " = " + val);
                return false;
            }
        }
        log.info("No auth tokens in localStorage — storage cleared");
        return true;
    }

    /**
     * Returns all cookies currently in the browser as a readable summary.
     */
    public String getAllCookiesSummary() {
        Set<Cookie> cookies = driver.manage().getCookies();
        if (cookies.isEmpty()) return "(no cookies)";
        StringBuilder sb = new StringBuilder();
        for (Cookie c : cookies) {
            sb.append(c.getName()).append("=").append(c.getValue()).append("; ");
        }
        return sb.toString().trim();
    }

    // ── Protected-route helpers ────────────────────────────────────────────

    /**
     * Navigates directly to a protected route and returns the URL
     * the app lands on — used to verify the app guards the route properly.
     */
    public String navigateToProtectedRoute(String baseUrl, String path) {
        String target = baseUrl + path;
        log.info("Navigating to protected route: " + target);
        driver.get(target);
        pause(1500);
        String landed = getCurrentUrl();
        log.info("  → Landed on: " + landed);
        return landed;
    }

    /**
     * Returns true if accessing a protected route redirected back to login.
     */
    public boolean isProtectedRouteGuarded(String baseUrl, String path) {
        String landed = navigateToProtectedRoute(baseUrl, path);
        return landed.contains("/login") || isDisplayed(loginHeading);
    }

    // ── Session persistence helper ─────────────────────────────────────────

    /**
     * Refreshes the current page and returns the resulting URL.
     * Used to test whether a logged-in session persists across refresh.
     */
    public String refreshAndGetUrl() {
        driver.navigate().refresh();
        pause(1500);
        return getCurrentUrl();
    }

    // ── Convenience ───────────────────────────────────────────────────────

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    /** Expose protected-route paths as constants for test use. */
    public static String profilePath()   { return PROFILE_PATH; }
    public static String ordersPath()    { return ORDERS_PATH; }
    public static String checkoutPath()  { return CHECKOUT_PATH; }
    public static String dashboardPath() { return DASHBOARD_PATH; }
}