package com.ekart.base;

import com.ekart.config.ConfigReader;
import com.ekart.utils.DriverManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseTest {
    protected static final Logger log = LogManager.getLogger(BaseTest.class);
    protected String baseUrl;

    @BeforeClass
    public void setUp() {
        String browser = ConfigReader.getBrowser();
        baseUrl = ConfigReader.getBaseUrl();
        log.info("Starting | Browser: " + browser + " | URL: " + baseUrl);
        DriverManager.initDriver(browser);
    }

    @AfterClass
    public void tearDown() {
        log.info("Tests complete. Closing browser.");
        DriverManager.quitDriver();
    }

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
