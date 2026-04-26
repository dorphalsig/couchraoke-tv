package com.couchraoke.quality.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class ProjectRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "test-timeouts"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                TestTimeoutRule(config),
                ComposablePreviewRule(config)
            )
        )
    }
}
