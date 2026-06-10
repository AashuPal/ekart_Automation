package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;

/**
 * STEP 4 & 5: Page Object Model - Login Page
 * Refactored with full POM + reusable login/logout utilities
 */
public class LoginPage extends BasePage {

    // ===== STEP 5: Locators — matched to actual eKart React app =====
    private final By emailField    = By.xpath("//input[@placeholder='Email address']");
    private final By passwordField = By.xpath("//input[@placeholder='Password']");
    private final By loginButton   = By.xpath("//button[contains(.,'Sign In')]");
    private final By errorMessage  = By.xpath("//div[contains(@class,'bg-red-50') and contains(@class,'border-red-200')]");
    private final By successMsg    = By.cssSelector(".toast-success");
    private final By registerLink  = By.xpath("//a[contains(text(),'Create account')]");
    private final By forgotPwdLink = By.xpath("//a[contains(text(),'Forgot password')]");
    private final By pageHeading   = By.xpath("//h2[contains(text(),'Welcome Back')]");
    private final By emailTab      = By.xpath("//button[contains(.,'Email')]");

    // ===== Logout / Session Locators =====
    private final By userMenuBtn   = By.xpath("//button[contains(@class,'rounded-full')] | //button[contains(.,'Account')]");
    private final By logoutBtn     = By.xpath("//button[contains(text(),'Logout')] | //a[contains(text(),'Logout')] | //button[contains(text(),'Sign out')]");
    private final By navbarUserEl  = By.xpath("//*[contains(@class,'user') or contains(@class,'avatar') or contains(@class,'profile')]");
    private final By cartIcon      = By.xpath("//*[contains(@class,'cart') or contains(text(),'Cart')]");
    private final By homeLink      = By.xpath("//a[contains(@href,'/') and (contains(.,'Home') or contains(.,'eK'))]");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ===== STEP 6: Navigation =====
    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/login");
        log.info("Navigated to Login page");
    }

    // ===== Private Helper =====
    /** Ensure Email/Password tab is active */
    private void ensureEmailTab() {
        try {
            click(emailTab);
            Thread.sleep(300);
        } catch (Exception e) {
            // already on email tab
        }
    }

    // ===== STEP 11 & 12: Login Actions =====
    public void enterEmail(String email)   { type(emailField, email); }
    public void enterPassword(String pwd)  { type(passwordField, pwd); }
    public void clickLogin()               { click(loginButton); }
    public void clickRegisterLink()        { click(registerLink); }
    public void clickForgotPassword()      { click(forgotPwdLink); }

    /**
     * REUSABLE LOGIN UTILITY
     * Full login flow — switches to email tab, enters credentials, submits
     */
    public void login(String email, String password) {
        log.info("Logging in as: " + email);
        ensureEmailTab();
        enterEmail(email);
        enterPassword(password);
        clickLogin();
    }

    /**
     * REUSABLE LOGOUT UTILITY
     * Clicks user menu then logout button
     * Returns true if logout was successful
     */
    public boolean logout() {
        try {
            // Try clicking user menu first
            if (isDisplayed(userMenuBtn)) {
                click(userMenuBtn);
                Thread.sleep(500);
            }
            // Then click logout
            if (isDisplayed(logoutBtn)) {
                click(logoutBtn);
                log.info("Logout clicked successfully");
                return true;
            }
            log.warn("Logout button not found");
            return false;
        } catch (Exception e) {
            log.error("Logout failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * REUSABLE: Check if user is currently logged in
     * Checks for user menu or avatar in navbar
     */
    public boolean isLoggedIn() {
        return isDisplayed(userMenuBtn) || isDisplayed(navbarUserEl);
    }

    /**
     * REUSABLE: Check if user is on login page (session expired/logged out)
     */
    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("/login");
    }

    // ===== STEP 13: Validate Error Messages =====
    public boolean isErrorDisplayed()   { return isDisplayed(errorMessage); }
    public boolean isSuccessDisplayed() { return isDisplayed(successMsg); }
    public String  getErrorText()       { return getText(errorMessage); }
    public String  getHeading()         { return getText(pageHeading); }

    // ===== Session Helpers =====
    /** Clear localStorage to simulate session expiry */
    public void clearSession() {
        try {
            ((JavascriptExecutor) driver)
                .executeScript("localStorage.clear(); sessionStorage.clear();");
            log.info("Session cleared via JS");
        } catch (Exception e) {
            log.error("Failed to clear session: " + e.getMessage());
        }
    }

    /** Get token from localStorage */
    public String getSessionToken() {
        try {
            return (String) ((JavascriptExecutor) driver)
                .executeScript("return localStorage.getItem('token');");
        } catch (Exception e) {
            return null;
        }
    }

    /** Check if token exists in localStorage */
    public boolean isSessionTokenPresent() {
        String token = getSessionToken();
        return token != null && !token.isEmpty();
    }

    /** Navigate to a protected page */
    public void navigateToProtectedPage(String baseUrl) {
        driver.get(baseUrl + "/profile");
        log.info("Navigated to protected page: /profile");
    }
}