package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.RegisterPage;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * STEP 9: Automate User Registration Flow
 * STEP 10: Validate Input Fields
 * STEP 13: Validate Error Messages
 * STEP 14: Assertions
 */
public class RegistrationTest extends BaseTest {

    private RegisterPage registerPage;

    @BeforeMethod
    public void goToRegisterPage() {
        registerPage = new RegisterPage(getDriver());
        registerPage.navigateTo(baseUrl);
    }

    // ===== STEP 9: Valid Registration =====
    @Test(priority = 1, description = "TC_REG_001: Register with valid credentials")
    public void testValidRegistration() {
        String uniqueEmail = "testuser_" + System.currentTimeMillis() + "@test.com";

        registerPage.registerUser(
            "Test User",
            uniqueEmail,
            "Test@1234",
            "9876543210"
        );

        // Wait for redirect away from register (app may go to /verify, /login, or home)
        try {
            WaitUtils.waitForUrlNotContains(getDriver(), "/register");
        } catch (Exception e) {
            log.warn("Registration redirect wait timed out — checking URL anyway");
        }

        String currentUrl = getDriver().getCurrentUrl();
        Assert.assertTrue(
            currentUrl.contains("verify") ||
            currentUrl.contains("login") ||
            !currentUrl.contains("register"),  // ← redirected away from register = success
            "Registration should redirect away from register page. Current URL: " + currentUrl
        );
        log.info("PASS: Valid registration | Email: " + uniqueEmail + " | URL: " + currentUrl);
    }

    // ===== STEP 10 & 13: Input Validation Tests =====
    @Test(priority = 2, description = "TC_REG_002: Register with empty fields")
    public void testEmptyFieldsValidation() {
        // App validates in JS before submit — just click register
        registerPage.clickRegister();

        // HTML5 required fields prevent submit, stays on register page
        Assert.assertTrue(
            registerPage.isErrorDisplayed() ||
            getDriver().getCurrentUrl().contains("register"),
            "Should show error or stay on register page"
        );
        log.info("PASS: Empty fields validation works");
    }

    @Test(priority = 3, description = "TC_REG_003: Register with invalid email format")
    public void testInvalidEmailFormat() {
        registerPage.enterName("Test User");
        registerPage.enterEmail("invalidemail"); // no @ symbol — HTML5 blocks this
        registerPage.enterPhone("9876543210");
        registerPage.enterPassword("Test@1234");
        registerPage.clickRegister();

        // HTML5 email validation keeps user on page
        Assert.assertTrue(
            registerPage.isErrorDisplayed() ||
            getDriver().getCurrentUrl().contains("register"),
            "Should show invalid email error"
        );
        log.info("PASS: Invalid email validation works");
    }

    @Test(priority = 4, description = "TC_REG_004: Register with weak password (less than 6 chars)")
    public void testWeakPassword() {
        registerPage.enterName("Test User");
        registerPage.enterEmail("weak" + System.currentTimeMillis() + "@test.com");
        registerPage.enterPhone("9876543210");
        registerPage.enterPassword("123"); // less than 6 chars — app blocks this
        registerPage.clickRegister();

        // App shows: "Password must be at least 6 characters."
        Assert.assertTrue(
            registerPage.isErrorDisplayed() ||
            getDriver().getCurrentUrl().contains("register"),
            "Should show weak password error"
        );
        if (registerPage.isErrorDisplayed()) {
            String error = registerPage.getErrorText();
            Assert.assertTrue(
                error.toLowerCase().contains("password") ||
                error.toLowerCase().contains("6"),
                "Error should mention password length"
            );
        }
        log.info("PASS: Weak password validation works");
    }

    @Test(priority = 5, description = "TC_REG_005: Register with missing required fields")
    public void testMissingRequiredFields() {
        // Only enter name, skip email and password
        registerPage.enterName("Test User");
        registerPage.clickRegister();

        // App checks: if (!form.name || !form.emailId || !form.password)
        Assert.assertTrue(
            registerPage.isErrorDisplayed() ||
            getDriver().getCurrentUrl().contains("register"),
            "Should show required fields error"
        );
        if (registerPage.isErrorDisplayed()) {
            String error = registerPage.getErrorText();
            Assert.assertTrue(
                error.toLowerCase().contains("required") ||
                error.toLowerCase().contains("fill"),
                "Error should mention required fields"
            );
        }
        log.info("PASS: Missing required fields validation works");
    }

    @Test(priority = 6, description = "TC_REG_006: Register with already existing email")
    public void testDuplicateEmail() {
        registerPage.registerUser(
            "Test User",
            "aashupalse@gmail.com", // existing email
            "Test@1234",
            "9876543210"
        );

        Assert.assertTrue(
            registerPage.isErrorDisplayed() ||
            getDriver().getCurrentUrl().contains("register"),
            "Should show duplicate email error"
        );
        log.info("PASS: Duplicate email validation works");
    }

    @Test(priority = 7, description = "TC_REG_007: Verify all required fields are present on page")
    public void testFormFieldsExist() {
        // STEP 10: Verify fields exist using correct placeholders
        Assert.assertNotNull(
            WaitUtils.waitForVisible(getDriver(),
                By.xpath("//input[@placeholder='Full Name']")),
            "Name field should be present"
        );
        Assert.assertNotNull(
            WaitUtils.waitForVisible(getDriver(),
                By.xpath("//input[@placeholder='Email Address']")),
            "Email field should be present"
        );
        Assert.assertNotNull(
            WaitUtils.waitForVisible(getDriver(),
                By.xpath("//input[@placeholder='Password (min. 6 characters)']")),
            "Password field should be present"
        );
        log.info("PASS: All required form fields present");
    }
}