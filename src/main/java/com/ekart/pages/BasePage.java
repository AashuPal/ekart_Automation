package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BasePage {
    protected WebDriver driver;
    protected static final Logger log = LogManager.getLogger(BasePage.class);

    public BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    protected void click(By locator) {
        WaitUtils.waitForClickable(driver, locator).click();
    }

    protected void type(By locator, String text) {
        WebElement el = WaitUtils.waitForVisible(driver, locator);
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(By locator) {
        return WaitUtils.waitForVisible(driver, locator).getText();
    }

    protected boolean isDisplayed(By locator) {
        try { return driver.findElement(locator).isDisplayed(); }
        catch (NoSuchElementException e) { return false; }
    }

    protected String getAttribute(By locator, String attr) {
        return driver.findElement(locator).getAttribute(attr);
    }

    public String getPageTitle() { return driver.getTitle(); }
    public String getCurrentUrl() { return driver.getCurrentUrl(); }
}
