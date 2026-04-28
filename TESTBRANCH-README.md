# TestBranch / Portable Quality Conventions

This repo contains a portable Gradle conventions plugin under `quality-conventions/`.

It is designed to be copied into another Android repo with minimal setup.

## What it provides

When you apply `com.couchraoke.quality-conventions` to an Android app or library module, it:

- applies JaCoCo
- applies Detekt
- applies RoBorazzi
- automatically configures preview-driven RoBorazzi generation for `com.couchraoke.*`
- loads Detekt formatting rules
- loads the custom Detekt rules packaged in the included `quality-conventions` build
- configures full-module Detekt for `src/main/kotlin` and `src/test/kotlin`
- adds `testBranch` and related selective quality tasks
- adds JaCoCo report and coverage verification tasks for debug unit tests
- wires `check` to depend on full `detekt` and full JaCoCo coverage verification

## What you must copy

Copy the whole `quality-conventions/` directory into the root of the target repo.

That includes:

- `quality-conventions/build.gradle.kts`
- `quality-conventions/settings.gradle.kts`
- `quality-conventions/src/...`

Do **not** copy only selected plugin files. The custom Detekt rules are packaged in the included build jar and are required by the plugin.

## How to include it in another repo

### 1. Include the build

In the target repo `settings.gradle.kts`:

```kotlin
includeBuild("quality-conventions")
```

Make sure the target repo repositories can resolve:

- `google()`
- `mavenCentral()`
- `gradlePluginPortal()` in plugin management

### 2. Add a root Detekt config

The plugin expects a root-level `detekt.yml`:

```text
detekt.yml
```

This file is read by the module Detekt tasks.

### 3. Apply the plugin in each Android module

For an app module:

```kotlin
plugins {
    id("com.android.application")
    id("com.couchraoke.quality-conventions")
}
```

For a library module:

```kotlin
plugins {
    id("com.android.library")
    id("com.couchraoke.quality-conventions")
}
```

## Optional configuration

You can customize the plugin with the `qualityConventions` extension.

### RoBorazzi preview generation

The conventions plugin configures the RoBorazzi plugin for automatic `@Preview`-driven Robolectric screenshot tests.

It scans for previews in the `com.couchraoke` package and uses Robolectric SDK 33. This behavior is wired into the `testBranch` flow.

Example:

```kotlin
qualityConventions {
    variantName.set("debug")
    minimumCoverage.set(0.85)
}
```

Supported properties:

- `variantName`
  - default: `"debug"`
- `minimumCoverage`
  - default: `0.80`
  - value is decimal, so `0.80` = 80%

## Default tasks that still exist

This plugin applies the normal Detekt Gradle plugin.

That means the standard Detekt tasks still exist, including things like:

- `detekt`
- `detektBaseline`

If your Detekt plugin version generates additional default tasks, those still exist too.

## Added tasks

The plugin adds these tasks per module:

- `testBranch`
- `testBranchSelectedTests`
- `testBranchRoborazzi`
- `testBranchDetekt`
- `testBranchJacocoReport`
- `testBranchJacocoCoverageVerification`
- `jacocoTestDebugUnitTestReport`
- `jacocoTestDebugUnitTestCoverageVerification`

## Full-flow behavior

### `:module:detekt`

Runs Detekt against:

- `src/main/kotlin`
- `src/test/kotlin`

and reads the root `detekt.yml`.

### `:module:testDebugUnitTest`

This remains the normal Android/Gradle debug unit test task.

It:

- runs debug unit tests
- does **not** run Detekt
- does **not** run `testBranch`
- does **not** create Detekt baselines by itself

### `:module:check`

This plugin wires `check` so it depends on:

- `detekt`
- `jacocoTestDebugUnitTestCoverageVerification`

## `testBranch` usage

Current CLI shape:

```bash
./gradlew :app:testBranch \
  --src=com.example.MyClass,com.example.OtherClass \
  --test=com.example.MyClassTest,com.example.OtherClassTest
```

Arguments:

- `--src`
  - comma-separated list of production FQCNs
- `--test`
  - comma-separated list of test FQCNs

## Current real behavior of `testBranch`

### What is working now

- selected test classes are filtered and run
- selective Detekt task exists and runs
- selective JaCoCo tasks exist and run
- full Detekt can load:
  - formatting rules
  - custom `test-timeouts` rules packaged in the included plugin build

### Important current caveat

`testBranch` is selective for tests, and now performs selector-based source/class narrowing for the most common Kotlin and Java cases used by the module.

Current state:

- selected **tests** are truly filtered
- selective **Detekt** and **JaCoCo** task scaffolding exists
- selective JaCoCo class narrowing includes normal compiled classes and Kotlin file-facade classes such as `FooKt.class`
- some edge cases may still require follow-up work before every possible selector shape is perfectly narrowed

So today, `testBranch` is selectively useful in practice:

- **tests:** yes
- **Detekt:** task exists, but may still be broader than the requested FQCNs in some cases
- **JaCoCo:** narrowed for common class and top-level Kotlin file selectors, with remaining edge cases possible

## Portability notes

This plugin is portable because it is an included build, not build-logic in the target repo.

Portability currently depends on carrying the whole `quality-conventions/` folder so that the included build can produce the plugin jar. That jar contains both the Gradle conventions plugin and the custom Detekt rules, and Gradle composite-build substitution resolves `com.couchraoke:quality-conventions:1.0.0` for `detektPlugins`.

## Typical setup checklist

When moving this to another repo, verify:

- `quality-conventions/` was copied to repo root
- `includeBuild("quality-conventions")` was added to `settings.gradle.kts`
- root `detekt.yml` exists
- module applies `com.couchraoke.quality-conventions`
- plugin management repositories include `google()`, `mavenCentral()`, `gradlePluginPortal()`

## Typical commands

Run full Detekt:

```bash
./gradlew :app:detekt
```

Create Detekt baseline:

```bash
./gradlew :app:detektBaseline
```

Run normal debug unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Run selective branch task:

```bash
./gradlew :app:testBranch \
  --src=com.example.MyClass \
  --test=com.example.MyClassTest
```

Run full module check:

```bash
./gradlew :app:check
```

## Honest status

The portable plugin is already usable for:

- full Detekt
- full JaCoCo verification wiring
- normal debug unit tests
- custom Detekt rule loading
- `testBranch` task orchestration (including selective screenshots)

The main remaining gap is finishing true FQCN-based narrowing for selective Detekt and selective JaCoCo.
