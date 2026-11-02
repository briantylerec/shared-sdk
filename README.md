# Shared Android SDK

This is the second independent GitHub repository in the lab. It publishes a stable SDK facade backed by DummyJSON and contains a runnable sample app that demonstrates Clean Architecture and MVVM while consuming that facade.

## Architecture and modules

```text
:domain   pure Kotlin model, repository contract, use case
:data     Retrofit API/DTOs, mapper, repository implementation, network factory
:sdk      public RedBullSdk facade, keeps implementation details internal
:sample   runnable Android consumer demonstrating data/domain/presentation + MVVM
```

The production SDK has no screen to render, so an Android `ViewModel` does not belong in its runtime library. `:sample` supplies that presentation layer: `SampleViewModel` owns screen state and calls a use case; its Activity renders state and does not perform network or business logic. The sample's composition root wires dependencies without forcing Hilt/Dagger on SDK consumers. Production apps can still provide the SDK through their own DI setup (the Events and Explore apps use Hilt).

Dependency direction is inward: `domain` has no Android, Retrofit, or UI dependencies; `data` implements the domain contract; `sdk` exposes a small factory and delegates to the use case. The sample app consumes only the public SDK facade and has its own app-facing repository/use case so it demonstrates the same boundary pattern as the separate consumer apps.

Public API retained: `RedBullSdk.create(baseUrl)` and `getProducts(limit)` keep their behavior and defaults. The original Kotlin import `com.redbull.lab.domain.Product` remains available as a typealias; new code should use `com.redbull.lab.domain.model.Product`. The network default remains `https://dummyjson.com/`, requires HTTPS, and clamps the requested page size to 1–100.

## Build and run

Requires JDK 17 and Android SDK Platform 36. From this repo root:

```bash
./gradlew test assembleDebug
./gradlew :sample:installDebug
./gradlew publishToMavenLocal
```

The sample app launches and fetches products from DummyJSON. It needs network access, but no API key.

The published consumer coordinate remains `com.redbull.lab:android-sdk:<version>`. Publishing this repository also publishes `android-data` and the pure Kotlin `android-domain` at the same version. The API contract proposal is in `contracts/openapi.yaml`.

## Version choices and update rationale

The initial catalog was pinned to AGP 8.7.3, Kotlin 2.0.21, Retrofit 2.11.0, Moshi 1.15.1 and coroutines 1.9.0 when the scaffold was prepared. Those versions were selected as a conservative reproducible baseline, but the dependency catalog was not rechecked before delivery; that is why it had fallen behind current stable releases. Pinning versions is useful for reproducibility, but it does not replace periodic maintenance.

The current catalog uses AGP 8.13.2 + Gradle 8.13 + Kotlin 2.3.21 as a compatible conservative toolchain; Android's release notes explicitly call out Kotlin 2.3 support in AGP 8.13.2. Kotlin 2.4.21 is newer, but we kept the explicitly compatible Kotlin 2.3 / AGP 8.13 line for this lab rather than mixing an unverified compiler/Android Gradle Plugin upgrade. Retrofit is updated to stable 3.0.0, Moshi to stable 1.15.2 (not the 2.0 alpha), and coroutines to 1.11.0. The unused direct OkHttp declaration was removed; Retrofit 3.0.0 itself brings its supported OkHttp 4.12 dependency, and this SDK does not use OkHttp APIs directly.

The app repositories' Kotlin and AGP plugin pins and Gradle wrappers were also aligned to Kotlin 2.3.21 / AGP 8.13.2 / Gradle 8.13 so their compilers can consume the SDK's Kotlin metadata. UI dependency versions in those projects were not broadly upgraded as part of this SDK change.

Version references checked on 2026-11-01:

- [Android Gradle Plugin 8.13 release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Android Gradle Plugin and Gradle compatibility table](https://developer.android.com/build/releases/about-agp)
- [Kotlin release history](https://kotlinlang.org/docs/releases.html)
- [Retrofit releases](https://github.com/square/retrofit/releases)
- [Moshi repository and stable coordinates](https://github.com/square/moshi)
- [Kotlin Coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases)
- [OkHttp repository](https://github.com/square/okhttp)

## Publication

This repository's tag workflow checks tests and builds before publishing the `android-sdk`, `android-data`, and `android-domain` artifacts. Grant the Events and Explore repositories read access to its GitHub Package. Never store package tokens in the repository.
