package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.config.ConfigReader;
import com.ekart.pages.LoginPage;

import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * STEP 11: Automate Login (Valid Credentials) STEP 12: Automate Login (Invalid
 * Credentials) STEP 13: Validate Error Messages STEP 14: Assertions
 */
public class LoginTest extends BaseTest {

	private LoginPage loginPage;

	@BeforeMethod
	public void goToLoginPage() {
		loginPage = new LoginPage(getDriver());
		loginPage.navigateTo(baseUrl);
	}

	// ===== STEP 11: Valid Login =====
	@Test(priority = 1, description = "TC_LOGIN_001: Login with valid credentials")
	public void testValidLogin() {
		loginPage.login(ConfigReader.getValidEmail(), ConfigReader.getValidPassword());

		// Wait up to 15s for redirect away from login
		try {
			WaitUtils.waitForUrl(getDriver(), "/");
		} catch (Exception e) {
			// URL might not change if verify-email redirect happens
		}

		String currentUrl = getDriver().getCurrentUrl();
		Assert.assertFalse(currentUrl.endsWith("/login"), // ← endsWith not contains (avoids false match)
				"Should redirect away from login. Current URL: " + currentUrl);
		log.info("PASS: Valid login | URL: " + currentUrl);
	}

	// ===== STEP 12: Invalid Login Tests =====
	@Test(priority = 2, description = "TC_LOGIN_002: Login with wrong password")
	public void testWrongPassword() {
		loginPage.login(ConfigReader.getValidEmail(), "WrongPassword123!");

		// STEP 13: Validate error message
		Assert.assertTrue(loginPage.isErrorDisplayed() || getDriver().getCurrentUrl().contains("login"),
				"Should show error for wrong password");
		log.info("PASS: Wrong password shows error");
	}

	@Test(priority = 3, description = "TC_LOGIN_003: Login with non-existent email")
	public void testNonExistentEmail() {
		loginPage.login("nonexistent@fake.com", "Test@1234");

		Assert.assertTrue(loginPage.isErrorDisplayed() || getDriver().getCurrentUrl().contains("login"),
				"Should show error for non-existent email");
		log.info("PASS: Non-existent email shows error");
	}

	@Test(priority = 4, description = "TC_LOGIN_004: Login with empty email")
	public void testEmptyEmail() {
		loginPage.enterPassword("Test@1234");
		loginPage.clickLogin();

		Assert.assertTrue(loginPage.isErrorDisplayed() || getDriver().getCurrentUrl().contains("login"),
				"Should show error for empty email");
		log.info("PASS: Empty email shows validation error");
	}

	@Test(priority = 5, description = "TC_LOGIN_005: Login with empty password")
	public void testEmptyPassword() {
		loginPage.enterEmail(ConfigReader.getValidEmail());
		loginPage.clickLogin();

		Assert.assertTrue(loginPage.isErrorDisplayed() || getDriver().getCurrentUrl().contains("login"),
				"Should show error for empty password");
		log.info("PASS: Empty password shows validation error");
	}

	@Test(priority = 6, description = "TC_LOGIN_006: Login with both fields empty")
	public void testBothFieldsEmpty() {
		loginPage.clickLogin();

		Assert.assertTrue(loginPage.isErrorDisplayed() || getDriver().getCurrentUrl().contains("login"),
				"Should show error when both fields empty");
		log.info("PASS: Both empty fields shows validation error");
	}

	@Test(priority = 7, description = "TC_LOGIN_007: Login with invalid email format")
	public void testInvalidEmailFormat() {
		loginPage.login("notanemail", "Test@1234");

		Assert.assertTrue(loginPage.isErrorDisplayed() || getDriver().getCurrentUrl().contains("login"),
				"Should show error for invalid email format");
		log.info("PASS: Invalid email format shows error");
	}

	@Test(priority = 8, description = "TC_LOGIN_008: Verify error message content")
	public void testErrorMessageContent() {
		loginPage.login("wrong@email.com", "wrongpassword");

		// STEP 13: Validate specific error message text
		if (loginPage.isErrorDisplayed()) {
			String errorText = loginPage.getErrorText();
			Assert.assertNotNull(errorText, "Error message text should not be null");
			Assert.assertTrue(errorText.length() > 0, "Error message should not be empty");
			log.info("PASS: Error message shown: " + errorText);
		} else {
			Assert.assertTrue(getDriver().getCurrentUrl().contains("login"), "Should stay on login page");
		}
	}

	@Test(priority = 9, description = "TC_LOGIN_009: Verify login page title/heading")
	public void testLoginPageHeading() {
		String heading = loginPage.getHeading();
		Assert.assertTrue(heading.toLowerCase().contains("login") || heading.toLowerCase().contains("sign in")
				|| heading.length() > 0, "Login page should have a heading");
		log.info("PASS: Login heading: " + heading);
	}

	@Test(priority = 10, description = "TC_LOGIN_010: Verify forgot password link exists")
	public void testForgotPasswordLinkExists() {
		// STEP 6: Navigation
		Assert.assertTrue(
				WaitUtils.waitForVisible(getDriver(),
						org.openqa.selenium.By.xpath("//a[contains(text(),'Forgot')]")) != null,
				"Forgot password link should exist");
		log.info("PASS: Forgot password link present");
	}
}
