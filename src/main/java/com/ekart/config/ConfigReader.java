package com.ekart.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ✅ STEP 4: Config Reader - reads config.properties
 */
public class ConfigReader {

    private static Properties properties;
    private static final String CONFIG_PATH =
        "src/test/resources/config.properties";

    static {
        try {
            FileInputStream fis = new FileInputStream(CONFIG_PATH);
            properties = new Properties();
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("❌ config.properties not found: " + e.getMessage());
        }
    }

    public static String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) throw new RuntimeException("❌ Key not found: " + key);
        return value.trim();
    }

    public static String getBaseUrl()           { return get("base.url"); }
    public static String getBrowser()           { return get("browser"); }
    public static int    getImplicitWait()      { return Integer.parseInt(get("implicit.wait")); }
    public static int    getExplicitWait()      { return Integer.parseInt(get("explicit.wait")); }
    public static boolean isHeadless()          { return Boolean.parseBoolean(get("headless")); }

    // Registration data
    public static String getRegEmail()          { return get("reg.email"); }
    public static String getRegPassword()       { return get("reg.password"); }
    public static String getRegPhone()          { return get("reg.phone"); }
    public static String getRegFirstName()      { return get("reg.firstName"); }
    public static String getRegLastName()       { return get("reg.lastName"); }

    // Login data
    public static String getValidEmail()        { return get("login.valid.email"); }
    public static String getValidPassword()     { return get("login.valid.password"); }
    public static String getInvalidEmail()      { return get("login.invalid.email"); }
    public static String getInvalidPassword()   { return get("login.invalid.password"); }

    // Admin credentials
    public static String getAdminEmail()        { return get("admin.email"); }
    public static String getAdminPassword()     { return get("admin.password"); }
}