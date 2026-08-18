package com.slotegrator.qa.report;

import com.slotegrator.qa.config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Writes the "Environment" panel of the Allure report, so a report tells you which host and which HTTP
 * transport produced it. Without this, two reports from different environments look identical.
 *
 * <p>Registered through {@code META-INF/services/org.testng.ITestNGListener}, so it applies to every suite
 * without a test class having to declare it.
 */
public class AllureEnvironment implements ISuiteListener {

    private static final Logger log = LoggerFactory.getLogger(AllureEnvironment.class);

    @Override
    public void onStart(ISuite suite) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Base URL", TestConfig.get("api.base.url").orElse("<unset>"));
        values.put("HTTP transport", TestConfig.transport().name().toLowerCase());
        values.put("Tester", TestConfig.get("auth.email").orElse("<unset>"));
        values.put("Players created per run", String.valueOf(TestConfig.playersToCreate()));
        values.put("Currency", TestConfig.playerCurrencyCode());
        values.put("Java", System.getProperty("java.version"));
        values.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));

        Properties properties = new Properties();
        properties.putAll(values);

        Path directory = Paths.get(
                System.getProperty("allure.results.directory", "target/allure-results"));
        try {
            Files.createDirectories(directory);
            try (var out = Files.newOutputStream(directory.resolve("environment.properties"))) {
                properties.store(out, "Environment under test");
            }
        } catch (IOException e) {
            // A missing environment panel must never fail a test run.
            log.warn("Could not write Allure environment.properties to {}: {}", directory, e.getMessage());
        }
    }
}
