package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.HomePage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * STEP 6: Basic Navigation Tests
 * STEP 14: Assertions
 */
public class NavigationTest extends BaseTest {

    @Test(priority = 1, description = "Verify home page loads successfully")
    public void testHomePageLoads() {
        HomePage homePage = new HomePage(getDriver());
        homePage.navigateTo(baseUrl);

        // STEP 14: Assertions
        Assert.assertTrue(getDriver().getTitle().length() > 0,
            "Page title should not be empty");
        Assert.assertTrue(getDriver().getCurrentUrl().contains(
            baseUrl.replace("https://","").replace("http://","")),
            "Should be on home page");
        log.info("PASS: Home page loaded | Title: " + getDriver().getTitle());
    }

    @Test(priority = 2, description = "Verify navigation to Login page")
    public void testNavigateToLogin() {
        getDriver().get(baseUrl + "/login");
        WaitUtils.waitForUrl(getDriver(), "login");
        Assert.assertTrue(getDriver().getCurrentUrl().contains("login"),
            "URL should contain 'login'");
        log.info("PASS: Navigated to login page");
    }

    @Test(priority = 3, description = "Verify navigation to Register page")
    public void testNavigateToRegister() {
        getDriver().get(baseUrl + "/register");
        WaitUtils.waitForUrl(getDriver(), "register");
        Assert.assertTrue(getDriver().getCurrentUrl().contains("register"),
            "URL should contain 'register'");
        log.info("PASS: Navigated to register page");
    }

    @Test(priority = 4, description = "Verify browser back navigation")
    public void testBrowserBackNavigation() {
        getDriver().get(baseUrl);
        getDriver().get(baseUrl + "/login");
        getDriver().navigate().back();
        Assert.assertEquals(getDriver().getCurrentUrl(), baseUrl + "/",
            "Should return to home page");
        log.info("PASS: Browser back navigation works");
    }

    @Test(priority = 5, description = "Verify page refresh")
    public void testPageRefresh() {
        getDriver().get(baseUrl);
        String titleBefore = getDriver().getTitle();
        getDriver().navigate().refresh();
        String titleAfter = getDriver().getTitle();
        Assert.assertEquals(titleBefore, titleAfter,
            "Title should be same after refresh");
        log.info("PASS: Page refresh works");
    }
}
