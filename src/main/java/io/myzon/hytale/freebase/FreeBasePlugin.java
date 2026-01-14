package io.myzon.hytale.freebase;

import io.myzon.hytale.application.services.ConfigurationService;
import io.myzon.hytale.application.services.ErrorHandlingService;
import io.myzon.hytale.application.services.I18nService;
import io.myzon.hytale.application.services.PluginLifecycleService;
import io.myzon.hytale.bootstrap.PluginCoreBootstrap;
import io.myzon.hytale.domain.model.PluginMetadata;
import io.myzon.hytale.domain.model.PluginState;
import io.myzon.hytale.domain.ports.PlatformPort;
import io.myzon.hytale.domain.ports.PluginLifecyclePort;
import io.myzon.hytale.domain.ports.StoragePort;
import io.myzon.hytale.freebase.service.WelcomeService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for the Free Base Plugin.
 * Fully decoupled from the Hytale engine and powered by the Myzon-Labs core.
 */
public class FreeBasePlugin implements PluginLifecyclePort {

    private final PluginMetadata metadata = new PluginMetadata(
            "free-base-plugin",
            "Hytale Free Base Plugin",
            "1.0.0",
            "Myzon Labs",
            "Open-source FREE base plugin template"
    );

    private final Path pluginDirectory = Paths.get("plugins", "FreeBasePlugin");
    private final PluginCoreBootstrap core = new PluginCoreBootstrap(
            metadata,
            pluginDirectory,
            this,
            "dev" // environment placeholder
    );

    private PluginState currentState = PluginState.UNINITIALIZED;

    // Core services
    private ConfigurationService configurationService;
    private I18nService i18nService;
    private PluginLifecycleService lifecycleService;
    private ErrorHandlingService errorHandlingService;
    private PlatformPort platformPort;
    private StoragePort storagePort;

    // Plugin services
    private WelcomeService welcomeService;

    /**
     * Bootstraps the plugin core and starts the lifecycle.
     */
    public CompletableFuture<Void> start() {
        return core.initialize()
                .thenCompose(v -> {
                    this.lifecycleService = core.getLifecycleService();
                    return lifecycleService.initialize();
                });
    }

    /**
     * CLI entry point for standalone testing.
     */
    public static void main(String[] args) {
        FreeBasePlugin plugin = new FreeBasePlugin();
        plugin.start().join();

        // Keep alive briefly to see logs
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        plugin.lifecycleService.disable().join();
    }

    // ================= PluginLifecyclePort =================

    @Override
    public CompletableFuture<Void> onEnable() {
        currentState = PluginState.INITIALIZING;
        captureCoreServices();
        createPluginServices();

        return welcomeService.showWelcomeMessage()
                .thenRun(() -> currentState = PluginState.ACTIVE)
                .thenCompose(v -> log("INFO", "Plugin enabled"));
    }

    @Override
    public CompletableFuture<Void> onDisable() {
        currentState = PluginState.DISABLING;
        CompletableFuture<Void> cleanup = welcomeService != null
                ? welcomeService.shutdown()
                : CompletableFuture.completedFuture(null);

        return cleanup
                .thenRun(() -> currentState = PluginState.DISABLED)
                .thenCompose(v -> log("INFO", "Plugin disabled"));
    }

    @Override
    public CompletableFuture<Void> onReload() {
        return configurationService.reload()
                .thenCompose(v -> i18nService.reload())
                .thenCompose(v -> welcomeService.reload())
                .thenCompose(v -> log("INFO", "Plugin reloaded"));
    }

    @Override
    public PluginState getCurrentState() {
        return currentState;
    }

    // ================= Helpers =================

    private void captureCoreServices() {
        this.configurationService = core.getConfigurationService();
        this.i18nService = core.getI18nService();
        this.errorHandlingService = core.getErrorHandlingService();
        this.lifecycleService = core.getLifecycleService();
        this.platformPort = core.getPlatformPort();
        this.storagePort = core.getStoragePort();
    }

    private void createPluginServices() {
        this.welcomeService = new WelcomeService(
                configurationService,
                i18nService,
                storagePort,
                platformPort,
                errorHandlingService
        );
    }

    private CompletableFuture<Void> log(String level, String message) {
        return platformPort.log("FreeBasePlugin", level, message)
                .exceptionally(e -> {
                    System.out.println("[" + level + "] " + message);
                    return null;
                });
    }
}
