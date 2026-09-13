package com.locationjoystick.lint.checks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class LjIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(HardcodedComposeStringDetector.ISSUE)
    override val api: Int = CURRENT_API
    override val minApi: Int = 8
    override val vendor: Vendor =
        Vendor(
            vendorName = "locationjoystick",
            feedbackUrl = "https://github.com/shortcuts/locationjoystick/issues",
            contact = "https://github.com/shortcuts/locationjoystick",
        )
}
