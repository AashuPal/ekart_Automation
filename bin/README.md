# eKart Selenium Automation Framework

## Project Structure (Page Object Model)
```
ekart-automation/
├── pom.xml                          # STEP 1 & 2: Maven + Dependencies
└── src/
    ├── main/java/com/ekart/
    │   ├── pages/
    │   │   ├── BasePage.java        # STEP 4: POM Base
    │   │   ├── HomePage.java        # STEP 6: Navigation
    │   │   ├── LoginPage.java       # STEP 11,12,13
    │   │   └── RegisterPage.java    # STEP 9,10,13
    │   └── utils/
    │       ├── DriverManager.java   # STEP 3: ChromeDriver Config
    │       ├── WaitUtils.java       # STEP 7: Implicit/Explicit Waits
    │       ├── AlertUtils.java      # STEP 8: Alerts & Popups
    │       └── ConfigReader.java    # Config management
    └── test/
        ├── java/com/ekart/
        │   ├── base/BaseTest.java   # STEP 4: Test Base
        │   └── tests/
        │       ├── NavigationTest.java   # STEP 6,14
        │       ├── RegistrationTest.java # STEP 9,10,13,14
        │       ├── LoginTest.java        # STEP 11,12,13,14
        │       └── AlertTest.java        # STEP 8
        └── resources/
            ├── config.properties    # Base URL, credentials
            ├── testng.xml           # Test suite config
            └── log4j2.xml           # Logging config
```

## How to Run

### Prerequisites
- Java 11+
- Maven 3.6+
- Google Chrome browser

### Run all tests
```bash
mvn clean test
```

### Run specific test class
```bash
mvn test -Dtest=LoginTest
```

### Run with different browser
Edit `config.properties`:
```
browser=firefox
```

## Test Cases Covered

| Step | Description | Test Class |
|------|-------------|-----------|
| 6 | Navigation tests | NavigationTest |
| 8 | Alert handling | AlertTest |
| 9 | User registration flow | RegistrationTest |
| 10 | Input field validation | RegistrationTest |
| 11 | Valid login | LoginTest |
| 12 | Invalid login | LoginTest |
| 13 | Error message validation | LoginTest, RegistrationTest |
| 14 | Assertions | All test classes |

## Update Locators
If tests fail, update locators in page classes to match your actual HTML.
Check element IDs/classes using browser DevTools (F12).
