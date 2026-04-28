package com.couchraoke.quality.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

class TestTimeoutRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "Every @Test annotation must specify a timeout attribute.",
        Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val testAnnotation = function.annotationEntries.find {
            it.shortName?.asString() == "Test"
        }

        if (testAnnotation != null) {
            val hasTimeout = testAnnotation.valueArguments.any { arg ->
                val argName = arg.getArgumentName()?.asName?.asString()
                argName == "timeout" || argName == null // if no named args, we might just assume timeout if it has args, but typically timeout is named or only arg.
                // Wait, @Test(timeout = 1000L) is typical.
                // Let's just check if there are any arguments. If not, it fails.
            }

            // If the annotation has no arguments at all, or we specifically know it's missing timeout
            if (testAnnotation.valueArguments.isEmpty()) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(testAnnotation),
                        "Test function ${function.name} is missing a timeout attribute in its @Test annotation."
                    )
                )
            } else {
                // To be robust, if it has arguments we assume one of them is timeout.
                // It's usually `@Test(timeout = 30000L)` or `@Test(expected = Exception::class, timeout = ...)`
                val timeoutArg = testAnnotation.valueArguments.find {
                    it.getArgumentName()?.asName?.asString() == "timeout"
                }

                // If it's a single argument and unnamed, it might be expected=, but usually timeout is named.
                // For simplicity, we just check if `timeout` named arg exists or there are un-named args (which is not typical for Test).
                val isMissing = timeoutArg == null && testAnnotation.valueArguments.all { it.getArgumentName() != null }

                if (isMissing) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(testAnnotation),
                            "Test function ${function.name} must specify a timeout parameter (e.g. timeout = 30000L)."
                        )
                    )
                }
            }
        }
    }
}
