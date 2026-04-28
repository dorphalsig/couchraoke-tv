package com.couchraoke.quality

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

abstract class TestBranchTask : DefaultTask() {
    @get:Input
    abstract val srcSelectors: ListProperty<String>

    @get:Input
    abstract val testSelectors: ListProperty<String>

    @Option(option = "src", description = "Production class selector. Repeat --src for multiple values or use comma-separated list.")
    fun setSrcSelectorsOption(values: List<String>) {
        srcSelectors.set(normalizeSelectors(values))
    }

    @Option(option = "test", description = "Test class selector. Repeat --test for multiple values or use comma-separated list.")
    fun setTestSelectorsOption(values: List<String>) {
        testSelectors.set(normalizeSelectors(values))
    }

    private fun normalizeSelectors(values: List<String>): List<String> =
        values.flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
}
