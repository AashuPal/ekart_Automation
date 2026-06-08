package com.ekart.utils;

import org.openqa.selenium.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * STEP 8: Handle Alerts & Popups
 */
public class AlertUtils {

    private static final Logger log = LogManager.getLogger(AlertUtils.class);

    // Accept (OK) alert
    public static String acceptAlert(WebDriver driver) {
        try {
            Alert alert = WaitUtils.waitForAlert(driver);
            String text = alert.getText();
            log.info("Accepting alert: " + text);
            alert.accept();
            return text;
        } catch (NoAlertPresentException e) {
            log.warn("No alert present");
            return null;
        }
    }

    // Dismiss (Cancel) alert
    public static String dismissAlert(WebDriver driver) {
        try {
            Alert alert = WaitUtils.waitForAlert(driver);
            String text = alert.getText();
            log.info("Dismissing alert: " + text);
            alert.dismiss();
            return text;
        } catch (NoAlertPresentException e) {
            log.warn("No alert present");
            return null;
        }
    }

    // Get alert text without accepting
    public static String getAlertText(WebDriver driver) {
        try {
            Alert alert = WaitUtils.waitForAlert(driver);
            return alert.getText();
        } catch (NoAlertPresentException e) {
            return null;
        }
    }

    // Handle prompt alert with input
    public static void sendTextToAlert(WebDriver driver, String text) {
        Alert alert = WaitUtils.waitForAlert(driver);
        alert.sendKeys(text);
        alert.accept();
    }

    // Switch to iframe
    public static void switchToFrame(WebDriver driver, String frameId) {
        driver.switchTo().frame(frameId);
    }

    // Switch back to main content
    public static void switchToDefault(WebDriver driver) {
        driver.switchTo().defaultContent();
    }

    // Handle new window/tab
    public static void switchToNewWindow(WebDriver driver, String mainHandle) {
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(mainHandle)) {
                driver.switchTo().window(handle);
                break;
            }
        }
    }
}
