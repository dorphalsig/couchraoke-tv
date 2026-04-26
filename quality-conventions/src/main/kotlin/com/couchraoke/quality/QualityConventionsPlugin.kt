package com.couchraoke.quality

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import io.github.takahirom.roborazzi.RoborazziExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

class QualityConventionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<QualityConventionsExtension>("qualityConventions").apply {
            variantName.convention("debug")
            minimumCoverage.convention(0.80)
        }

        project.pluginManager.withPlugin("com.android.application") { configureAndroidTesting(project) }
        project.pluginManager.withPlugin("com.android.library") { configureAndroidTesting(project) }

        project.plugins.apply("io.gitlab.arturbosch.detekt")
        project.plugins.apply("jacoco")
        project.plugins.apply("io.github.takahirom.roborazzi")

        configureDetekt(project)
        configureJacoco(project, extension)
        configureTestBranch(project, extension)
        configureRoborazzi(project)
    }
    private fun configureRoborazzi(project: Project) {
        project.extensions.configure<RoborazziExtension>("roborazzi") {
            generateComposePreviewRobolectricTests {
                enable.set(true)
                packages.set(listOf("com.couchraoke"))
                robolectricConfig.put("sdk", "[33]")
            }
        }

        project.tasks.configureEach {
            if (name.endsWith("ComposePreviewRobolectricTests")) {
                outputs.upToDateWhen { false }
                outputs.doNotCacheIf("Roborazzi preview tests must regenerate from current preview sources.") { true }
            }
        }

        project.dependencies {
            add("testImplementation", "io.github.takahirom.roborazzi:roborazzi-compose:1.59.0")
            add("testImplementation", "io.github.takahirom.roborazzi:roborazzi:1.59.0")
            add("testImplementation", "io.github.takahirom.roborazzi:roborazzi-junit-rule:1.59.0")
            add("testImplementation", "io.github.takahirom.roborazzi:roborazzi-compose-preview-scanner-support:1.59.0")
            add("testImplementation", "io.github.sergio-sastre.ComposablePreviewScanner:android:0.8.2")
            add("testImplementation", "org.robolectric:robolectric:4.16.1")
        }
    }
    private fun configureAndroidTesting(project: Project) {
        val android = project.extensions.findByName("android")
        if (android is ApplicationExtension) {
            android.testOptions.unitTests.isReturnDefaultValues = true
            android.testOptions.unitTests.isIncludeAndroidResources = true
            android.testOptions.unitTests.all { test ->
                test.failFast = false
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                test.extensions.configure(JacocoTaskExtension::class.java) {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        } else if (android is LibraryExtension) {
            android.testOptions.unitTests.isReturnDefaultValues = true
            android.testOptions.unitTests.isIncludeAndroidResources = true
            android.testOptions.unitTests.all { test ->
                test.failFast = false
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                test.extensions.configure(JacocoTaskExtension::class.java) {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
        project.tasks.withType<Test>().configureEach {
            if (!name.startsWith("testBranch")) {
                dependsOn(project.tasks.named("detekt"))
            }
        }
    }

    private fun configureDetekt(project: Project) {
        project.extensions.configure<DetektExtension>("detekt") {
            toolVersion = "1.23.8"
            config.setFrom(project.rootProject.files("detekt.yml"))
            buildUponDefaultConfig = true
            source.setFrom(
                "src/main/kotlin",
                "src/main/java",
                "src/test/kotlin",
                "src/test/java"
            )
        }

        project.dependencies {
            add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
            add("detektPlugins", "com.couchraoke:quality-conventions:1.0.0")
        }

        project.tasks.withType<Detekt>().configureEach {
            reports {
                html.required.set(true)
                xml.required.set(true)
                txt.required.set(true)
            }
        }
    }

    private fun configureJacoco(project: Project, extension: QualityConventionsExtension) {
        project.extensions.configure<JacocoPluginExtension>("jacoco") {
            toolVersion = "0.8.12"
        }

        project.afterEvaluate {
            val variant = extension.variantName.get()
            val capitalizedVariant = variant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val testTaskName = "test${capitalizedVariant}UnitTest"

            val jacocoReportTaskName = "jacoco${testTaskName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}Report"
            val jacocoCoverageTaskName = "jacoco${testTaskName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}CoverageVerification"

            val reportTask = project.tasks.register<JacocoReport>(jacocoReportTaskName) {
                dependsOn(testTaskName)
                reports {
                    xml.required.set(true)
                    html.required.set(true)
                }

                val mainSrc = project.files("src/main/kotlin", "src/main/java").asFileTree
                sourceDirectories.setFrom(mainSrc)
                val classDirs = project.fileTree(project.layout.buildDirectory) {
                    include(
                        "tmp/kotlin-classes/$variant/**/*.class",
                        "intermediates/javac/$variant/**/*.class",
                        "intermediates/classes/$variant/**/*.class",
                        "intermediates/built_in_kotlinc/$variant/**/*.class"
                    )
                    exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
                }
                classDirectories.setFrom(classDirs)
                executionData.setFrom(project.fileTree(project.layout.buildDirectory) {
                    include("jacoco/$testTaskName.exec")
                })
            }

            val verifyTask = project.tasks.register<JacocoCoverageVerification>(jacocoCoverageTaskName) {
                dependsOn(reportTask)

                val mainSrc = project.files("src/main/kotlin", "src/main/java").asFileTree
                sourceDirectories.setFrom(mainSrc)
                val classDirs = project.fileTree(project.layout.buildDirectory) {
                    include(
                        "tmp/kotlin-classes/$variant/**/*.class",
                        "intermediates/javac/$variant/**/*.class",
                        "intermediates/classes/$variant/**/*.class",
                        "intermediates/built_in_kotlinc/$variant/**/*.class"
                    )
                    exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
                }
                classDirectories.setFrom(classDirs)
                executionData.setFrom(project.fileTree(project.layout.buildDirectory) {
                    include("jacoco/$testTaskName.exec")
                })

                violationRules {
                    rule {
                        limit {
                            minimum = extension.minimumCoverage.get().toBigDecimal()
                        }
                    }
                }
            }

            val roborazziTaskName = "verifyRoborazzi$capitalizedVariant"
            project.tasks.configureEach {
                if (name == roborazziTaskName) {
                    dependsOn(verifyTask)
                }
            }

            project.tasks.named("check") {
                dependsOn(verifyTask)
                if (project.tasks.findByName(roborazziTaskName) != null) {
                    dependsOn(roborazziTaskName)
                }
            }
        }
    }

    private fun configureTestBranch(project: Project, extension: QualityConventionsExtension) {
        val testBranch = project.tasks.register<TestBranchTask>("testBranch") {
            group = "verification"
            description = "Runs selective tests, detekt, and jacoco on provided FQCNs."
        }

        val srcProvider = testBranch.flatMap { it.srcSelectors }
        val testProvider = testBranch.flatMap { it.testSelectors }

        val testBranchSelectedTests = project.tasks.register<Test>("testBranchSelectedTests") {
            group = "verification"
            useJUnit()
            systemProperty("robolectric.pixelCopyRenderMode", "hardware")
        }

        val testBranchRoborazzi = project.tasks.register<Test>("testBranchRoborazzi") {
            group = "verification"
            description = "Runs selective screenshots with Roborazzi."
            useJUnit()
            systemProperty("roborazzi.test.record", "true")
            systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            outputs.upToDateWhen { false }
            outputs.doNotCacheIf("Roborazzi screenshots must be regenerated for fresh validation.") { true }
        }

        val testBranchDetekt = project.tasks.register<Detekt>("testBranchDetekt") {
            group = "verification"
            config.setFrom(project.rootProject.files("detekt.yml"))
            buildUponDefaultConfig = true

            val detektSources = srcProvider.zip(testProvider) { src, tst ->
                val allFqcns = src + tst
                allFqcns.map { it.replace('.', '/') + ".kt" }.mapNotNull {
                    val mainFile = project.file("src/main/kotlin/$it")
                    val testFile = project.file("src/test/kotlin/$it")
                    if (mainFile.exists()) mainFile else if (testFile.exists()) testFile else null
                }
            }

            setSource(detektSources)
        }

        project.afterEvaluate {
            val variant = extension.variantName.get()
            val capitalizedVariant = variant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val testTaskName = "test${capitalizedVariant}UnitTest"

            val realTestTask = project.tasks.findByName(testTaskName) as? Test
            if (realTestTask != null) {
                val generatedPreviewTest = project.layout.buildDirectory.file(
                    "generated/roborazzi/preview-screenshot/$variant/com/github/takahirom/roborazzi/RoborazziPreviewParameterizedTests.kt"
                )
                testBranchSelectedTests.configure {
                    classpath = realTestTask.classpath
                    testClassesDirs = realTestTask.testClassesDirs
                    doFirst {
                        val selectedTests = testProvider.get()
                        filter.includePatterns.clear()
                        if (selectedTests.isNotEmpty()) {
                            selectedTests.forEach { filter.includeTestsMatching(it) }
                        } else {
                            filter.includeTestsMatching("**/NonExistentTest")
                        }
                    }
                }

                testBranchRoborazzi.configure {
                    classpath = realTestTask.classpath
                    testClassesDirs = realTestTask.testClassesDirs
                    dependsOn("generate${capitalizedVariant}ComposePreviewRobolectricTests")
                    doFirst {
                        val selectedTests = testProvider.get()
                        val previewGenerated = generatedPreviewTest.get().asFile.exists()
                        filter.includePatterns.clear()
                        if (selectedTests.isNotEmpty()) {
                            selectedTests.map { selector ->
                                if (previewGenerated && selector.isPreviewScreenshotSelector()) {
                                    "com.github.takahirom.roborazzi.RoborazziPreviewParameterizedTests"
                                } else {
                                    selector
                                }
                            }.distinct().forEach { filter.includeTestsMatching(it) }
                        } else {
                            filter.includeTestsMatching("**/NonExistentTest")
                        }
                    }
                }
            }

            val testBranchJacocoReport = project.tasks.register<JacocoReport>("testBranchJacocoReport") {
                dependsOn(testBranchSelectedTests)
                reports {
                    xml.required.set(true)
                    html.required.set(true)
                }

                val classDirsProvider = srcProvider.map { fqcns ->
                    project.fileTree(project.layout.buildDirectory) {
                        if (fqcns.isNotEmpty()) {
                            val patterns = fqcns.flatMap {
                                val base = it.replace('.', '/')
                                listOf(
                                    "tmp/kotlin-classes/$variant/**/$base.class", "tmp/kotlin-classes/$variant/**/$base$*.class",
                                    "intermediates/javac/$variant/**/$base.class", "intermediates/javac/$variant/**/$base$*.class",
                                    "intermediates/classes/$variant/**/$base.class", "intermediates/classes/$variant/**/$base$*.class",
                                    "intermediates/built_in_kotlinc/$variant/**/$base.class", "intermediates/built_in_kotlinc/$variant/**/$base$*.class"
                                )
                            }
                            include(patterns)
                        } else {
                            include("**/NonExistentClass.class")
                        }
                    }
                }
                classDirectories.setFrom(classDirsProvider)

                val srcDirsProvider = srcProvider.map { fqcns ->
                    val files = fqcns.mapNotNull {
                        val file = project.file("src/main/kotlin/${it.replace('.', '/')}.kt")
                        if (file.exists()) file else null
                    }
                    project.files(files)
                }
                sourceDirectories.setFrom(srcDirsProvider)

                executionData.setFrom(project.fileTree(project.layout.buildDirectory) {
                    include("outputs/unit_test_code_coverage/${variant}UnitTest/testBranchSelectedTests.exec")
                    include("jacoco/testBranchSelectedTests.exec")
                })
            }

            val testBranchJacocoCoverageVerification = project.tasks.register<JacocoCoverageVerification>("testBranchJacocoCoverageVerification") {
                dependsOn(testBranchJacocoReport)

                val classDirsProvider = srcProvider.map { fqcns ->
                    project.fileTree(project.layout.buildDirectory) {
                        if (fqcns.isNotEmpty()) {
                            val patterns = fqcns.flatMap {
                                val base = it.replace('.', '/')
                                listOf(
                                    "tmp/kotlin-classes/$variant/**/$base.class", "tmp/kotlin-classes/$variant/**/$base$*.class",
                                    "intermediates/javac/$variant/**/$base.class", "intermediates/javac/$variant/**/$base$*.class",
                                    "intermediates/classes/$variant/**/$base.class", "intermediates/classes/$variant/**/$base$*.class",
                                    "intermediates/built_in_kotlinc/$variant/**/$base.class", "intermediates/built_in_kotlinc/$variant/**/$base$*.class"
                                )
                            }
                            include(patterns)
                        } else {
                            include("**/NonExistentClass.class")
                        }
                    }
                }
                classDirectories.setFrom(classDirsProvider)

                val srcDirsProvider = srcProvider.map { fqcns ->
                    val files = fqcns.mapNotNull {
                        val file = project.file("src/main/kotlin/${it.replace('.', '/')}.kt")
                        if (file.exists()) file else null
                    }
                    project.files(files)
                }
                sourceDirectories.setFrom(srcDirsProvider)

                executionData.setFrom(project.fileTree(project.layout.buildDirectory) {
                    include("outputs/unit_test_code_coverage/${variant}UnitTest/testBranchSelectedTests.exec")
                    include("jacoco/testBranchSelectedTests.exec")
                })

                violationRules {
                    rule {
                        limit {
                            minimum = extension.minimumCoverage.get().toBigDecimal()
                        }
                    }
                }
            }

            testBranch.configure {
                dependsOn(testBranchSelectedTests)
                dependsOn(testBranchDetekt)
                dependsOn(testBranchJacocoCoverageVerification)
                dependsOn(testBranchRoborazzi)
            }
        }
    }
}

private fun String.isPreviewScreenshotSelector(): Boolean =
    endsWith("ScreenTest") || endsWith("ModalTest")

