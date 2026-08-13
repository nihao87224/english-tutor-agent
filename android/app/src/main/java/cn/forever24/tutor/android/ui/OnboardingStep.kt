package cn.forever24.tutor.android.ui

enum class OnboardingStep(
    val label: String,
) {
    GOAL("Learning goal"),
    PREFERENCES("Preferences"),
    SELF_ASSESSMENT("Self assessment"),
    ASSESSMENT("Assessment"),
    RESULT("Result"),
    COMPLETE("Complete");

    companion object {
        fun fromBackendValue(value: String?): OnboardingStep =
            entries.firstOrNull { step -> step.name == value } ?: GOAL
    }
}
