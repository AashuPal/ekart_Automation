package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;

/**
 * Page Object: Checkout Page (/checkout)
 *
 * 3-step checkout flow:
 *  Step 1 — Shipping Address (fullName, addressLine1, city, state, postalCode, phone)
 *  Step 2 — Payment Method  (COD | CARD | UPI) + card details if CARD
 *  Step 3 — Review & Place Order
 *
 * Validation rules (from frontend):
 *  Required: addressLine1, city, postalCode
 *  Optional: fullName, addressLine2, state, phone
 *  postalCode: max 6 digits
 *  phone: max 10 digits
 */
public class CheckoutPage extends BasePage {

    protected static final Logger log = LogManager.getLogger(CheckoutPage.class);

    // ── Progress steps ────────────────────────────────────────────────────────
    private static final By STEP_HEADING =
        By.xpath("//h2[contains(@class,'font-bold') and contains(@class,'text-gray-900')]");

    private static final By STEP_SHIPPING_LABEL =
        By.xpath("//span[text()='Shipping']");

    private static final By STEP_PAYMENT_LABEL =
        By.xpath("//span[text()='Payment']");

    private static final By STEP_REVIEW_LABEL =
        By.xpath("//span[text()='Review']");

    // ── Step 1: Shipping Address fields ──────────────────────────────────────
    private static final By FIELD_FULL_NAME =
        By.xpath("//input[@placeholder='Enter full name']");

    private static final By FIELD_ADDRESS_LINE1 =
        By.xpath("//input[@placeholder='Street address, house number']");

    private static final By FIELD_ADDRESS_LINE2 =
        By.xpath("//input[@placeholder='Apartment, landmark, etc.']");

    private static final By FIELD_CITY =
        By.xpath("//input[@placeholder='City']");

    private static final By FIELD_STATE =
        By.xpath("//input[@placeholder='State']");

    private static final By FIELD_POSTAL_CODE =
        By.xpath("//input[@placeholder='6-digit PIN code']");

    private static final By FIELD_PHONE =
        By.xpath("//input[@placeholder='10-digit mobile number']");

    private static final By BTN_CONTINUE_TO_PAYMENT =
        By.xpath("//button[normalize-space(text())='Continue to Payment']");

    // ── Step 2: Payment Method ────────────────────────────────────────────────
    private static final By RADIO_COD =
        By.xpath("//input[@type='radio' and @value='COD']");

    private static final By RADIO_CARD =
        By.xpath("//input[@type='radio' and @value='CARD']");

    private static final By RADIO_UPI =
        By.xpath("//input[@type='radio' and @value='UPI']");

    // Card detail fields (visible only when CARD selected)
    private static final By FIELD_CARD_NUMBER =
        By.xpath("//input[@placeholder='1234 5678 9012 3456']");

    private static final By FIELD_CARD_EXPIRY =
        By.xpath("//input[@placeholder='MM/YY']");

    private static final By FIELD_CARD_CVV =
        By.xpath("//input[@placeholder='123']");

    private static final By FIELD_CARD_HOLDER =
        By.xpath("//input[@placeholder='Name on card']");

    private static final By BTN_REVIEW_ORDER =
        By.xpath("//button[normalize-space(text())='Review Order']");

    private static final By BTN_BACK_TO_SHIPPING =
        By.xpath("//button[normalize-space(text())='Back']");

    // ── Step 3: Review & Place Order ─────────────────────────────────────────
    private static final By REVIEW_HEADING =
        By.xpath("//h2[normalize-space(text())='Review Your Order']");

    private static final By REVIEW_SHIPPING_NAME =
        By.xpath("//p[@class[contains(.,'font-medium') and contains(.,'text-gray-900')]]");

    private static final By REVIEW_PAYMENT_METHOD =
        By.xpath("//h3[contains(.,'Payment Method')]/following-sibling::div//p");

    private static final By REVIEW_ORDER_ITEMS_COUNT =
        By.xpath("//h3[contains(.,'Order Items')]");

    private static final By REVIEW_TOTAL =
        By.xpath("//span[normalize-space(text())='Total']/following-sibling::span[contains(@class,'text-indigo-600')]");

    private static final By BTN_PLACE_ORDER =
        By.xpath("//button[contains(.,'Place Order')]");

    private static final By BTN_BACK_TO_PAYMENT =
        By.xpath("//button[normalize-space(text())='Back']");

    // ── Toast / error messages ────────────────────────────────────────────────
    private static final By TOAST_ERROR =
        By.cssSelector("[class*='Toastify__toast--error'], [class*='toast-error']");

    private static final By TOAST_ANY =
        By.cssSelector("[class*='Toastify__toast'], [class*='toast']");

    // ── Order confirmation ────────────────────────────────────────────────────
    private static final By ORDER_CONFIRMATION_HEADING =
        By.xpath("//h1[contains(.,'Order Confirmed') or contains(.,'Order Placed')]");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/checkout");
        waitForStep(1);
    }

    // FIX: Replaced arrow case syntax (Java 14+) with traditional colon syntax
    public void waitForStep(int step) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        switch (step) {
            case 1:
                wait.until(d -> isDisplayed(FIELD_ADDRESS_LINE1) ||
                                isDisplayed(BTN_CONTINUE_TO_PAYMENT));
                break;
            case 2:
                wait.until(d -> isDisplayed(RADIO_COD));
                break;
            case 3:
                wait.until(d -> isDisplayed(REVIEW_HEADING) ||
                                isDisplayed(BTN_PLACE_ORDER));
                break;
            default:
                log.warn("Unknown checkout step: " + step);
                break;
        }
        log.info("Checkout step " + step + " loaded");
    }

    public boolean isOnCheckoutPage() {
        return driver.getCurrentUrl().contains("/checkout");
    }

    // =========================================================================
    // Step 1 — Shipping Address
    // =========================================================================

    /** Fill all shipping address fields. Pass empty string to skip a field. */
    public void fillShippingAddress(String fullName, String addressLine1,
                                    String addressLine2, String city,
                                    String state, String postalCode,
                                    String phone) {
        fillField(FIELD_FULL_NAME,     fullName);
        fillField(FIELD_ADDRESS_LINE1, addressLine1);
        fillField(FIELD_ADDRESS_LINE2, addressLine2);
        fillField(FIELD_CITY,          city);
        fillField(FIELD_STATE,         state);
        fillField(FIELD_POSTAL_CODE,   postalCode);
        fillField(FIELD_PHONE,         phone);
        log.info("Shipping address filled");
    }

    public void clickContinueToPayment() {
        WaitUtils.waitForClickable(driver, BTN_CONTINUE_TO_PAYMENT).click();
    }

    public boolean isAddressLine1Visible() { return isDisplayed(FIELD_ADDRESS_LINE1); }
    public boolean isCityVisible()          { return isDisplayed(FIELD_CITY); }
    public boolean isPostalCodeVisible()    { return isDisplayed(FIELD_POSTAL_CODE); }
    public boolean isPhoneVisible()         { return isDisplayed(FIELD_PHONE); }

    public String getFullNameValue()     { return getFieldValue(FIELD_FULL_NAME); }
    public String getAddressLine1Value() { return getFieldValue(FIELD_ADDRESS_LINE1); }
    public String getCityValue()         { return getFieldValue(FIELD_CITY); }
    public String getStateValue()        { return getFieldValue(FIELD_STATE); }
    public String getPostalCodeValue()   { return getFieldValue(FIELD_POSTAL_CODE); }
    public String getPhoneValue()        { return getFieldValue(FIELD_PHONE); }

    /** Max length enforced by browser for postalCode field */
    public int getPostalCodeMaxLength() {
        try {
            return Integer.parseInt(
                driver.findElement(FIELD_POSTAL_CODE).getAttribute("maxLength"));
        } catch (Exception e) { return -1; }
    }

    public int getPhoneMaxLength() {
        try {
            return Integer.parseInt(
                driver.findElement(FIELD_PHONE).getAttribute("maxLength"));
        } catch (Exception e) { return -1; }
    }

    // =========================================================================
    // Step 2 — Payment Method
    // =========================================================================

    // FIX: Replaced arrow case syntax (Java 14+) with traditional colon syntax
    public void selectPaymentMethod(String method) {
        switch (method.toUpperCase()) {
            case "COD":
                click(RADIO_COD);
                break;
            case "CARD":
                click(RADIO_CARD);
                break;
            case "UPI":
                click(RADIO_UPI);
                break;
            default:
                log.warn("Unknown payment method: " + method);
                break;
        }
        log.info("Payment method selected: " + method);
    }

    public boolean isCardFormVisible() { return isDisplayed(FIELD_CARD_NUMBER); }

    public void fillCardDetails(String number, String expiry, String cvv, String holderName) {
        fillField(FIELD_CARD_NUMBER, number);
        fillField(FIELD_CARD_EXPIRY, expiry);
        fillField(FIELD_CARD_CVV,    cvv);
        fillField(FIELD_CARD_HOLDER, holderName);
        log.info("Card details filled");
    }

    public void clickReviewOrder() {
        WaitUtils.waitForClickable(driver, BTN_REVIEW_ORDER).click();
    }

    public void clickBackToShipping() {
        click(BTN_BACK_TO_SHIPPING);
    }

    public boolean isCODSelected() {
        try { return driver.findElement(RADIO_COD).isSelected(); }
        catch (Exception e) { return false; }
    }

    public boolean isCardSelected() {
        try { return driver.findElement(RADIO_CARD).isSelected(); }
        catch (Exception e) { return false; }
    }

    public boolean isUPISelected() {
        try { return driver.findElement(RADIO_UPI).isSelected(); }
        catch (Exception e) { return false; }
    }

    // =========================================================================
    // Step 3 — Review & Place Order
    // =========================================================================

    public boolean isReviewStepVisible()    { return isDisplayed(REVIEW_HEADING); }
    public boolean isPlaceOrderBtnVisible() { return isDisplayed(BTN_PLACE_ORDER); }

    public String getReviewTotalText() {
        try { return driver.findElement(REVIEW_TOTAL).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getReviewPaymentText() {
        try { return driver.findElement(REVIEW_PAYMENT_METHOD).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public void clickPlaceOrder() {
        WaitUtils.waitForClickable(driver, BTN_PLACE_ORDER).click();
        log.info("Place Order clicked");
    }

    public void clickBackToPayment() {
        click(BTN_BACK_TO_PAYMENT);
    }

    // =========================================================================
    // Toast / Validation messages
    // =========================================================================

    public boolean isToastVisible() {
        return DynamicElementHandler.appearsWithin(getDriver(), TOAST_ANY, 5);
    }

    public boolean isErrorToastVisible() {
        return DynamicElementHandler.appearsWithin(getDriver(), TOAST_ERROR, 5);
    }

    public String getToastText() {
        try {
            WaitUtils.waitForVisible(driver, TOAST_ANY, 5);
            return driver.findElement(TOAST_ANY).getText().trim();
        } catch (Exception e) { return ""; }
    }

    // =========================================================================
    // Order Confirmation
    // =========================================================================

    public boolean isOrderConfirmed() {
        return DynamicElementHandler.appearsWithin(
            driver, ORDER_CONFIRMATION_HEADING, 15);
    }

    public boolean isOnOrderConfirmationPage() {
        return driver.getCurrentUrl().contains("/order-confirmation");
    }

    // =========================================================================
    // Progress step indicators
    // =========================================================================

    public boolean isShippingStepActive() {
        try {
            String cls = driver.findElement(STEP_SHIPPING_LABEL).getAttribute("class");
            return cls != null && cls.contains("indigo");
        } catch (Exception e) { return false; }
    }

    public boolean isPaymentStepActive() {
        try {
            String cls = driver.findElement(STEP_PAYMENT_LABEL).getAttribute("class");
            return cls != null && cls.contains("indigo");
        } catch (Exception e) { return false; }
    }

    public boolean isReviewStepActive() {
        try {
            String cls = driver.findElement(STEP_REVIEW_LABEL).getAttribute("class");
            return cls != null && cls.contains("indigo");
        } catch (Exception e) { return false; }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void fillField(By locator, String value) {
        if (value == null || value.isEmpty()) return;
        try {
            WebElement el = WaitUtils.waitForVisible(driver, locator);
            el.clear();
            el.sendKeys(value);
        } catch (Exception e) {
            log.warn("Could not fill field " + locator + ": " + e.getMessage());
        }
    }

    private String getFieldValue(By locator) {
        try { return driver.findElement(locator).getAttribute("value"); }
        catch (Exception e) { return ""; }
    }

    private WebDriver getDriver() { return driver; }

    private static class DynamicElementHandler {
        static boolean appearsWithin(WebDriver driver, By locator, int timeoutSeconds) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
                return true;
            } catch (Exception e) { return false; }
        }
    }
}