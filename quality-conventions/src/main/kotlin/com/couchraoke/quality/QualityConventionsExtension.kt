package com.couchraoke.quality

import org.gradle.api.provider.Property

interface QualityConventionsExtension {
    val variantName: Property<String>

    /**
     * Minimum LINE covered ratio. LINE is always defined, so this is the floor that every
     * selected class must clear.
     */
    val minimumCoverage: Property<Double>

    /**
     * Minimum BRANCH covered ratio, for classes that have branches at all.
     *
     * Deliberately lower than [minimumCoverage]: branch coverage reads systematically below
     * line coverage on the same code, partly because Kotlin emits synthetic branches that no
     * test can reach. The lower number expresses the same rigor in a stricter metric rather
     * than a weaker standard.
     *
     * A class with no `if`/`when` has a 0/0 branch ratio, which JaCoCo evaluates as `NaN` and
     * skips, so this limit never penalises straight-line code. That skip is also why BRANCH
     * cannot be the only limit — see the KDoc on the rule in `QualityConventionsPlugin`.
     */
    val minimumBranchCoverage: Property<Double>
}
