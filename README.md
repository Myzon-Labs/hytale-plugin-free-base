# Hytale Plugin Free Base

Open-source, free, engine-agnostic base plugin for Hytale built on **Myzon-Labs hytale-plugin-core**.

## What you get
- Clean architecture (ports & adapters) and zero direct Hytale API calls
- Works today with `MockPlatformAdapter` (Hytale API not available yet)
- Async + cache-first example service with config, i18n, logging, and storage
- Versioned YAML config and bilingual i18n (en-US, es-ES)
- Educational, readable code you can extend

## Project structure
```
src/main/java/io/myzon/hytale/freebase/FreeBasePlugin.java   # Main plugin class
src/main/java/io/myzon/hytale/freebase/service/WelcomeService.java  # Example service (async + cache-first)
src/main/resources/config/config.yml                        # Versioned config
src/main/resources/plugin.json                              # Engine-agnostic metadata
src/main/resources/i18n/{en_US,es_ES}.yml                   # i18n (core path)
src/main/resources/lang/{en-US,es-ES}.yml                   # i18n (prompt-required path)
```

## Architecture (high level)
- **Domain**: core models and ports (from hytale-plugin-core)
- **Application**: services provided by the core (config, i18n, lifecycle, storage)
- **Adapters**: YAML config/i18n, file storage, mock platform (from the core)
- **Bootstrap**: `PluginCoreBootstrap` wires everything and exposes services
- **Plugin layer**: `FreeBasePlugin` + `WelcomeService` (your code)

Dependency direction is inward only (plugin -> services -> ports). No engine APIs are used.

## How the core is used here
1) `PluginCoreBootstrap` is created with plugin metadata and the plugin directory.
2) Core initializes adapters (config/i18n/storage/platform) and services (config/i18n/lifecycle/error handling).
3) Lifecycle service calls the plugin hooks (`onEnable`, `onReload`, `onDisable`).
4) `WelcomeService` consumes `ConfigurationService`, `I18nService`, `StoragePort`, and `PlatformPort`.

## Example service (WelcomeService)
- Reads config (`welcome.enabled`, `welcome.message`, `welcome.delay_seconds`, `welcome.show_tips`).
- Uses i18n keys (`welcome.title`, `welcome.message`, `welcome.tip`).
- Logs via `PlatformPort.log`.
- Async + cache-first storage: keeps an in-memory welcome counter, persists with `StoragePort.save/load`.

## How to extend
1) Create a service class and inject the services you need (config, i18n, storage, platform).
2) Instantiate it in `FreeBasePlugin.createPluginServices()`.
3) Add configuration defaults in `config/config.yml` and translations in both `i18n/*.yml` and `lang/*.yml`.
4) Keep everything async (use `CompletableFuture`) and avoid blocking.

## Best practices for Hytale plugins
- Stay API-agnostic: depend on `PlatformPort`, never on Hytale classes.
- Keep user-facing text in i18n files; avoid hardcoded strings.
- Prefer async and cache-first for I/O (use `StoragePort` and in-memory caches).
- Validate config and provide safe defaults.
- Log meaningfully via `PlatformPort.log`; report errors through `ErrorHandlingService`.
- Keep code small, readable, and documented.

## Build and run
```
# From repository root
mvn clean package

# Standalone test (uses MockPlatformAdapter)
java -jar target/hytale-plugin-free-base-1.0.0.jar
```

## Versioned configuration
Default config lives at `src/main/resources/config/config.yml` and is designed to be regenerated. Keys are dot-notation friendly (e.g., `welcome.enabled`).

## Internationalization
- Core path (used by `YamlI18nAdapter`): `src/main/resources/i18n/en_US.yml`, `es_ES.yml`
- Prompt path (for clarity): `src/main/resources/lang/en-US.yml`, `es-ES.yml`

## License
MIT. No premium or licensing logic is included.
