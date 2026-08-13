package cn.forever24.tutor.android.ui

enum class PrimaryGoal(
    val label: String,
    val description: String,
) {
    WORKPLACE(
        label = "Workplace English",
        description = "Meetings, technical discussions and confident work communication.",
    ),
    GENERAL(
        label = "General English",
        description = "Everyday listening, speaking and flexible real-life expression.",
    ),
    IELTS(
        label = "IELTS Preparation",
        description = "Speaking, writing and exam-oriented practice with clear feedback.",
    ),
}
