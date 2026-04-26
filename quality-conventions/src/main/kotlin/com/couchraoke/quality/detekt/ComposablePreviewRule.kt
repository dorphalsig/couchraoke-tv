package com.couchraoke.quality.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

class ComposablePreviewRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "Every @Composable function must also have a @Preview annotation.",
        Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val isComposable = function.annotationEntries.any {
            it.shortName?.asString() == "Composable"
        }

        if (isComposable) {
            val hasPreview = function.annotationEntries.any {
                val name = it.shortName?.asString()
                name != null && name.contains("Preview")
            }

            if (!hasPreview) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(function),
                        "Composable function ${function.name} is missing a @Preview annotation."
                    )
                )
            }
        }
    }
}
