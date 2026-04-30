package com.couchraoke.quality

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
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

class QualityConventionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<QualityConventionsExtension>("qualityConventions").apply {
            variantName.convention("debug")
            minimumCoverage.convention(0.80)
        }

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

        val pluginClasspath = project.files(
            java.io.File(QualityConventionsPlugin::class.java.protectionDomain.codeSource.location.toURI())
        )
        project.dependencies {
            add("compileOnly", pluginClasspath)
            add("testImplementation", pluginClasspath)
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

            val jacocoExcludes = resolveJacocoExcludes(project)
            val reportTask = project.tasks.register<JacocoReport>(jacocoReportTaskName) {
                dependsOn(testTaskName)
                reports {
                    xml.required.set(false)
                    html.required.set(false)
                    csv.required.set(true)
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
                    exclude(jacocoExcludes)
                }
                classDirectories.setFrom(classDirs)
                executionData.setFrom(project.fileTree(project.layout.buildDirectory) {
                    include("jacoco/$testTaskName.exec")
                })
                doLast {
                    val csvFile = (this as JacocoReport).reports.csv.outputLocation.get().asFile
                    if (csvFile.exists()) println("JaCoCo coverage report: ${csvFile.absolutePath}")
                }
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
                    exclude(jacocoExcludes)
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
                dependsOn("detekt")
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
            extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
            outputs.upToDateWhen { false }
            outputs.doNotCacheIf("testBranch selectors are command-scoped validation inputs.") { true }
        }

        val testBranchRoborazzi = project.tasks.register<Test>("testBranchRoborazzi") {
            group = "verification"
            description = "Runs selective screenshots with Roborazzi."
            useJUnit()
            systemProperty("roborazzi.test.record", "true")
            systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
            outputs.upToDateWhen { false }
            outputs.doNotCacheIf("Roborazzi screenshots must be regenerated for fresh validation.") { true }
            val roborazziOutputDir = project.layout.buildDirectory.dir("outputs/roborazzi")
            doLast {
                val roborazziDir = roborazziOutputDir.get().asFile
                if (roborazziDir.exists()) {
                    val screenshots = roborazziDir.walkTopDown()
                        .filter { it.extension == "png" }
                        .sortedBy { it.name }
                        .toList()
                    if (screenshots.isNotEmpty()) {
                        println("Roborazzi screenshots (${screenshots.size}):")
                        screenshots.forEach { println("  ${it.absolutePath}") }
                    }
                }
            }
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
            val testBranchExcludes = resolveJacocoExcludes(project)
            val composableScreenFqcns = findComposableScreenFqcns(project)
            val selectiveClassDirsProvider = srcProvider.map { selectors ->
                selectiveClassDirectories(project, variant, selectors, testBranchExcludes)
            }
            val selectiveSourceDirsProvider = srcProvider.map { selectors ->
                selectiveProductionSources(project, selectors)
            }

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
                        val selectedSrcs = srcProvider.get()
                        val previewGenerated = generatedPreviewTest.get().asFile.exists()
                        filter.includePatterns.clear()
                        val effectiveTests = selectedTests.map { selector ->
                            if (previewGenerated && selector.isPreviewScreenshotSelector()) {
                                "com.github.takahirom.roborazzi.RoborazziPreviewParameterizedTests"
                            } else {
                                selector
                            }
                        }.toMutableList()
                        if (previewGenerated && selectedSrcs.any { it in composableScreenFqcns }) {
                            effectiveTests += "com.github.takahirom.roborazzi.RoborazziPreviewParameterizedTests"
                        }
                        val finalTests = effectiveTests.distinct()
                        if (finalTests.isNotEmpty()) {
                            finalTests.forEach { filter.includeTestsMatching(it) }
                        } else {
                            filter.includeTestsMatching("**/NonExistentTest")
                        }
                    }
                }
            }

            val testBranchJacocoReport = project.tasks.register<JacocoReport>("testBranchJacocoReport") {
                dependsOn(testBranchSelectedTests)
                mustRunAfter(testBranchRoborazzi)
                outputs.upToDateWhen { false }
                outputs.doNotCacheIf("testBranch selectors are command-scoped validation inputs.") { true }
                reports {
                    xml.required.set(false)
                    html.required.set(false)
                    csv.required.set(true)
                }

                classDirectories.setFrom(selectiveClassDirsProvider)
                sourceDirectories.setFrom(selectiveSourceDirsProvider)

                executionData.setFrom(project.fileTree(project.layout.buildDirectory) {
                    include("outputs/unit_test_code_coverage/${variant}UnitTest/testBranchSelectedTests.exec")
                    include("jacoco/testBranchSelectedTests.exec")
                })
                doLast {
                    val csvFile = (this as JacocoReport).reports.csv.outputLocation.get().asFile
                    if (csvFile.exists()) println("JaCoCo coverage report: ${csvFile.absolutePath}")
                }
            }

            val testBranchJacocoCoverageVerification = project.tasks.register<JacocoCoverageVerification>("testBranchJacocoCoverageVerification") {
                dependsOn(testBranchJacocoReport)
                outputs.upToDateWhen { false }
                outputs.doNotCacheIf("testBranch selectors are command-scoped validation inputs.") { true }

                classDirectories.setFrom(selectiveClassDirsProvider)
                sourceDirectories.setFrom(selectiveSourceDirsProvider)

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

private fun selectiveProductionSources(project: Project, selectors: List<String>): FileCollection {
    val files = selectors.mapNotNull { selector ->
        selectiveProductionFile(project, selector)
    }
    return project.files(files)
}

private fun selectiveProductionFile(project: Project, selector: String) =
    listOf(
        project.file("src/main/kotlin/${selector.replace('.', '/')}.kt"),
        project.file("src/main/java/${selector.replace('.', '/')}.java")
    ).firstOrNull { it.exists() }

private fun selectiveClassDirectories(
    project: Project,
    variant: String,
    selectors: List<String>,
    excludes: List<String> = emptyList(),
) = project.fileTree(project.layout.buildDirectory) {
    if (selectors.isNotEmpty()) {
        val patterns = selectors.flatMap { selector ->
            selectiveClassPatterns(project, variant, selector)
        }
        include(patterns)
    } else {
        include("**/NonExistentClass.class")
    }
    if (excludes.isNotEmpty()) exclude(excludes)
}

private fun selectiveClassPatterns(project: Project, variant: String, selector: String): List<String> {
    val base = selector.replace('.', '/')
    val simpleName = selector.substringAfterLast('.')
    val sourceFile = selectiveProductionFile(project, selector)
    val compiledBase = if (
        sourceFile?.extension == "kt" && !sourceFile.declaresMatchingType(simpleName)
    ) {
        "${base}Kt"
    } else {
        base
    }

    return listOf(
        "tmp/kotlin-classes/$variant/**/$compiledBase.class",
        "tmp/kotlin-classes/$variant/**/$compiledBase$*.class",
        "intermediates/javac/$variant/**/$compiledBase.class",
        "intermediates/javac/$variant/**/$compiledBase$*.class",
        "intermediates/classes/$variant/**/$compiledBase.class",
        "intermediates/classes/$variant/**/$compiledBase$*.class",
        "intermediates/built_in_kotlinc/$variant/**/$compiledBase.class",
        "intermediates/built_in_kotlinc/$variant/**/$compiledBase$*.class"
    )
}

private fun java.io.File.declaresMatchingType(simpleName: String): Boolean {
    val declarationPattern = Regex(
        """(?m)^\s*(?:\w+\s+)*(?:class|interface|object|typealias)\s+${Regex.escape(simpleName)}\b|^\s*(?:\w+\s+)*fun\s+interface\s+${Regex.escape(simpleName)}\b"""
    )
    return declarationPattern.containsMatchIn(readText())
}

private val BUILTIN_JACOCO_EXCLUDES = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/ComposableSingletons$*.class",
)

private fun resolveJacocoExcludes(project: Project): List<String> {
    val userExcludes = (project.findProperty("jacoco.excludes") as? String)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
    return BUILTIN_JACOCO_EXCLUDES + userExcludes + findNoCoverageGeneratedExcludes(project)
}

private fun findComposableScreenFqcns(project: Project): Set<String> {
    val srcDir = project.file("src/main/kotlin")
    if (!srcDir.exists()) return emptySet()
    val result = mutableSetOf<String>()
    srcDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
        val content = file.readText()
        if (!content.contains("@NoCoverageGenerated")) return@forEach
        if (!content.contains("@Composable")) return@forEach
        val pkg = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
            .find(content)?.groupValues?.get(1) ?: return@forEach
        result += "$pkg.${file.nameWithoutExtension}"
    }
    return result
}

private fun findNoCoverageGeneratedExcludes(project: Project): List<String> {
    val srcDir = project.file("src/main/kotlin")
    if (!srcDir.exists()) return emptyList()
    val classDecl = Regex(
        """^\s*(?:(?:public|internal|private|protected|abstract|sealed|data|open|inner|enum|annotation)\s+)*(?:class|object|interface)\s+(\w+)"""
    )
    val funDecl = Regex("""^\s*(?:\w+\s+)*fun\s+""")
    val patterns = mutableListOf<String>()
    srcDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
        val lines = file.readLines()
        val name = file.nameWithoutExtension
        if (lines.any { it.trimStart().startsWith("@file:NoCoverageGenerated") }) {
            patterns += "**/${name}Kt.class"
            patterns += "**/${name}Kt$*.class"
            return@forEach
        }
        var lookingForDecl = false
        var fileKtExcluded = false
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("@NoCoverageGenerated") -> lookingForDecl = true
                lookingForDecl && trimmed.startsWith("@") -> Unit
                lookingForDecl -> {
                    val classMatch = classDecl.find(line)
                    if (classMatch != null) {
                        val className = classMatch.groupValues[1]
                        patterns += "**/$className.class"
                        patterns += "**/$className$*.class"
                    } else if (funDecl.containsMatchIn(line) && !fileKtExcluded) {
                        patterns += "**/${name}Kt.class"
                        patterns += "**/${name}Kt$*.class"
                        fileKtExcluded = true
                    }
                    lookingForDecl = false
                }
            }
        }
    }
    return patterns
}

