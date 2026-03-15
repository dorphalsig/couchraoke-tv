plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
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
    "android/**/*.*"
)

val filteredClassDirectories = files(
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        exclude(jacocoExcludes)
    },
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Ktor
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
