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
- loads the custom Detekt rules from `quality-conventions/detekt-rules`
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
- `quality-conventions/detekt-rules/...`

Do **not** copy only the plugin source. The custom Detekt rules jar is built from `quality-conventions/detekt-rules` and is required by the plugin.

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
  - custom `test-timeouts` rules from the copied plugin folder

### Important current caveat

`testBranch` is **not yet fully FQCN-scoped for Detekt and JaCoCo**.

Current state:

- selected **tests** are truly filtered
- selective **Detekt** and **JaCoCo** task scaffolding exists
- but selective source/class narrowing for Detekt and JaCoCo is not yet fully implemented

So today, `testBranch` is partially selective:

- **tests:** yes
- **Detekt:** task exists, but not fully narrowed to the requested FQCNs yet
- **JaCoCo:** task exists, but not fully narrowed to the requested FQCNs yet

## Portability notes

This plugin is portable because it is an included build, not build-logic in the target repo.

However, portability currently depends on carrying the whole `quality-conventions/` folder so that the custom Detekt rules jar can be built and loaded from:

```text
quality-conventions/detekt-rules/build/libs/
```

The plugin expects exactly one jar in that directory when the consumer module is configured.

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
