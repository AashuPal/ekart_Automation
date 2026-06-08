package com.ekart.utils;

import com.ekart.config.ConfigReader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

/**
 * ✅ STEP 7: Explicit Wait Helper
 */
public class WaitHelper {

    private WebDriver driver;
    private WebDriverWait wait;

    public WaitHelper(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
            Duration.ofSeconds(ConfigReader.getExplicitWait()));
    }

    /** Wait until element is visible */
    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Wait until element is clickable */
    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Wait until element contains text */
    public boolean waitForText(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /** Wait until URL contains string */
    public boolean waitForUrl(String urlFragment) {
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /** Wait until element disappears */
    public boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /** Wait for alert and return it */
    public Alert waitForAlert() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    /** Custom wait with message */
    public WebElement waitForVisible(By locator, String message) {
        WebDriverWait customWait = new WebDriverWait(driver,
            Duration.ofSeconds(ConfigReader.getExplicitWait()));
        return customWait.until(
            ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Fluent wait - polls every 500ms, ignores NoSuchElement */
    public WebElement fluentWait(By locator) {
        Wait<WebDriver> fluentWait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(30))
            .pollingEvery(Duration.ofMillis(500))
            .ignoring(NoSuchElementException.class);
        return fluentWait.until(d -> d.findElement(locator));
    }
}
