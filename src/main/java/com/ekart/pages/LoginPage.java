package com.ekart.pages;

import org.openqa.selenium.*;

/**
 * STEP 4 & 5: Page Object - Login Page
 * Locators updated to match actual eKart React app placeholders
 */
public class LoginPage extends BasePage {

    // ===== STEP 5: Locators — matched to actual React app =====
    private final By emailField    = By.xpath("//input[@placeholder='Email address']");
    private final By passwordField = By.xpath("//input[@placeholder='Password']");
    private final By loginButton   = By.xpath("//button[contains(.,'Sign In')]");
    private final By errorMessage  = By.xpath("//div[contains(@class,'bg-red-50') and contains(@class,'border-red')]");
    private final By successMsg    = By.cssSelector(".toast-success");
    private final By registerLink  = By.xpath("//a[contains(text(),'Create account')]");
    private final By forgotPwdLink = By.xpath("//a[contains(text(),'Forgot password')]");
    private final By pageHeading   = By.xpath("//h2[contains(text(),'Welcome Back')]");
    private final By userAvatar    = By.xpath(
        "//*[contains(@class,'avatar') or contains(@class,'user-icon') " +
        "or contains(@class,'user-menu') or contains(@class,'profile') " +
        "or contains(@class,'account') or @data-testid='user-menu']"
    );
    private final By logoutBtn     = By.xpath(
        "//button[normalize-space()='Logout' or normalize-space()='Log Out' " +
        "or normalize-space()='Sign Out'] " +
        "| //a[normalize-space()='Logout' or normalize-space()='Log Out' " +
        "or normalize-space()='Sign Out']"
    );

    // Email tab button (login page has Email / Phone tabs)
    private final By emailTab      = By.xpath("//button[contains(.,'Email')]");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ===== STEP 11 & 12: Login Actions =====
    public void enterEmail(String email)   { type(emailField, email); }
    public void enterPassword(String pwd)  { type(passwordField, pwd); }
    public void clickLogin()               { click(loginButton); }
    public void clickRegisterLink()        { click(registerLink); }
    public void clickForgotPassword()      { click(forgotPwdLink); }

    /** Ensure Email tab is active before interacting with email/password fields */
    private void ensureEmailTab() {
        try {
            click(emailTab);
            Thread.sleep(300); // wait for tab switch animation
        } catch (Exception e) {
            // already on email tab
        }
    }

    /** Login with email and password */
    public void login(String email, String password) {
        log.info("Logging in as: " + email);
        ensureEmailTab();
        enterEmail(email);
        enterPassword(password);
        clickLogin();
    }

    /** Clear fields */
    public void clearEmail() {
        driver.findElement(emailField).clear();
    }
    public void clearPassword() {
        driver.findElement(passwordField).clear();
    }

    // ===== STEP 13: Validate Error Messages =====
    public boolean isErrorDisplayed()   { return isDisplayed(errorMessage); }
    public boolean isSuccessDisplayed() { return isDisplayed(successMsg); }
    public boolean isUserLoggedIn()     { return isDisplayed(userAvatar); }
    public boolean isLoggedIn()         { return isDisplayed(userAvatar); }
    public String  getErrorText()       { return getText(errorMessage); }
    public String  getHeading()         { return getText(pageHeading); }

    // ===== STEP 6: Navigation =====
    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/login");
        log.info("Navigated to Login page");
    }

    public void logout() {
        if (isDisplayed(userAvatar)) {
            click(userAvatar);
            click(logoutBtn);
        }
    }
}