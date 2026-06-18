package com.ekart.pages;

import com.ekart.utils.WaitUtils;
import com.ekart.utils.DynamicElementHandler;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object: Cart Page  (/cart)
 *
 * Cart is localStorage-based. Key localStorage structure:
 *  [{ productId, productName, unitPrice, quantity, imageUrl }]
 *
 * Covers:
 *  - Navigate to /cart
 *  - Cart item list (names, prices, quantities)
 *  - Quantity increment / decrement per item
 *  - Remove item
 *  - Move to wishlist
 *  - Order summary (subtotal, shipping, tax, total)
 *  - Empty cart state
 *  - Proceed to Checkout button
 *  - localStorage read/write helpers
 */
public class CartPage extends BasePage {

    protected static final Logger log = LogManager.getLogger(CartPage.class);

    // ── Page heading ──────────────────────────────────────────────────────────
    private static final By CART_HEADING =
        By.xpath("//h1[contains(.,'Shopping Cart')]");

    private static final By ITEM_COUNT_BADGE =
        By.xpath("//h1//span[contains(@class,'text-indigo-600')]");

    // ── Cart item rows ────────────────────────────────────────────────────────
    private static final By CART_ITEM_ROWS =
        By.cssSelector("div.bg-white.rounded-2xl.shadow-sm");

    private static final By ITEM_NAME =
        By.cssSelector("a.font-semibold.text-gray-900");

    private static final By ITEM_PRICE =
        By.cssSelector("p.text-indigo-600.font-extrabold");

    private static final By ITEM_QUANTITY =
        By.cssSelector("span.px-4.py-2.font-bold");

    private static final By ITEM_TOTAL =
        By.xpath(".//p[contains(@class,'font-extrabold') and contains(@class,'text-gray-900')]");

    // ── Quantity controls (relative to each row) ──────────────────────────────
    private static final By QTY_MINUS =
        By.xpath(".//button[.//*[name()='svg' and @class[contains(.,'FiMinus')]]] " +
                 "| .//button[contains(@class,'hover:bg-gray-100')][1]");

    private static final By QTY_PLUS =
        By.xpath(".//button[.//*[name()='svg' and @class[contains(.,'FiPlus')]]] " +
                 "| .//button[contains(@class,'hover:bg-gray-100')][2]");

    // ── Remove / move to wishlist ─────────────────────────────────────────────
    private static final By REMOVE_BUTTON =
        By.xpath(".//button[@title='Remove item']");

    private static final By MOVE_TO_WISHLIST =
        By.xpath(".//button[.//span[text()='Move to Wishlist']]");

    // ── Order summary ─────────────────────────────────────────────────────────
    private static final By ORDER_SUMMARY_HEADING =
        By.xpath("//h2[text()='Order Summary']");

    private static final By SUBTOTAL_VALUE =
        By.xpath("//span[text()='Subtotal']/following-sibling::span[contains(@class,'font-semibold')]");

    private static final By SHIPPING_VALUE =
        By.xpath("//span[text()='Shipping']/following-sibling::span");

    private static final By TAX_VALUE =
        By.xpath("//span[contains(text(),'Tax')]/following-sibling::span[contains(@class,'font-semibold')]");

    private static final By TOTAL_VALUE =
        By.xpath("//span[text()='Total']/following-sibling::span[contains(@class,'text-indigo-600')]");

    private static final By FREE_SHIPPING_MSG =
        By.xpath("//p[contains(@class,'text-green-600') and contains(.,'FREE')]");

    // ── Actions ───────────────────────────────────────────────────────────────
    private static final By PROCEED_TO_CHECKOUT =
        By.xpath("//button[.//span[text()='Proceed to Checkout']]");

    private static final By CONTINUE_SHOPPING =
        By.xpath("//a[.//span[text()='Continue Shopping']]");

    // ── Empty state ───────────────────────────────────────────────────────────
    private static final By EMPTY_CART_HEADING =
        By.xpath("//h2[text()='Your cart is empty']");

    private static final By EMPTY_CART_LINK =
        By.xpath("//a[.//span[text()='Continue Shopping']]");

    // ── Toast notification ────────────────────────────────────────────────────
    private static final By TOAST_MESSAGE =
        By.cssSelector("div[class*='toast'], [class*='Toastify'], [id*='toast']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    public void navigateTo(String baseUrl) {
        driver.get(baseUrl + "/cart");
        waitForCartLoad();
    }

    public void waitForCartLoad() {
        DynamicElementHandler.waitForLoadersToDisappear(driver);
        new WebDriverWait(driver, Duration.ofSeconds(15))
            .until(d ->
                !d.findElements(CART_HEADING).isEmpty() ||
                !d.findElements(EMPTY_CART_HEADING).isEmpty()
            );
    }

    public boolean isOnCartPage() {
        return driver.getCurrentUrl().contains("/cart");
    }

    // =========================================================================
    // Cart item queries
    // =========================================================================

    public List<WebElement> getCartRows() {
        return driver.findElements(CART_ITEM_ROWS)
                     .stream()
                     .filter(r -> !r.findElements(ITEM_NAME).isEmpty())
                     .collect(Collectors.toList());
    }

    public int getCartItemCount() {
        return getCartRows().size();
    }

    public boolean isCartEmpty() {
        return isDisplayed(EMPTY_CART_HEADING);
    }

    /** Returns names of all items in cart. */
    public List<String> getCartItemNames() {
        return getCartRows().stream()
            .map(row -> {
                try { return row.findElement(ITEM_NAME).getText().trim(); }
                catch (Exception e) { return ""; }
            })
            .filter(n -> !n.isEmpty())
            .collect(Collectors.toList());
    }

    /** Returns unit price of item at given index (0-based). */
    public double getItemUnitPrice(int index) {
        try {
            List<WebElement> rows = getCartRows();
            String raw = rows.get(index).findElement(ITEM_PRICE).getText();
            return parsePrice(raw);
        } catch (Exception e) { return -1; }
    }

    /** Returns quantity of item at given index. */
    public int getItemQuantity(int index) {
        try {
            List<WebElement> rows = getCartRows();
            return Integer.parseInt(
                rows.get(index).findElement(ITEM_QUANTITY).getText().trim());
        } catch (Exception e) { return -1; }
    }

    /** Returns row total (price × qty) of item at given index. */
    public double getItemRowTotal(int index) {
        try {
            List<WebElement> rows = getCartRows();
            String raw = rows.get(index).findElement(ITEM_TOTAL).getText();
            return parsePrice(raw);
        } catch (Exception e) { return -1; }
    }

    /** Returns true if a product with the given name is in the cart rows. */
    public boolean isProductInCart(String productName) {
        return getCartItemNames().stream()
            .anyMatch(n -> n.toLowerCase().contains(productName.toLowerCase()));
    }

    // =========================================================================
    // Quantity controls
    // =========================================================================

    public void increaseQuantity(int rowIndex) {
        WebElement row = getCartRows().get(rowIndex);
        List<WebElement> btns = row.findElements(
            By.cssSelector("button.p-2\\.5"));
        if (btns.size() >= 2) btns.get(1).click(); // Plus is second button
        else log.warn("Plus button not found at row " + rowIndex);
    }

    public void decreaseQuantity(int rowIndex) {
        WebElement row = getCartRows().get(rowIndex);
        List<WebElement> btns = row.findElements(
            By.cssSelector("button.p-2\\.5"));
        if (!btns.isEmpty()) btns.get(0).click(); // Minus is first button
        else log.warn("Minus button not found at row " + rowIndex);
    }

    // =========================================================================
    // Item actions
    // =========================================================================

    public void removeItem(int rowIndex) {
        List<WebElement> rows = getCartRows();
        WebElement removeBtn = rows.get(rowIndex)
            .findElement(By.xpath(".//button[@title='Remove item']"));
        removeBtn.click();
        // Wait for row count to decrease
        int before = rows.size();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> getCartRows().size() < before || isCartEmpty());
        } catch (TimeoutException ignored) {}
    }

    public void moveToWishlist(int rowIndex) {
        List<WebElement> rows = getCartRows();
        WebElement btn = rows.get(rowIndex)
            .findElement(By.xpath(".//button[.//span[text()='Move to Wishlist']]"));
        btn.click();
        int before = rows.size();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> getCartRows().size() < before || isCartEmpty());
        } catch (TimeoutException ignored) {}
    }

    // =========================================================================
    // Order summary
    // =========================================================================

    public boolean isOrderSummaryVisible() {
        return isDisplayed(ORDER_SUMMARY_HEADING);
    }

    public double getSubtotal() {
        try { return parsePrice(driver.findElement(SUBTOTAL_VALUE).getText()); }
        catch (Exception e) { return -1; }
    }

    public double getShipping() {
        try {
            String text = driver.findElement(SHIPPING_VALUE).getText().trim();
            if (text.equalsIgnoreCase("FREE") || text.equals("₹0") || text.equals("0")) return 0.0;
            return parsePrice(text);
        } catch (Exception e) { return -1; }
    }

    public double getTax() {
        try { return parsePrice(driver.findElement(TAX_VALUE).getText()); }
        catch (Exception e) { return -1; }
    }

    public double getTotal() {
        try { return parsePrice(driver.findElement(TOTAL_VALUE).getText()); }
        catch (Exception e) { return -1; }
    }

    public boolean isFreeShippingMessageVisible() {
        return isDisplayed(FREE_SHIPPING_MSG);
    }

    // =========================================================================
    // Actions
    // =========================================================================

    public void clickCheckout() {
        WaitUtils.waitForClickable(driver, PROCEED_TO_CHECKOUT).click();
    }

    public void clickContinueShopping() {
        WaitUtils.waitForClickable(driver, CONTINUE_SHOPPING).click();
        WaitUtils.waitForPageReady(driver);
    }

    // =========================================================================
    // Item count badge in heading
    // =========================================================================

    /** Returns the number shown in the cart heading badge: "Shopping Cart (3 items)" → 3 */
    public int getHeadingItemCount() {
        try {
            String text = driver.findElement(ITEM_COUNT_BADGE).getText();
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return -1; }
    }

    // =========================================================================
    // localStorage helpers (JS-based, no API call needed)
    // =========================================================================

    /** Reads cart array from localStorage and returns raw JSON string. */
    public String getCartLocalStorage() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object result = js.executeScript("return localStorage.getItem('cart');");
        return result == null ? "[]" : result.toString();
    }

    /** Returns number of items in localStorage cart. */
    public int getLocalStorageCartCount() {
        String json = getCartLocalStorage();
        if (json.equals("[]") || json.equals("null")) return 0;
        // Count occurrences of "productId" as proxy for item count
        int count = 0;
        int idx = 0;
        while ((idx = json.indexOf("productId", idx)) != -1) { count++; idx++; }
        return count;
    }

    /** Clears the cart in localStorage directly (faster than UI clicks). */
    public void clearCartViaLocalStorage() {
        ((JavascriptExecutor) driver)
            .executeScript("localStorage.setItem('cart', '[]'); " +
                           "window.dispatchEvent(new Event('cartUpdated'));");
        log.info("Cart cleared via localStorage");
    }

    /** Returns true if localStorage cart contains the given product ID. */
    public boolean localStorageContainsProduct(String productId) {
        return getCartLocalStorage().contains(productId);
    }

    // =========================================================================
    // Private
    // =========================================================================

    private double parsePrice(String raw) {
        try {
            // Remove currency symbol, spaces, and Indian-locale commas (e.g. ₹5,525 → 5525)
            String cleaned = raw.replaceAll("[₹Rs.\\s]", "").replace(",", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse price: '" + raw + "'");
            return 0;
        }
    }
}