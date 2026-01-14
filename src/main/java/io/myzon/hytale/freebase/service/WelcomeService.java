package io.myzon.hytale.freebase.service;

import io.myzon.hytale.application.services.ConfigurationService;
import io.myzon.hytale.application.services.ErrorHandlingService;
import io.myzon.hytale.application.services.I18nService;
import io.myzon.hytale.domain.ports.PlatformPort;
import io.myzon.hytale.domain.ports.StoragePort;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example service that demonstrates:
 * - Config access (async, typed)
 * - i18n usage
 * - Logging via PlatformPort
 * - Async storage with cache-first reads
 */
public class WelcomeService {

    private static final String STORAGE_KEY = "welcome.count";

    private final ConfigurationService configurationService;
    private final I18nService i18nService;
    private final StoragePort storagePort;
    private final PlatformPort platformPort;
    private final ErrorHandlingService errorHandling;

    // In-memory cache (cache-first strategy)
    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    public WelcomeService(ConfigurationService configurationService,
                          I18nService i18nService,
                          StoragePort storagePort,
                          PlatformPort platformPort,
                          ErrorHandlingService errorHandling) {
        this.configurationService = configurationService;
        this.i18nService = i18nService;
        this.storagePort = storagePort;
        this.platformPort = platformPort;
        this.errorHandling = errorHandling;
    }

    /**
     * Shows the welcome message using config + i18n + async storage.
     */
    public CompletableFuture<Void> showWelcomeMessage() {
        return configurationService.getBoolean("welcome.enabled", true)
                .thenCompose(enabled -> {
                    if (!Boolean.TRUE.equals(enabled)) {
                        return log("INFO", "Welcome message disabled by configuration");
                    }
                    return configurationService.getInteger("welcome.delay_seconds", 0)
                            .thenCompose(this::applyDelay)
                            .thenCompose(v -> renderWelcome());
                });
    }

    /**
     * Clears caches when reloading.
     */
    public CompletableFuture<Void> reload() {
        cache.clear();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Cleanup hook for shutdown.
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    // ================= Internal logic =================

    private CompletableFuture<Void> renderWelcome() {
        CompletableFuture<String> titleF = i18nService.getMessage("welcome.title");
        CompletableFuture<String> defaultMessageF = i18nService.getMessage("welcome.message");
        CompletableFuture<String> tipF = i18nService.getMessage("welcome.tip");
        CompletableFuture<String> customMessageF = configurationService.getString("welcome.message", "");
        CompletableFuture<Boolean> showTipsF = configurationService.getBoolean("welcome.show_tips", true);
        CompletableFuture<Integer> countF = loadWelcomeCount();

        return CompletableFuture.allOf(titleF, defaultMessageF, tipF, customMessageF, showTipsF, countF)
                .thenCompose(v -> {
                    String title = titleF.join();
                    String message = customMessageF.join();
                    if (message == null || message.isBlank()) {
                        message = defaultMessageF.join();
                    }
                    boolean showTips = Boolean.TRUE.equals(showTipsF.join());
                    String tip = tipF.join();
                    int count = countF.join();

                    return logBanner(title, message, tip, showTips, count)
                            .thenCompose(x -> incrementWelcomeCount(count + 1));
                });
    }

    private CompletableFuture<Integer> loadWelcomeCount() {
        if (cache.containsKey(STORAGE_KEY)) {
            return CompletableFuture.completedFuture(cache.get(STORAGE_KEY));
        }

        return storagePort.load(STORAGE_KEY)
                .thenApply(obj -> {
                    int value = parseInt(obj);
                    cache.put(STORAGE_KEY, value);
                    return value;
                })
                .exceptionally(e -> {
                    log("WARN", "Failed to load welcome count, defaulting to 0");
                    cache.put(STORAGE_KEY, 0);
                    return 0;
                });
    }

    private CompletableFuture<Void> incrementWelcomeCount(int nextValue) {
        cache.put(STORAGE_KEY, nextValue);
        return storagePort.save(STORAGE_KEY, nextValue)
                .exceptionally(e -> {
                    errorHandling.logError("StorageError", "Failed to save welcome count", e);
                    return null;
                });
    }

    private CompletableFuture<Void> logBanner(String title, String message, String tip, boolean showTips, int count) {
        String line = "=".repeat(60);
        String countLine = "Welcome count: " + count;

        CompletableFuture<Void> sequence = log("INFO", line)
                .thenCompose(v -> log("INFO", title))
                .thenCompose(v -> log("INFO", line.replace('=', '-')))
                .thenCompose(v -> log("INFO", message))
                .thenCompose(v -> log("INFO", countLine));

        if (showTips && tip != null && !tip.isBlank()) {
            sequence = sequence.thenCompose(v -> log("INFO", "Tip: " + tip));
        }

        return sequence.thenCompose(v -> log("INFO", line));
    }

    private int parseInt(Object obj) {
        if (obj instanceof Number number) {
            return number.intValue();
        }
        if (obj instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private CompletableFuture<Void> applyDelay(int delaySeconds) {
        if (delaySeconds <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        long delayMillis = delaySeconds * 1000L;
        return platformPort.scheduleTask(() -> { }, delayMillis);
    }

    private CompletableFuture<Void> log(String level, String message) {
        return platformPort.log("WelcomeService", level, message)
                .exceptionally(e -> {
                    System.out.println("[" + level + "] " + message);
                    return null;
                });
    }
}
