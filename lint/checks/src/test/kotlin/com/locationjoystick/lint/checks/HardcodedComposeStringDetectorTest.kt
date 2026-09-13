package com.locationjoystick.lint.checks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

/**
 * The detector's own logic (method resolution, argument mapping, `@Preview`
 * exemption) is language-agnostic UAST over resolved `PsiMethod`s, so these
 * fixtures use Java stand-ins for the Compose/annotation types instead of
 * real Kotlin sources — this environment's lint test harness (`TestLintTask`)
 * cannot parse Kotlin fixtures reliably, while the detector itself is
 * separately verified end-to-end against real Kotlin Compose call sites via
 * `./gradlew :core:designsystem:lintRelease`.
 */
class HardcodedComposeStringDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = HardcodedComposeStringDetector()

    override fun getIssues(): List<Issue> = listOf(HardcodedComposeStringDetector.ISSUE)

    private val composableStub: TestFile =
        java(
            """
            package androidx.compose.runtime;
            public @interface Composable {}
            """,
        ).indented()

    private val previewStub: TestFile =
        java(
            """
            package androidx.compose.ui.tooling.preview;
            public @interface Preview {}
            """,
        ).indented()

    private val material3Stub: TestFile =
        java(
            """
            package androidx.compose.material3;
            public class Material3 {
                public static void Text(String text) {}
                public static String stringResource(int id) { return ""; }
            }
            """,
        ).indented()

    private val stubs = listOf(composableStub, previewStub, material3Stub)

    fun testHardcodedStringInTextIsFlagged() {
        lint()
            .files(
                *stubs.toTypedArray(),
                java(
                    """
                    package com.example;
                    import androidx.compose.material3.Material3;
                    import static androidx.compose.material3.Material3.Text;
                    import androidx.compose.runtime.Composable;

                    public class Sample {
                        @Composable
                        public void sample() {
                            Text("Hardcoded");
                        }
                    }
                    """,
                ).indented(),
            ).issues(HardcodedComposeStringDetector.ISSUE)
            .run()
            .expectWarningCount(1)
    }

    fun testStringResourceIsClean() {
        lint()
            .files(
                *stubs.toTypedArray(),
                java(
                    """
                    package com.example;
                    import static androidx.compose.material3.Material3.Text;
                    import static androidx.compose.material3.Material3.stringResource;
                    import androidx.compose.runtime.Composable;

                    public class Sample {
                        @Composable
                        public void sample() {
                            Text(stringResource(1));
                        }
                    }
                    """,
                ).indented(),
            ).issues(HardcodedComposeStringDetector.ISSUE)
            .run()
            .expectClean()
    }

    fun testHardcodedStringInsidePreviewComposableIsClean() {
        lint()
            .files(
                *stubs.toTypedArray(),
                java(
                    """
                    package com.example;
                    import static androidx.compose.material3.Material3.Text;
                    import androidx.compose.runtime.Composable;
                    import androidx.compose.ui.tooling.preview.Preview;

                    public class Sample {
                        @Preview
                        @Composable
                        public void samplePreview() {
                            Text("Hardcoded preview text");
                        }
                    }
                    """,
                ).indented(),
            ).issues(HardcodedComposeStringDetector.ISSUE)
            .run()
            .expectClean()
    }
}
