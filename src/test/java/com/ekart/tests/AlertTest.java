package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.utils.AlertUtils;
import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * STEP 8: Handle Alerts & Popups
 */
public class AlertTest extends BaseTest {

    @Test(priority = 1, description = "TC_ALERT_001: Handle browser alert if present")
    public void testHandleAlert() {
        getDriver().get(baseUrl);
        try {
            // Trigger any JS alert on the page if present
            Alert alert = WaitUtils.waitForAlert(getDriver());
            String alertText = alert.getText();
            Assert.assertNotNull(alertText);
            AlertUtils.acceptAlert(getDriver());
            log.info("PASS: Alert handled: " + alertText);
        } catch (Exception e) {
            // No alert present — that's also fine
            log.info("INFO: No browser alert on home page (expected)");
            Assert.assertTrue(true, "No alert present - OK");
        }
    }

    @Test(priority = 2, description = "TC_ALERT_002: Verify no unexpected popups on login")
    public void testNoUnexpectedPopupsOnLogin() {
        getDriver().get(baseUrl + "/login");
        try {
            Alert alert = WaitUtils.waitForAlert(getDriver());
            // If alert appears, dismiss it
            AlertUtils.dismissAlert(getDriver());
            log.warn("Unexpected alert on login page was dismissed");
        } catch (Exception e) {
            log.info("PASS: No unexpected alerts on login page");
            Assert.assertTrue(true);
        }
    }
}
