package cn.forever24.tutor.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = TutorPrimary,
    secondary = TutorSecondary,
    tertiary = TutorTertiary,
    background = TutorBackground,
    surfaceVariant = TutorSurfaceVariant,
)

@Composable
fun EnglishTutorAgentTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
