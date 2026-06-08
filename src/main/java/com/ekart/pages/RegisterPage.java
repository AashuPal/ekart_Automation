package com.ekart.pages;

import org.openqa.selenium.*;

/**
 * STEP 4 & 5: Page Object Model - Registration Page Contains all locators (id,
 * xpath, css) and actions
 */
public class RegisterPage extends BasePage {

	// ===== STEP 5: Locators (id, xpath, css) =====
	private final By nameField = By.xpath("//input[@placeholder='Full Name']");
	private final By emailField = By.xpath("//input[@placeholder='Email Address']");
	private final By phoneField = By.xpath("//input[@placeholder='Phone Number (optional)']");
	private final By passwordField = By.xpath("//input[@placeholder='Password (min. 6 characters)']");
	private final By registerButton = By.xpath("//button[contains(.,'Create Account')]");
	private final By errorMessage = By.xpath("//div[contains(@class,'bg-red-50')]");
	private final By successMessage = By.cssSelector(".toast-success");

	// Field-level validation error locators
	private final By nameError = By.xpath("//input[@id='name']/following-sibling::*[contains(@class,'error')]");
	private final By emailError = By.xpath("//input[@id='email']/following-sibling::*[contains(@class,'error')]");
	private final By passwordError = By.xpath("//input[@id='password']/following-sibling::*[contains(@class,'error')]");

	// Navigation link
	private final By loginLink = By.xpath("//a[contains(text(),'Login') or contains(text(),'Sign in')]");
	private final By pageHeading = By.xpath("//h1 | //h2");

	public RegisterPage(WebDriver driver) {
		super(driver);
	}

	// ===== STEP 9: Registration Actions =====
	public void enterName(String name) {
		type(nameField, name);
	}

	public void enterEmail(String email) {
		type(emailField, email);
	}

	public void enterPhone(String phone) {
		type(phoneField, phone);
	}

	public void enterPassword(String pwd) {
		type(passwordField, pwd);
	}


	public void clickRegister() {
		click(registerButton);
	}

	public void clickLoginLink() {
		click(loginLink);
	}

	// Register with all fields at once
	public void registerUser(String name, String email, String phone, String password) {
		log.info("Registering user: " + email);
		enterName(name);
		enterEmail(email);
		enterPhone(phone);
		enterPassword(password);
		clickRegister();
	}

	// ===== STEP 10: Validate Input Fields =====
	public boolean isNameErrorDisplayed() {
		return isDisplayed(nameError);
	}

	public boolean isEmailErrorDisplayed() {
		return isDisplayed(emailError);
	}

	public boolean isPasswordErrorDisplayed() {
		return isDisplayed(passwordError);
	}

	public boolean isSuccessDisplayed() {
		return isDisplayed(successMessage);
	}

	public boolean isErrorDisplayed() {
		return isDisplayed(errorMessage);
	}

	public String getErrorText() {
		return getText(errorMessage);
	}

	public String getSuccessText() {
		return getText(successMessage);
	}

	public String getHeading() {
		return getText(pageHeading);
	}

	// ===== STEP 6: Navigation =====
	public void navigateTo(String baseUrl) {
		driver.get(baseUrl + "/register");
		log.info("Navigated to Register page");
	}
}
