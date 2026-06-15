package com.ekart.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * DynamicElementHandler
 *
 * Reusable utility for elements that:
 *  - Appear/disappear after API calls (product cards, filter chips)
 *  - Cause StaleElementReferenceException (list items re-rendered after sort/filter)
 *  - Load lazily (images, pagination)
 *  - Have race conditions between JS state and DOM
 *
 * Usage:
 *   int count = DynamicElementHandler.retryGetCount(driver, By.cssSelector("a[href*='/product/']"));
 *   DynamicElementHandler.waitForCountToStabilize(driver, cardLocator, 500);
 */
public class DynamicElementHandler {

    private static final Logger log = LogManager.getLogger(DynamicElementHandler.class);
    private static final int DEFAULT_TIMEOUT   = 15;
    private static final int DEFAULT_RETRIES   = 3;
    private static final long POLL_MS          = 300;

    // =========================================================================
    // Stale Element Safe Interactions
    // =========================================================================

    /**
     * Click an element with automatic retry on StaleElementReferenceException.
     * Re-fetches the element before each retry attempt.
     */
    public static void safeClick(WebDriver driver, By locator) {
        for (int attempt = 1; attempt <= DEFAULT_RETRIES; attempt++) {
            try {
                WaitUtils.waitForClickable(driver, locator).click();
                return;
            } catch (StaleElementReferenceException e) {
                log.warn("StaleElement on click attempt " + attempt + "/" + DEFAULT_RETRIES
                    + " for: " + locator);
                if (attempt == DEFAULT_RETRIES) throw e;
                sleep(POLL_MS);
            }
        }
    }

    /**
     * Get text from an element with automatic retry on StaleElementReferenceException.
     */
    public static String safeGetText(WebDriver driver, By locator) {
        for (int attempt = 1; attempt <= DEFAULT_RETRIES; attempt++) {
            try {
                return WaitUtils.waitForVisible(driver, locator).getText().trim();
            } catch (StaleElementReferenceException e) {
                log.warn("StaleElement on getText attempt " + attempt + "/" + DEFAULT_RETRIES);
                if (attempt == DEFAULT_RETRIES) throw e;
                sleep(POLL_MS);
            }
        }
        return "";
    }

    /**
     * Get text from a specific element in a list by index — safe against re-renders.
     * Re-fetches the whole list each time.
     */
    public static String safeGetTextAt(WebDriver driver, By listLocator, int index) {
        for (int attempt = 1; attempt <= DEFAULT_RETRIES; attempt++) {
            try {
                List<WebElement> items = driver.findElements(listLocator);
                if (index >= items.size()) return "";
                return items.get(index).getText().trim();
            } catch (StaleElementReferenceException e) {
                log.warn("StaleElement on getTextAt[" + index + "] attempt " + attempt);
                sleep(POLL_MS);
            }
        }
        return "";
    }

    /**
     * Get an attribute from a list item at index — safe against re-renders.
     */
    public static String safeGetAttributeAt(WebDriver driver, By listLocator,
                                            int index, String attr) {
        for (int attempt = 1; attempt <= DEFAULT_RETRIES; attempt++) {
            try {
                List<WebElement> items = driver.findElements(listLocator);
                if (index >= items.size()) return "";
                return items.get(index).getAttribute(attr);
            } catch (StaleElementReferenceException e) {
                log.warn("StaleElement on getAttributeAt[" + index + "] attempt " + attempt);
                sleep(POLL_MS);
            }
        }
        return "";
    }

    // =========================================================================
    // Dynamic Count & Stability Waits
    // =========================================================================

    /**
     * Get element count with retry — safe if the list re-renders mid-call.
     */
    public static int retryGetCount(WebDriver driver, By locator) {
        for (int attempt = 1; attempt <= DEFAULT_RETRIES; attempt++) {
            try {
                return driver.findElements(locator).size();
            } catch (Exception e) {
                log.warn("retryGetCount attempt " + attempt + ": " + e.getMessage());
                sleep(POLL_MS);
            }
        }
        return 0;
    }

    /**
     * Wait until element count stabilizes (same value for stabilizeMs).
     * Useful after sort/filter: count may flicker as cards re-render.
     *
     * @param stabilizeMs  how long (ms) count must stay unchanged to be "stable"
     */
    public static int waitForCountToStabilize(WebDriver driver, By locator, long stabilizeMs) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        int[] stable = {-1};
        long[] stableSince = {0};

        wait.until(d -> {
            int current = d.findElements(locator).size();
            if (current != stable[0]) {
                stable[0]     = current;
                stableSince[0] = System.currentTimeMillis();
            }
            return (System.currentTimeMillis() - stableSince[0]) >= stabilizeMs;
        });
        return stable[0];
    }

    /**
     * Wait for element count to change from a baseline value.
     */
    public static void waitForCountToChangeFrom(WebDriver driver, By locator, int baseline) {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
            .until(d -> d.findElements(locator).size() != baseline);
        log.info("Count changed from " + baseline + " to "
            + driver.findElements(locator).size());
    }

    /**
     * Wait for element count to be at least minCount.
     */
    public static void waitForMinCount(WebDriver driver, By locator, int minCount) {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
            .until(d -> d.findElements(locator).size() >= minCount);
    }

    // =========================================================================
    // Loader / Spinner Waits
    // =========================================================================

    /**
     * Wait for all loading spinners to disappear.
     */
    public static void waitForLoadersToDisappear(WebDriver driver) {
        By spinners = By.cssSelector(".animate-spin");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
                .until(d -> d.findElements(spinners).isEmpty());
        } catch (TimeoutException e) {
            log.warn("Spinners still visible after " + DEFAULT_TIMEOUT + "s");
        }
    }

    /**
     * Wait for skeleton placeholder cards to disappear (used during initial load).
     */
    public static void waitForSkeletonsToDisappear(WebDriver driver) {
        By skeletons = By.cssSelector(".skeleton, .animate-pulse");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
                .until(d -> d.findElements(skeletons).isEmpty() ||
                           !d.findElements(By.cssSelector("a[href*='/product/']")).isEmpty());
        } catch (TimeoutException e) {
            log.warn("Skeletons still visible after timeout");
        }
    }

    // =========================================================================
    // Conditional Existence
    // =========================================================================

    /**
     * Returns true if the element appears within timeoutSeconds.
     * Never throws — safe for optional/dynamic elements.
     */
    public static boolean appearsWithin(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Returns true if the element disappears within timeoutSeconds.
     */
    public static boolean disappearsWithin(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // =========================================================================
    // Retry with Lambda (generic)
    // =========================================================================

    /**
     * Retry a Supplier<T> up to maxAttempts times on any exception.
     * Returns the result of the first successful attempt, or throws on last failure.
     *
     * Example:
     *   int count = DynamicElementHandler.retry(() -> driver.findElements(by).size(), 3);
     */
    public static <T> T retry(Supplier<T> action, int maxAttempts) {
        Exception last = null;
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                return action.get();
            } catch (Exception e) {
                last = e;
                log.warn("Retry " + i + "/" + maxAttempts + ": " + e.getMessage());
                sleep(POLL_MS);
            }
        }
        throw new RuntimeException("All " + maxAttempts + " attempts failed", last);
    }

    // =========================================================================
    // URL Change Wait
    // =========================================================================

    /**
     * Wait for the URL to change from the given URL.
     * Useful after clicking a link when you don't know the target URL in advance.
     */
    public static void waitForUrlToChangeFrom(WebDriver driver, String previousUrl) {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT))
            .until(d -> !d.getCurrentUrl().equals(previousUrl));
    }

    // =========================================================================
    // Private
    // =========================================================================

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}