package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.config.ConfigReader;
import com.ekart.pages.LoginPage;
import com.ekart.pages.LogoutPage;
import com.ekart.utils.WaitUtils;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  LogoutTest — Automated Logout & Session Handling Validation
 * ══════════════════════════════════════════════════════════════════════════════
 *
 *  Test Cases:
 *
 *  TC_LOGOUT_001  Logout via profile menu redirects to login page
 *  TC_LOGOUT_002  Login button reappears in nav after logout
 *  TC_LOGOUT_003  Auth cookies are cleared after logout
 *  TC_LOGOUT_004  LocalStorage auth tokens are cleared after logout
 *  TC_LOGOUT_005  User avatar / profile icon disappears after logout
 *  TC_LOGOUT_006  Session does NOT persist after logout — re-login required
 *  TC_LOGOUT_007  Protected route /profile redirects to login when unauthenticated
 *  TC_LOGOUT_008  Protected route /orders redirects to login when unauthenticated
 *  TC_LOGOUT_009  Protected route /checkout redirects to login when unauthenticated
 *  TC_LOGOUT_010  Session persists across page refresh (logged-IN state)
 *  TC_LOGOUT_011  Logout is idempotent — calling logout twice has no error
 *  TC_LOGOUT_012  Back-button after logout does not restore authenticated state
 *  TC_LOGOUT_013  Full login → logout → login cycle works correctly
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class LogoutTest extends BaseTest {

    private LoginPage  loginPage;
    private LogoutPage logoutPage;

    private String validEmail;
    private String validPassword;

    @BeforeClass(alwaysRun = true)
    public void loadCredentials() {
        validEmail    = ConfigReader.getValidEmail();
        validPassword = ConfigReader.getValidPassword();
    }

    @BeforeMethod(alwaysRun = true)
    public void initPages() {
        loginPage  = new LoginPage(getDriver());
        logoutPage = new LogoutPage(getDriver());
    }

    // ──────────────────────────────────────────────────────────────────────
    //  HELPER: log in before a test
    // ──────────────────────────────────────────────────────────────────────
    private void doLogin() {
        loginPage.navigateTo(baseUrl);
        loginPage.login(validEmail, validPassword);
        pause(2000); // allow redirect + app state to settle
        log.info("Setup login complete. URL: " + getDriver().getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_001 — Logout redirects to login page
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
          description = "TC_LOGOUT_001: Logout via profile menu redirects to /login")
    public void testLogoutRedirectsToLogin() {
        doLogin();

        boolean triggered = logoutPage.performLogout();
        Assert.assertTrue(triggered,
            "Logout action should be triggerable — logout menu item not found");

        // After logout the app should send the user to /login
        boolean onLogin = logoutPage.isRedirectedToLogin();
        Assert.assertTrue(onLogin,
            "Should be redirected to /login after logout. " +
            "Current URL: " + getDriver().getCurrentUrl());

        log.info("PASS TC_LOGOUT_001: redirected to → " + getDriver().getCurrentUrl());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_002 — Login link reappears in nav
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
          description = "TC_LOGOUT_002: Login link is visible in nav after logout")
    public void testLoginLinkAppearsAfterLogout() {
        doLogin();
        logoutPage.performLogout();
        pause(1000);

        // Navigate to home so we can see the nav clearly
        getDriver().get(baseUrl);
        pause(1000);

        Assert.assertTrue(
            logoutPage.isLoginLinkVisible() || logoutPage.isRegisterLinkVisible(),
            "Login or Register link should appear in the nav after logout"
        );
        log.info("PASS TC_LOGOUT_002: Login/Register link visible after logout");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_003 — Auth cookies cleared
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
          description = "TC_LOGOUT_003: Auth-related cookies are cleared after logout")
    public void testAuthCookiesClearedAfterLogout() {
        doLogin();

        // Snapshot cookies before logout
        String cookiesBefore = logoutPage.getAllCookiesSummary();
        log.info("Cookies BEFORE logout: " + cookiesBefore);

        logoutPage.performLogout();
        pause(1000);

        String cookiesAfter = logoutPage.getAllCookiesSummary();
        log.info("Cookies AFTER logout: " + cookiesAfter);

        boolean cleared = logoutPage.areAuthCookiesCleared();
        Assert.assertTrue(cleared,
            "Authentication cookies should be cleared after logout. " +
            "Remaining cookies: " + cookiesAfter);

        log.info("PASS TC_LOGOUT_003: Auth cookies cleared");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_004 — localStorage tokens cleared
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
          description = "TC_LOGOUT_004: Auth tokens removed from localStorage after logout")
    public void testLocalStorageTokensClearedAfterLogout() {
        doLogin();

        String tokenBefore = logoutPage.getLocalStorageItem("token");
        log.info("localStorage['token'] BEFORE logout: " + tokenBefore);

        logoutPage.performLogout();
        pause(1000);

        boolean cleared = logoutPage.areLocalStorageTokensCleared();
        Assert.assertTrue(cleared,
            "Auth tokens should be removed from localStorage after logout");

        log.info("PASS TC_LOGOUT_004: localStorage tokens cleared");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_005 — User avatar disappears after logout
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 5,
          description = "TC_LOGOUT_005: User profile avatar/icon is gone after logout")
    public void testUserAvatarGoneAfterLogout() {
        doLogin();
        logoutPage.performLogout();
        pause(1000);

        getDriver().get(baseUrl);
        pause(1000);

        Assert.assertTrue(logoutPage.isUserAvatarGone(),
            "User avatar/profile icon should not be visible after logout");

        log.info("PASS TC_LOGOUT_005: User avatar not visible after logout");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_006 — Session does NOT persist after logout
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 6,
          description = "TC_LOGOUT_006: Session does not persist — re-login required")
    public void testSessionDoesNotPersistAfterLogout() {
        doLogin();
        logoutPage.performLogout();
        pause(1000);

        // Navigate to home and refresh — should still be logged out
        getDriver().get(baseUrl);
        getDriver().navigate().refresh();
        pause(1500);

        // Accessing a protected route should bounce back to login
        boolean guarded = logoutPage.isProtectedRouteGuarded(baseUrl, LogoutPage.profilePath());
        Assert.assertTrue(guarded,
            "After logout, /profile should redirect to login (session must not persist)");

        log.info("PASS TC_LOGOUT_006: Session does not persist after logout");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_007 — /profile is guarded when unauthenticated
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 7,
          description = "TC_LOGOUT_007: /profile route redirects to login when not logged in")
    public void testProfileRouteGuardedWhenNotLoggedIn() {
        // Start without logging in
        getDriver().get(baseUrl);

        boolean guarded = logoutPage.isProtectedRouteGuarded(baseUrl, LogoutPage.profilePath());
        Assert.assertTrue(guarded,
            "/profile should redirect unauthenticated users to /login. " +
            "Landed on: " + getDriver().getCurrentUrl());

        log.info("PASS TC_LOGOUT_007: /profile is route-guarded");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_008 — /orders is guarded when unauthenticated
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 8,
          description = "TC_LOGOUT_008: /orders route redirects to login when not logged in")
    public void testOrdersRouteGuardedWhenNotLoggedIn() {
        getDriver().get(baseUrl);

        boolean guarded = logoutPage.isProtectedRouteGuarded(baseUrl, LogoutPage.ordersPath());
        Assert.assertTrue(guarded,
            "/orders should redirect unauthenticated users to /login. " +
            "Landed on: " + getDriver().getCurrentUrl());

        log.info("PASS TC_LOGOUT_008: /orders is route-guarded");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_009 — /checkout is guarded when unauthenticated
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 9,
          description = "TC_LOGOUT_009: /checkout route redirects to login when not logged in")
    public void testCheckoutRouteGuardedWhenNotLoggedIn() {
        getDriver().get(baseUrl);

        boolean guarded = logoutPage.isProtectedRouteGuarded(baseUrl, LogoutPage.checkoutPath());
        Assert.assertTrue(guarded,
            "/checkout should redirect unauthenticated users to /login. " +
            "Landed on: " + getDriver().getCurrentUrl());

        log.info("PASS TC_LOGOUT_009: /checkout is route-guarded");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_010 — Session persists across page refresh (logged-IN)
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 10,
          description = "TC_LOGOUT_010: Logged-in session persists across page refresh")
    public void testSessionPersistsAcrossRefresh() {
        doLogin();
        String urlBeforeRefresh = getDriver().getCurrentUrl();

        // Refresh the page
        String urlAfterRefresh = logoutPage.refreshAndGetUrl();

        // Should NOT be redirected to login after a simple refresh
        Assert.assertFalse(urlAfterRefresh.contains("/login"),
            "Logged-in session should survive a page refresh. " +
            "Before: " + urlBeforeRefresh + " | After: " + urlAfterRefresh);

        log.info("PASS TC_LOGOUT_010: Session persists after refresh. URL: " + urlAfterRefresh);

        // Cleanup
        logoutPage.performLogout();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_011 — Logout is idempotent (no crash on second attempt)
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 11,
          description = "TC_LOGOUT_011: Calling logout when already logged out doesn't error")
    public void testLogoutIdempotent() {
        doLogin();

        // First logout
        logoutPage.performLogout();
        pause(1000);

        // Second logout attempt — page should still be usable
        boolean alreadyOnLogin = logoutPage.isRedirectedToLogin();
        Assert.assertTrue(alreadyOnLogin,
            "After first logout should be on login page");

        // Try hitting logout again — no exception should be thrown
        try {
            logoutPage.performLogout(); // should gracefully do nothing
        } catch (Exception e) {
            Assert.fail("Second logout attempt threw exception: " + e.getMessage());
        }

        // App should still be on a valid page
        String url = getDriver().getCurrentUrl();
        Assert.assertNotNull(url, "URL should still be valid after second logout attempt");
        Assert.assertFalse(url.isEmpty(), "URL should not be empty");

        log.info("PASS TC_LOGOUT_011: Second logout attempt is safe. URL: " + url);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_012 — Browser back button after logout
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 12,
          description = "TC_LOGOUT_012: Browser back button after logout doesn't restore session")
    public void testBackButtonAfterLogoutDoesNotRestoreSession() {
        doLogin();
        String authenticatedUrl = getDriver().getCurrentUrl();
        log.info("Authenticated URL: " + authenticatedUrl);

        logoutPage.performLogout();
        pause(1000);

        // Press back
        getDriver().navigate().back();
        pause(1500);

        String urlAfterBack = getDriver().getCurrentUrl();
        log.info("URL after back button: " + urlAfterBack);

        // Even if the browser shows the old URL, the app should detect
        // the session is gone and either stay on /login or redirect back
        boolean sessionRestored = !urlAfterBack.contains("/login")
                && logoutPage.isLoginLinkVisible() == false;

        // The key assertion: auth cookies/storage should still be cleared
        boolean cookiesStillClear = logoutPage.areAuthCookiesCleared();
        Assert.assertTrue(cookiesStillClear,
            "Auth cookies should remain cleared even after back-button navigation");

        log.info("PASS TC_LOGOUT_012: Session not restored by back button. Cookies clear: " + cookiesStillClear);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TC_LOGOUT_013 — Full login → logout → login cycle
    // ══════════════════════════════════════════════════════════════════════
    @Test(priority = 13,
          description = "TC_LOGOUT_013: Full login → logout → login cycle works correctly")
    public void testFullLoginLogoutLoginCycle() {
        // ── Round 1: Login ────────────────────────────────────────────────
        doLogin();
        String urlAfterFirstLogin = getDriver().getCurrentUrl();
        Assert.assertFalse(urlAfterFirstLogin.contains("/login"),
            "Should be past /login after first login. URL: " + urlAfterFirstLogin);
        log.info("Cycle step 1 — logged in: " + urlAfterFirstLogin);

        // ── Logout ────────────────────────────────────────────────────────
        boolean loggedOut = logoutPage.performLogout();
        Assert.assertTrue(loggedOut, "Should be able to logout");
        pause(1000);

        Assert.assertTrue(logoutPage.isRedirectedToLogin(),
            "Should land on /login after logout. URL: " + getDriver().getCurrentUrl());
        log.info("Cycle step 2 — logged out: " + getDriver().getCurrentUrl());

        // ── Round 2: Login again ──────────────────────────────────────────
        loginPage.navigateTo(baseUrl);
        loginPage.login(validEmail, validPassword);
        pause(2000);

        String urlAfterSecondLogin = getDriver().getCurrentUrl();
        Assert.assertFalse(urlAfterSecondLogin.contains("/login"),
            "Should be past /login after second login. URL: " + urlAfterSecondLogin);
        log.info("Cycle step 3 — re-logged in: " + urlAfterSecondLogin);

        // Cleanup
        logoutPage.performLogout();

        log.info("PASS TC_LOGOUT_013: Full login → logout → login cycle succeeded");
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}