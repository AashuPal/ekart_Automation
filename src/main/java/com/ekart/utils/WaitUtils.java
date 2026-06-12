package com.ekart.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;

/**
 * STEP 7: Implicit & Explicit Waits
 */
public class WaitUtils {

	private static final int DEFAULT_TIMEOUT = 15;

	/** Wait until element is VISIBLE */
	public static WebElement waitForVisible(WebDriver driver, By locator) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	/** Wait until element is VISIBLE with custom timeout */
	public static WebElement waitForVisible(WebDriver driver, By locator, int timeout) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	/** Wait until element is CLICKABLE */
	public static WebElement waitForClickable(WebDriver driver, By locator) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(ExpectedConditions.elementToBeClickable(locator));
	}

	/** Wait for text to be present in element */
	public static boolean waitForText(WebDriver driver, By locator, String text) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
	}

	/** Wait for URL to contain a fragment */
	public static boolean waitForUrl(WebDriver driver, String urlFragment) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(ExpectedConditions.urlContains(urlFragment));
	}

	/** Wait for URL to NOT contain a fragment (e.g. after login redirect) */
	public static boolean waitForUrlNotContains(WebDriver driver, String urlFragment) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(d -> !d.getCurrentUrl().contains(urlFragment));
	}

	/** Wait for alert to be present */
	public static Alert waitForAlert(WebDriver driver) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(ExpectedConditions.alertIsPresent());
	}

	/**
	 * Wait until element is INVISIBLE (disappears from DOM/view) Use this for
	 * loading spinners, overlays etc.
	 */
	public static boolean waitForInvisible(WebDriver driver, By locator) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
	}

	/**
	 * Safe version of waitForInvisible — uses short timeout (5s) Does NOT throw
	 * exception if element is never found (already invisible = OK) Use this for
	 * optional spinners that may or may not appear
	 */
	public static boolean waitForInvisibleSafe(WebDriver driver, By locator) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
			return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
		} catch (Exception e) {
			return true; // element not present = already invisible = OK
		}
	}

	/** Fluent wait — polls every 500ms, ignores NoSuchElement + StaleElement */
	public static WebElement fluentWait(WebDriver driver, By locator) {
		FluentWait<WebDriver> fluentWait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(20))
				.pollingEvery(Duration.ofMillis(100)).ignoring(NoSuchElementException.class)
				.ignoring(StaleElementReferenceException.class);
		return fluentWait.until(d -> d.findElement(locator));
	}

	// Wait for document.readyState == "complete" — replaces Thread.sleep after
	// navigate/refresh
	public static void waitForPageReady(WebDriver driver) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
		wait.until(d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
	}
}