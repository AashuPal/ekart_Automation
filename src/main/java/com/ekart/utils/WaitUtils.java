package com.ekart.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;

/**
 * STEP 7: Implicit & Explicit Waits
 */
public class WaitUtils {

    private static final int DEFAULT_TIMEOUT = 15;

    // Explicit wait - wait until element is visible
    public static WebElement waitForVisible(WebDriver driver, By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // Wait until element is clickable
    public static WebElement waitForClickable(WebDriver driver, By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // Wait for element with custom timeout
    public static WebElement waitForVisible(WebDriver driver, By locator, int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // Wait for text to be present in element
    public static boolean waitForText(WebDriver driver, By locator, String text) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    // Wait for URL to contain a fragment
    public static boolean waitForUrl(WebDriver driver, String urlFragment) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /**
     * Wait for the URL to NOT contain the given fragment.
     * Useful for waiting until navigated away from a page (e.g. after login).
     */
    public static boolean waitForUrlNotContains(WebDriver driver, String urlFragment) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(d -> !d.getCurrentUrl().contains(urlFragment));
    }

    // Wait for alert
    public static Alert waitForAlert(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    // Fluent wait - polls every 500ms
    public static WebElement fluentWait(WebDriver driver, By locator) {
        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
            .withTimeout(Duration.ofSeconds(20))
            .pollingEvery(Duration.ofMillis(500))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
        return fluentWait.until(d -> d.findElement(locator));
    }
}