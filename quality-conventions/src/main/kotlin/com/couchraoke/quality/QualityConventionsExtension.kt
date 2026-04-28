package com.couchraoke.quality

import org.gradle.api.provider.Property

interface QualityConventionsExtension {
    val variantName: Property<String>
    val minimumCoverage: Property<Double>
}
