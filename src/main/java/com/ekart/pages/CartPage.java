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
 * Page Object: Cart Page (/cart)
 * Cart is localStorage-based.
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

    // Unit price element from frontend CartPage.jsx
    private static final By ITEM_PRICE =
        By.cssSelector("p.text-indigo-600.font-extrabold");

    private static final By ITEM_QUANTITY =
        By.cssSelector("span.px-4.py-2.font-bold");

    // Row total element from frontend CartPage.jsx
    private static final By ITEM_TOTAL =
        By.cssSelector("p.font-extrabold.text-gray-900");

    // ── Order summary ─────────────────────────────────────────────────────────
    private static final By ORDER_SUMMARY_HEADING =
        By.xpath("//h2[text()='Order Summary']");

    private static final By SUBTOTAL_VALUE =
        By.xpath("//span[normalize-space(text())='Subtotal']/following-sibling::span[contains(@class,'font-semibold')]");

    private static final By SHIPPING_VALUE =
        By.xpath("//span[normalize-space(text())='Shipping']/following-sibling::span");

    private static final By TAX_VALUE =
        By.xpath("//span[contains(normalize-space(text()),'Tax')]/following-sibling::span[contains(@class,'font-semibold')]");

    private static final By TOTAL_VALUE =
        By.xpath("//span[normalize-space(text())='Total']/following-sibling::span[contains(@class,'text-indigo-600')]");

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

    public List<String> getCartItemNames() {
        return getCartRows().stream()
            .map(row -> {
                try { return row.findElement(ITEM_NAME).getText().trim(); }
                catch (Exception e) { return ""; }
            })
            .filter(n -> !n.isEmpty())
            .collect(Collectors.toList());
    }

    public double getItemUnitPrice(int index) {
        try {
            List<WebElement> rows = getCartRows();
            return parsePrice(rows.get(index).findElement(ITEM_PRICE).getText());
        } catch (Exception e) { return -1; }
    }

    public int getItemQuantity(int index) {
        try {
            List<WebElement> rows = getCartRows();
            return Integer.parseInt(
                rows.get(index).findElement(ITEM_QUANTITY).getText().trim());
        } catch (Exception e) { return -1; }
    }

    public double getItemRowTotal(int index) {
        try {
            List<WebElement> rows = getCartRows();
            return parsePrice(rows.get(index).findElement(ITEM_TOTAL).getText());
        } catch (Exception e) { return -1; }
    }

    public boolean isProductInCart(String productName) {
        return getCartItemNames().stream()
            .anyMatch(n -> n.toLowerCase().contains(productName.toLowerCase()));
    }

    // =========================================================================
    // Quantity controls
    // =========================================================================

    public void increaseQuantity(int rowIndex) {
        WebElement row = getCartRows().get(rowIndex);
        List<WebElement> btns = row.findElements(By.cssSelector("button.p-2\\.5"));
        if (btns.size() >= 2) btns.get(1).click();
        else log.warn("Plus button not found at row " + rowIndex);
    }

    public void decreaseQuantity(int rowIndex) {
        WebElement row = getCartRows().get(rowIndex);
        List<WebElement> btns = row.findElements(By.cssSelector("button.p-2\\.5"));
        if (!btns.isEmpty()) btns.get(0).click();
        else log.warn("Minus button not found at row " + rowIndex);
    }

    // =========================================================================
    // Item actions
    // =========================================================================

    public void removeItem(int rowIndex) {
        List<WebElement> rows = getCartRows();
        rows.get(rowIndex)
            .findElement(By.xpath(".//button[@title='Remove item']"))
            .click();
        int before = rows.size();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> getCartRows().size() < before || isCartEmpty());
        } catch (TimeoutException ignored) {}
    }

    public void moveToWishlist(int rowIndex) {
        List<WebElement> rows = getCartRows();
        rows.get(rowIndex)
            .findElement(By.xpath(".//button[.//span[text()='Move to Wishlist']]"))
            .click();
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
            if (text.equalsIgnoreCase("FREE")) return 0.0;
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
    // Item count badge
    // =========================================================================

    public int getHeadingItemCount() {
        try {
            String text = driver.findElement(ITEM_COUNT_BADGE).getText();
            return Integer.parseInt(text.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return -1; }
    }

    // =========================================================================
    // localStorage helpers
    // =========================================================================

    public String getCartLocalStorage() {
        Object result = ((JavascriptExecutor) driver)
            .executeScript("return localStorage.getItem('cart');");
        return result == null ? "[]" : result.toString();
    }

    public int getLocalStorageCartCount() {
        String json = getCartLocalStorage();
        if (json.equals("[]") || json.equals("null")) return 0;
        int count = 0, idx = 0;
        while ((idx = json.indexOf("productId", idx)) != -1) { count++; idx++; }
        return count;
    }

    public void clearCartViaLocalStorage() {
        ((JavascriptExecutor) driver).executeScript(
            "localStorage.setItem('cart', '[]');" +
            "window.dispatchEvent(new Event('cartUpdated'));");
        log.info("Cart cleared via localStorage");
    }

    public boolean localStorageContainsProduct(String productId) {
        return getCartLocalStorage().contains(productId);
    }

    // =========================================================================
    // Private — parsePrice
    // =========================================================================

    /**
     * Parses Indian locale price strings to double.
     *
     * Examples:
     *   "₹5,525"    → 5525.0
     *   "₹1,31,049" → 131049.0
     *   "FREE"      → 0.0
     *   "₹55.25"    → 55.25  (genuine decimal — not a locale comma issue)
     *
     * Key rule: remove ALL commas before parsing so "5,525" → "5525" not "5.525"
     */
    private double parsePrice(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0.0;
        try {
            String s = raw.trim();
            if (s.equalsIgnoreCase("FREE")) return 0.0;
            // Remove ₹ symbol (Unicode \u20B9) and Rs
            s = s.replace("\u20B9", "").replace("\u20b9", "");
            s = s.replace("Rs.", "").replace("Rs", "");
            // Remove Indian locale commas BEFORE parsing
            // "5,525" → "5525"  |  "1,31,049" → "131049"
            s = s.replace(",", "").replaceAll("\\s+", "").trim();
            return s.isEmpty() ? 0.0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse price: '" + raw + "'");
            return 0.0;
        }
    }
}