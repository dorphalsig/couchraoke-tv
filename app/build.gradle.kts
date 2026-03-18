plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    jacoco
}

android {
    namespace = "com.couchraoke.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.couchraoke.tv"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.extensions.configure(JacocoTaskExtension::class) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    baseline = file("detekt-baseline.xml")
}

tasks.named("check") {
    dependsOn("detekt")
}

android {
    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    // Tiny-file exemptions (≤30 lines, constitution §V)
    "**/domain/timing/SongPlaybackState*.*",
    "**/domain/timing/NoteWindowState*.*",
    "**/data/files/LocalFileResolver*.*",
    // Kotlinx-serialization generated companions + data class methods (generated code exemption)
    "**/*\$serializer*.*",
    "**/*Companion*.*",
    // Protocol message data classes — generated copy/equals/toString not testable
    "**/domain/network/protocol/HelloMessage*.*",
    "**/domain/network/protocol/ErrorMessage*.*",
    "**/domain/network/protocol/SessionStateMessage*.*",
    "**/domain/network/protocol/AssignSingerMessage*.*",
    "**/domain/network/protocol/Capabilities*.*",
    "**/domain/network/protocol/PingMessage*.*",
    "**/domain/network/protocol/PongMessage*.*",
    "**/domain/network/protocol/ClockAckMessage*.*",
    "**/domain/network/protocol/SlotInfo*.*",
    "**/domain/network/protocol/SlotMap*.*",
    // Untestable in unit context (requires real Ktor/JmDNS/Android)
    "**/data/network/WebSocketServer*.*",
    "**/data/network/MdnsAdvertiser*.*",
    // Category A — Compose composable file classes and all their nested lambdas
    "**/presentation/**/*Kt.*",
    "**/presentation/**/*Kt$*.*",
    // Category B — Hilt/Dagger generated classes
    "**/di/**/*.*",
    "hilt_aggregated_deps/**/*.*",
    "dagger/**/*.*",
    "**/*_HiltModules*.*",
    "**/*ProvideFactory*.*",
    // Category C — Android Application/Activity classes
    "**/MainActivity*.*",
    "**/CouchraokeApp*.*",
    // Category D — Tiny-file exemptions (≤30 lines, constitution §V)
    "**/presentation/songlist/PlayerAssignment*.*",
    "**/presentation/songlist/DuetPart*.*",
    "**/presentation/songlist/SelectPlayersMode*.*",
    "**/presentation/songlist/preview/SongPreviewController*.*",
    "**/presentation/songlist/preview/ISongPreviewController*.*",
    "**/presentation/songlist/preview/NoOpPreviewController*.*",
    // Category E — Kotlin inline lambda classes
    "**/*\$inlined\$*.*",
    // Compose-generated classes — not testable, must not lower coverage thresholds
    "**/*ComposableSingletons*.*",
    "**/*_Factory*.*",
    "**/*_MembersInjector*.*",
    "**/Hilt_*.*"
)

val filteredClassDirectories = files(
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        exclude(jacocoExcludes)
    },
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(jacocoExcludes)
    },
    fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
        exclude(jacocoExcludes)
    }
)

val coverageExecutionData = files(
    layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"),
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
)

val jacocoTestReport by tasks.registering(JacocoReport::class) {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Generates JaCoCo XML and HTML coverage reports for debug JVM unit tests."

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(filteredClassDirectories)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(coverageExecutionData)
}

val jacocoTestCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    dependsOn("testDebugUnitTest")
    group = "verification"
    description = "Fails the build when JaCoCo coverage thresholds are not met for debug JVM unit tests."

    classDirectories.setFrom(filteredClassDirectories)
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(coverageExecutionData)

    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "**.R",
                "**.R$*",
                "**.BuildConfig",
                "**.Manifest*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

tasks.register("ciUnitTests") {
    group = "verification"
    description = "Runs debug JVM unit tests, generates JaCoCo reports, and verifies coverage thresholds."
    dependsOn(jacocoTestReport, jacocoTestCoverageVerification)
}

// Parser-focused JVM test tasks (T003)
tasks.register<Test>("parserUnitTest") {
    group = "verification"
    description = "Runs parser-focused unit tests excluding acceptance tests."
    useJUnit()

    // Reuse test classpath and compiled classes without running the full JVM test task first.
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs

    // Include parser unit tests, exclude acceptance tests.
    include("**/domain/parser/**/*Test.class")
    exclude("**/domain/parser/**/*AcceptanceTest.class")

    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}

tasks.register<Test>("parserAcceptanceTest") {
    group = "verification"
    description = "Runs parser-focused acceptance tests."
    useJUnit()

    // Reuse test classpath and compiled classes without running the full JVM test task first.
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs

    // Include only parser acceptance tests.
    include("**/domain/parser/**/*AcceptanceTest.class")

    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}

tasks.register("parserTest") {
    group = "verification"
    description = "Runs all parser-focused tests (unit + acceptance)."
    dependsOn("parserUnitTest", "parserAcceptanceTest")
}

tasks.register<Test>("timingUnitTest") {
    group = "verification"
    description = "Runs timing-focused unit tests excluding acceptance tests."
    useJUnit()

    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs

    include("**/domain/timing/**/*Test.class")
    exclude("**/domain/timing/**/*AcceptanceTest.class")

    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}

tasks.register<Test>("timingAcceptanceTest") {
    group = "verification"
    description = "Runs timing-focused acceptance tests."
    useJUnit()

    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs

    include("**/domain/timing/**/*AcceptanceTest.class")

    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}

tasks.register("timingTest") {
    group = "verification"
    description = "Runs all timing-focused tests (unit + acceptance)."
    dependsOn("timingUnitTest", "timingAcceptanceTest")
}

tasks.register<Test>("scoringUnitTest") {
    group = "verification"
    description = "Runs scoring-focused unit tests excluding acceptance tests."
    useJUnit()
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs
    include("**/domain/scoring/**/*Test.class")
    exclude("**/domain/scoring/**/*AcceptanceTest.class")
    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}
tasks.register<Test>("scoringAcceptanceTest") {
    group = "verification"
    description = "Runs scoring-focused acceptance tests."
    useJUnit()
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs
    include("**/domain/scoring/**/*AcceptanceTest.class")
    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}
tasks.register("scoringTest") {
    group = "verification"
    description = "Runs all scoring-focused tests (unit + acceptance)."
    dependsOn("scoringUnitTest", "scoringAcceptanceTest")
}
tasks.register<Test>("libraryUnitTest") {
    group = "verification"
    description = "Runs library-focused unit tests excluding acceptance tests."
    useJUnit()
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs
    include("**/domain/library/**/*Test.class")
    exclude("**/domain/library/**/*AcceptanceTest.class")
    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}
tasks.register<Test>("libraryAcceptanceTest") {
    group = "verification"
    description = "Runs library-focused acceptance tests."
    useJUnit()
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs
    include("**/domain/library/**/*AcceptanceTest.class")
    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}
tasks.register("libraryTest") {
    group = "verification"
    description = "Runs all library-focused tests (unit + acceptance)."
    dependsOn("libraryUnitTest", "libraryAcceptanceTest")
}
tasks.register<Test>("networkUnitTest") {
    group = "verification"
    description = "Runs network/session-focused unit tests excluding acceptance tests."
    useJUnit()
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs
    include("**/data/network/**/*Test.class", "**/domain/network/**/*Test.class", "**/domain/session/**/*Test.class")
    exclude("**/*AcceptanceTest.class")
    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}
tasks.register<Test>("networkAcceptanceTest") {
    group = "verification"
    description = "Runs network/session-focused acceptance tests."
    useJUnit()
    val testTask = tasks.named<Test>("testDebugUnitTest")
    classpath = testTask.get().classpath
    testClassesDirs = testTask.get().testClassesDirs
    include("**/data/network/**/*AcceptanceTest.class", "**/domain/network/**/*AcceptanceTest.class")
    dependsOn(testTask.get().taskDependencies.getDependencies(testTask.get()))
}
tasks.register("networkTest") {
    group = "verification"
    description = "Runs all network/session-focused tests (unit + acceptance)."
    dependsOn("networkUnitTest", "networkAcceptanceTest")
}



dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.tv.material)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Ktor
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // QR Code
    implementation(libs.zxing.android.embedded)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // mDNS
    implementation(libs.jmdns)

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
