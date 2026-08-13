package cn.forever24.tutor.android.ui

enum class CorrectionStyle(
    val label: String,
    val description: String,
) {
    LIGHT(
        label = "Light",
        description = "Keep conversation moving and save most notes for later.",
    ),
    STANDARD(
        label = "Standard",
        description = "Balance fluent practice with a few clear corrections.",
    ),
    STRICT(
        label = "Strict",
        description = "Use tighter feedback during focused practice.",
    ),
}
