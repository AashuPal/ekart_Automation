package com.ekart.tests;

import com.ekart.base.BaseTest;
import com.ekart.pages.CartPage;
import com.ekart.pages.CheckoutPage;
import com.ekart.pages.ProductDetailPage;
import com.ekart.pages.ProductListingPage;
import com.ekart.utils.WaitUtils;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Checkout Initiation & Form Input Validation Tests
 *
 * ── Checkout Initiation ───────────────────────────────────────────
 *  TC_CHK_001  Proceed to Checkout button on cart navigates to /checkout
 *  TC_CHK_002  Checkout page loads with Step 1 (Shipping) active
 *  TC_CHK_003  All required shipping form fields are visible
 *  TC_CHK_004  Continue to Payment advances to Step 2 with valid address
 *  TC_CHK_005  Step 2 shows all 3 payment options (COD, CARD, UPI)
 *  TC_CHK_006  Selecting CARD shows card detail form
 *  TC_CHK_007  Review Order advances to Step 3 — review page shown
 *  TC_CHK_008  Review step shows shipping address and payment method
 *
 * ── Form Input Validation ─────────────────────────────────────────
 *  TC_VAL_001  Missing required address fields blocks Step 1 → Step 2
 *  TC_VAL_002  Postal code enforces max 6 digits
 *  TC_VAL_003  Phone enforces max 10 digits
 *  TC_VAL_004  Back button on Step 2 returns to Step 1 with data intact
 *  TC_VAL_005  Back button on Step 3 returns to Step 2
 *  TC_VAL_006  COD payment selection is default; no card form shown
 *  TC_VAL_007  Card fields visible only when CARD is selected
 */
public class CheckoutTest extends BaseTest {

    private static final String KNOWN_PRODUCT_ID =
        "656a732e-9e66-496b-83c0-8991a7a987c0";

    // Valid test address data
    private static final String FULL_NAME    = "Test User";
    private static final String ADDRESS_LINE1 = "123 MG Road";
    private static final String ADDRESS_LINE2 = "Near City Mall";
    private static final String CITY          = "Bangalore";
    private static final String STATE         = "Karnataka";
    private static final String POSTAL_CODE   = "560001";
    private static final String PHONE         = "9876543210";

    private ProductListingPage listingPage;
    private ProductDetailPage  detailPage;
    private CartPage           cartPage;
    private CheckoutPage       checkoutPage;

    @BeforeClass(dependsOnMethods = "setUp")
    public void loginOnce() {
        getDriver().manage().timeouts()
            .pageLoadTimeout(java.time.Duration.ofSeconds(60));
        loginAsValidUser();
        log.info("Logged in once for CheckoutTest");
    }

    @BeforeMethod
    public void setup() {
        listingPage  = new ProductListingPage(getDriver());
        detailPage   = new ProductDetailPage(getDriver());
        cartPage     = new CartPage(getDriver());
        checkoutPage = new CheckoutPage(getDriver());

        // Recover session if expired
        try {
            if (getDriver().getCurrentUrl().contains("/login"))
                loginAsValidUser();
        } catch (Exception ignored) {}

        // Add product to cart for checkout tests
        try {
            getDriver().get(baseUrl);
            WaitUtils.waitForPageReady(getDriver());
            cartPage.clearCartViaLocalStorage();
            listingPage.navigateToProduct(baseUrl, KNOWN_PRODUCT_ID);
            detailPage.waitForLoad();
            detailPage.clickAddToCart();
        } catch (Exception e) {
            log.warn("Setup add-to-cart failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // CHECKOUT INITIATION
    // =========================================================================

    // ── TC_CHK_001 ────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "TC_CHK_001: Proceed to Checkout button navigates to /checkout")
    public void testProceedToCheckoutNavigation() {
        cartPage.navigateTo(baseUrl);
        Assert.assertFalse(cartPage.isCartEmpty(), "Cart must have items for checkout");

        cartPage.clickCheckout();
        WaitUtils.waitForPageReady(getDriver());

        String url = getDriver().getCurrentUrl();
        Assert.assertTrue(url.contains("/checkout"),
            "URL must contain /checkout after clicking Proceed. Got: " + url);

        log.info("PASS TC_CHK_001 | Navigated to: " + url);
    }

    // ── TC_CHK_002 ────────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "TC_CHK_002: Checkout page loads with Step 1 Shipping active")
    public void testCheckoutStep1Loads() {
        checkoutPage.navigateTo(baseUrl);

        Assert.assertTrue(checkoutPage.isOnCheckoutPage(),
            "Must be on /checkout page");
        Assert.assertTrue(checkoutPage.isAddressLine1Visible(),
            "Address Line 1 field must be visible on Step 1");
        Assert.assertTrue(checkoutPage.isShippingStepActive(),
            "Shipping step indicator must be active (indigo color)");

        log.info("PASS TC_CHK_002 | Step 1 Shipping loaded");
    }

    // ── TC_CHK_003 ────────────────────────────────────────────────────────────
    @Test(priority = 3,
          description = "TC_CHK_003: All required shipping form fields are visible")
    public void testShippingFormFieldsVisible() {
        checkoutPage.navigateTo(baseUrl);

        Assert.assertTrue(checkoutPage.isAddressLine1Visible(),
            "Address Line 1 field must be visible");
        Assert.assertTrue(checkoutPage.isCityVisible(),
            "City field must be visible");
        Assert.assertTrue(checkoutPage.isPostalCodeVisible(),
            "Postal Code field must be visible");
        Assert.assertTrue(checkoutPage.isPhoneVisible(),
            "Phone field must be visible");

        log.info("PASS TC_CHK_003 | All required shipping fields visible");
    }

    // ── TC_CHK_004 ────────────────────────────────────────────────────────────
    @Test(priority = 4,
          description = "TC_CHK_004: Valid address fills + Continue to Payment advances to Step 2")
    public void testContinueToPaymentWithValidAddress() {
        checkoutPage.navigateTo(baseUrl);

        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, ADDRESS_LINE2,
            CITY, STATE, POSTAL_CODE, PHONE);

        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        // Payment options must now be visible
        Assert.assertTrue(
            getDriver().getCurrentUrl().contains("/checkout"),
            "Must remain on checkout page");
        Assert.assertFalse(checkoutPage.isAddressLine1Visible(),
            "Step 1 form must be hidden on Step 2");

        log.info("PASS TC_CHK_004 | Moved to Step 2 with valid address");
    }

    // ── TC_CHK_005 ────────────────────────────────────────────────────────────
    @Test(priority = 5,
          description = "TC_CHK_005: Step 2 shows all 3 payment options COD, CARD, UPI")
    public void testPaymentOptionsVisible() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        // All 3 payment radio buttons must be present
        Assert.assertTrue(
            getDriver().findElements(
                org.openqa.selenium.By.xpath("//input[@type='radio' and @value='COD']"))
                .size() > 0,
            "COD radio button must be present");
        Assert.assertTrue(
            getDriver().findElements(
                org.openqa.selenium.By.xpath("//input[@type='radio' and @value='CARD']"))
                .size() > 0,
            "CARD radio button must be present");
        Assert.assertTrue(
            getDriver().findElements(
                org.openqa.selenium.By.xpath("//input[@type='radio' and @value='UPI']"))
                .size() > 0,
            "UPI radio button must be present");

        log.info("PASS TC_CHK_005 | All 3 payment options visible");
    }

    // ── TC_CHK_006 ────────────────────────────────────────────────────────────
    @Test(priority = 6,
          description = "TC_CHK_006: Selecting CARD payment reveals card detail form")
    public void testCardPaymentShowsCardForm() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        // Card form must NOT be visible before selecting CARD
        Assert.assertFalse(checkoutPage.isCardFormVisible(),
            "Card form must be hidden before selecting CARD");

        checkoutPage.selectPaymentMethod("CARD");

        Assert.assertTrue(checkoutPage.isCardFormVisible(),
            "Card form must appear after selecting CARD payment");

        log.info("PASS TC_CHK_006 | Card form appears on CARD selection");
    }

    // ── TC_CHK_007 ────────────────────────────────────────────────────────────
    @Test(priority = 7,
          description = "TC_CHK_007: Review Order advances to Step 3 review page")
    public void testReviewOrderAdvancesToStep3() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        checkoutPage.selectPaymentMethod("COD");
        checkoutPage.clickReviewOrder();
        checkoutPage.waitForStep(3);

        Assert.assertTrue(checkoutPage.isReviewStepVisible(),
            "'Review Your Order' heading must be visible on Step 3");
        Assert.assertTrue(checkoutPage.isPlaceOrderBtnVisible(),
            "'Place Order' button must be visible on Step 3");

        log.info("PASS TC_CHK_007 | Step 3 Review page loaded");
    }

    // ── TC_CHK_008 ────────────────────────────────────────────────────────────
    @Test(priority = 8,
          description = "TC_CHK_008: Review step shows shipping address and payment method")
    public void testReviewStepShowsDetails() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        checkoutPage.selectPaymentMethod("COD");
        checkoutPage.clickReviewOrder();
        checkoutPage.waitForStep(3);

        // Verify review page content
        String pageSource = getDriver().getPageSource();

        Assert.assertTrue(pageSource.contains(CITY) || pageSource.contains(ADDRESS_LINE1),
            "Review step must show shipping address details");
        Assert.assertTrue(
            pageSource.contains("Cash on Delivery") || pageSource.contains("COD"),
            "Review step must show selected payment method");

        String totalText = checkoutPage.getReviewTotalText();
        Assert.assertFalse(totalText.isEmpty(),
            "Review step must show order total");

        log.info("PASS TC_CHK_008 | Review shows address city=" + CITY
                 + " payment=COD total=" + totalText);
    }

    // =========================================================================
    // FORM INPUT VALIDATION
    // =========================================================================

    // ── TC_VAL_001 ────────────────────────────────────────────────────────────
    @Test(priority = 9,
          description = "TC_VAL_001: Missing required address fields blocks Step 1 to Step 2")
    public void testMissingRequiredFieldsBlocksStep1() {
        checkoutPage.navigateTo(baseUrl);

        // Do NOT fill addressLine1, city, postalCode — leave all empty
        checkoutPage.clickContinueToPayment();

        // Should stay on Step 1 — either toast error or still see address form
        boolean stayedOnStep1 = checkoutPage.isAddressLine1Visible();
        boolean toastShown    = checkoutPage.isToastVisible();

        Assert.assertTrue(stayedOnStep1 || toastShown,
            "Empty required fields must block Step 1 — address form still visible or error toast shown");

        log.info("PASS TC_VAL_001 | stayedOnStep1=" + stayedOnStep1 + " toastShown=" + toastShown);
    }

    // ── TC_VAL_002 ────────────────────────────────────────────────────────────
    @Test(priority = 10,
          description = "TC_VAL_002: Postal code field enforces maximum 6 digit length")
    public void testPostalCodeMaxLength() {
        checkoutPage.navigateTo(baseUrl);

        // Max length attribute check
        int maxLen = checkoutPage.getPostalCodeMaxLength();
        Assert.assertEquals(maxLen, 6,
            "Postal code maxLength attribute must be 6. Got=" + maxLen);

        // Type 10 digits — only 6 should be accepted
        org.openqa.selenium.WebElement postalField =
            getDriver().findElement(
                org.openqa.selenium.By.xpath("//input[@placeholder='6-digit PIN code']"));
        postalField.clear();
        postalField.sendKeys("1234567890");

        String value = postalField.getAttribute("value");
        Assert.assertTrue(value.length() <= 6,
            "Postal code must not exceed 6 chars. Got='" + value + "' length=" + value.length());

        log.info("PASS TC_VAL_002 | Postal code max 6 enforced | actual length=" + value.length());
    }

    // ── TC_VAL_003 ────────────────────────────────────────────────────────────
    @Test(priority = 11,
          description = "TC_VAL_003: Phone field enforces maximum 10 digit length")
    public void testPhoneMaxLength() {
        checkoutPage.navigateTo(baseUrl);

        int maxLen = checkoutPage.getPhoneMaxLength();
        Assert.assertEquals(maxLen, 10,
            "Phone maxLength attribute must be 10. Got=" + maxLen);

        // Type 15 digits — only 10 should be accepted
        org.openqa.selenium.WebElement phoneField =
            getDriver().findElement(
                org.openqa.selenium.By.xpath("//input[@placeholder='10-digit mobile number']"));
        phoneField.clear();
        phoneField.sendKeys("123456789012345");

        String value = phoneField.getAttribute("value");
        Assert.assertTrue(value.length() <= 10,
            "Phone must not exceed 10 chars. Got='" + value + "' length=" + value.length());

        log.info("PASS TC_VAL_003 | Phone max 10 enforced | actual length=" + value.length());
    }

    // ── TC_VAL_004 ────────────────────────────────────────────────────────────
    @Test(priority = 12,
          description = "TC_VAL_004: Back button on Step 2 returns to Step 1 with data intact")
    public void testBackFromStep2RetainsData() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        // Go back to Step 1
        checkoutPage.clickBackToShipping();
        checkoutPage.waitForStep(1);

        // Data must still be filled
        String city    = checkoutPage.getCityValue();
        String postal  = checkoutPage.getPostalCodeValue();
        String address = checkoutPage.getAddressLine1Value();

        Assert.assertTrue(
            city.contains(CITY) || address.contains(ADDRESS_LINE1) || postal.contains(POSTAL_CODE),
            "Shipping data must persist after Back from Step 2. city='" + city
            + "' address='" + address + "' postal='" + postal + "'");

        log.info("PASS TC_VAL_004 | Data persisted on back: city='" + city + "'");
    }

    // ── TC_VAL_005 ────────────────────────────────────────────────────────────
    @Test(priority = 13,
          description = "TC_VAL_005: Back button on Step 3 returns to Step 2 payment page")
    public void testBackFromStep3ReturnsToStep2() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        checkoutPage.selectPaymentMethod("COD");
        checkoutPage.clickReviewOrder();
        checkoutPage.waitForStep(3);

        Assert.assertTrue(checkoutPage.isReviewStepVisible(), "Must be on Step 3");

        checkoutPage.clickBackToPayment();
        checkoutPage.waitForStep(2);

        // Payment options must be visible again
        boolean paymentVisible = getDriver()
            .findElements(org.openqa.selenium.By.xpath("//input[@type='radio' and @value='COD']"))
            .size() > 0;

        Assert.assertTrue(paymentVisible,
            "Payment options must be visible after Back from Step 3");

        log.info("PASS TC_VAL_005 | Back from Step 3 shows Step 2 payment");
    }

    // ── TC_VAL_006 ────────────────────────────────────────────────────────────
    @Test(priority = 14,
          description = "TC_VAL_006: COD is default payment; no card form visible")
    public void testCODIsDefaultAndNoCardForm() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        // COD should be selected by default
        Assert.assertTrue(checkoutPage.isCODSelected(),
            "COD must be selected by default on Step 2");

        // Card form must NOT be visible
        Assert.assertFalse(checkoutPage.isCardFormVisible(),
            "Card form must NOT be visible when COD is selected");

        log.info("PASS TC_VAL_006 | COD default selected, no card form shown");
    }

    // ── TC_VAL_007 ────────────────────────────────────────────────────────────
    @Test(priority = 15,
          description = "TC_VAL_007: Card form visible only when CARD selected, hides on UPI/COD")
    public void testCardFormVisibilityToggle() {
        checkoutPage.navigateTo(baseUrl);
        checkoutPage.fillShippingAddress(
            FULL_NAME, ADDRESS_LINE1, "", CITY, STATE, POSTAL_CODE, PHONE);
        checkoutPage.clickContinueToPayment();
        checkoutPage.waitForStep(2);

        // Select CARD → card form appears
        checkoutPage.selectPaymentMethod("CARD");
        Assert.assertTrue(checkoutPage.isCardFormVisible(),
            "Card form must appear when CARD is selected");

        // Switch to UPI → card form disappears
        checkoutPage.selectPaymentMethod("UPI");
        Assert.assertFalse(checkoutPage.isCardFormVisible(),
            "Card form must hide when UPI is selected");

        // Switch back to COD → card form stays hidden
        checkoutPage.selectPaymentMethod("COD");
        Assert.assertFalse(checkoutPage.isCardFormVisible(),
            "Card form must hide when COD is selected");

        log.info("PASS TC_VAL_007 | Card form toggles correctly: CARD=visible, UPI/COD=hidden");
    }
}