package com.ekart.pages;

import org.openqa.selenium.*;

/**
 * STEP 4 & 6: Home Page - Navigation Tests
 */
public class HomePage extends BasePage {

    private final By logo         = By.cssSelector("[class*='logo'], .brand, #logo");
    private final By loginBtn     = By.xpath("//a[contains(text(),'Login')] | //button[contains(text(),'Login')]");
    private final By registerBtn  = By.xpath("//a[contains(text(),'Register')] | //button[contains(text(),'Register')]");
    private final By searchBox    = By.cssSelector("input[type='search'], input[placeholder*='search' i]");
    private final By cartIcon     = By.cssSelector("[class*='cart'], #cart");
    private final By navMenu      = By.cssSelector("nav, .navbar");
    private final By pageHeading  = By.tagName("h1");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl);
        log.info("Navigated to Home: " + baseUrl);
    }

    public void clickLogin()    { click(loginBtn); }
    public void clickRegister() { click(registerBtn); }
    public void clickCart()     { click(cartIcon); }

    public void searchProduct(String query) {
        type(searchBox, query);
        driver.findElement(searchBox).sendKeys(Keys.ENTER);
    }

    public boolean isLogoDisplayed()   { return isDisplayed(logo); }
    public boolean isNavDisplayed()    { return isDisplayed(navMenu); }
    public boolean isCartDisplayed()   { return isDisplayed(cartIcon); }
    public String  getHeading()        { return isDisplayed(pageHeading) ? getText(pageHeading) : ""; }
}
