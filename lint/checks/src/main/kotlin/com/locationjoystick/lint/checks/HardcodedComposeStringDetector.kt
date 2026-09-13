package com.locationjoystick.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.skipParenthesizedExprDown

/**
 * Flags a literal `String` passed to the text-bearing parameter of a small,
 * fixed set of Compose/Material calls. Deliberately narrow: it only flags a
 * plain string literal with no `$` interpolation (`ULiteralExpression`
 * whose `.value` is a `String`) — a string template with an interpolated
 * argument is not flagged, because the extraction sweep turns those into
 * `stringResource(R.string.x, arg)` calls, which are themselves calls, not
 * literals, and so are already clean.
 */
class HardcodedComposeStringDetector :
    Detector(),
    Detector.UastScanner {
        override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

        override fun createUastHandler(context: JavaContext): UElementHandler =
            object : UElementHandler() {
                override fun visitCallExpression(node: UCallExpression) {
                    val target = TARGETS[node.methodName] ?: return
                    val method = node.resolve() ?: return
                    val containingClass = method.containingClass?.qualifiedName ?: return
                    if (target.classPrefixes.none { containingClass.startsWith(it) }) return

                    val mapping = context.evaluator.computeArgumentMapping(node, method)
                    val argumentExpression =
                        mapping.entries.firstOrNull { it.value.name == target.paramName }?.key ?: return

                    val literal = argumentExpression.skipParenthesizedExprDown() as? ULiteralExpression ?: return
                    if (literal.value !is String) return

                    if (isInsidePreview(node)) return

                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(literal),
                        "Hardcoded string passed to ${node.methodName}() — use stringResource() instead",
                    )
                }
            }

        private fun isInsidePreview(node: UElement): Boolean {
            val method = node.getParentOfType(UMethod::class.java) ?: return false
            return method.uAnnotations.any { it.qualifiedName?.substringAfterLast('.') == "Preview" }
        }

        private data class Target(
            val paramName: String,
            val classPrefixes: List<String>,
        )

        companion object {
            private val TARGETS =
                mapOf(
                    "Text" to
                        Target("text", listOf("androidx.compose.material3.", "androidx.compose.material.")),
                    "Icon" to
                        Target(
                            "contentDescription",
                            listOf("androidx.compose.material3.", "androidx.compose.material."),
                        ),
                    "Image" to
                        Target("contentDescription", listOf("androidx.compose.foundation.")),
                    "showSnackbar" to
                        Target("message", listOf("androidx.compose.material3.", "androidx.compose.material.")),
                )

            val ISSUE: Issue =
                Issue.create(
                    id = "HardcodedComposeString",
                    briefDescription = "Hardcoded user-facing string in Compose UI",
                    explanation =
                        """
                        User-facing text must come from a string resource so the app can be \
                        localized later. Move this literal into the owning module's \
                        `res/values/strings.xml` and reference it via `stringResource(R.string.xxx)` \
                        (or `context.getString(...)` outside a composable).
                        """,
                    category = Category.I18N,
                    priority = 6,
                    severity = Severity.WARNING,
                    implementation = Implementation(HardcodedComposeStringDetector::class.java, Scope.JAVA_FILE_SCOPE),
                )
        }
    }
