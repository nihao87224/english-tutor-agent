package cn.forever24.tutor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.forever24.tutor.android.ui.theme.EnglishTutorAgentTheme

@Composable
fun AppRoot(
    uiState: HomeUiState,
    onGoalSelected: (PrimaryGoal) -> Unit,
    onDailyMinutesSelected: (Int) -> Unit,
    onCorrectionStyleSelected: (CorrectionStyle) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onSaveRawTextChanged: (Boolean) -> Unit,
    onSaveRawAudioChanged: (Boolean) -> Unit,
    onSelfRatingSelected: (SelfAssessmentSkill, SelfRating) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.currentOnboardingStep.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            uiState.availableGoals.forEach { goal ->
                GoalOption(
                    goal = goal,
                    selected = uiState.selectedGoal == goal,
                    onClick = { onGoalSelected(goal) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            PreferenceSection(
                uiState = uiState,
                onDailyMinutesSelected = onDailyMinutesSelected,
                onCorrectionStyleSelected = onCorrectionStyleSelected,
                onReminderChanged = onReminderChanged,
                onSaveRawTextChanged = onSaveRawTextChanged,
                onSaveRawAudioChanged = onSaveRawAudioChanged,
                onSelfRatingSelected = onSelfRatingSelected,
            )
            ResultAndPlanSection(uiState = uiState)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = (uiState.canContinue && uiState.canSubmitPreferences) || uiState.canStartFirstTraining,
                onClick = { onContinue() },
            ) {
                Text(text = if (uiState.canStartFirstTraining) "Start first training" else "Continue")
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    uiState: HomeUiState,
    onDailyMinutesSelected: (Int) -> Unit,
    onCorrectionStyleSelected: (CorrectionStyle) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onSaveRawTextChanged: (Boolean) -> Unit,
    onSaveRawAudioChanged: (Boolean) -> Unit,
    onSelfRatingSelected: (SelfAssessmentSkill, SelfRating) -> Unit,
) {
    Text(
        text = "Daily practice",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.availableDailyMinutes.forEach { minutes ->
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { onDailyMinutesSelected(minutes) },
            ) {
                Text(text = minutes.toString())
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Correction style",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    uiState.availableCorrectionStyles.forEach { style ->
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onCorrectionStyleSelected(style) },
        ) {
            Text(text = if (uiState.correctionStyle == style) "${style.label} selected" else style.label)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    PreferenceSwitch(
        label = "Learning reminder",
        checked = uiState.reminderEnabled,
        onCheckedChange = onReminderChanged,
    )
    PreferenceSwitch(
        label = "Save raw text",
        checked = uiState.saveRawText,
        onCheckedChange = onSaveRawTextChanged,
    )
            PreferenceSwitch(
                label = "Save raw audio",
                checked = uiState.saveRawAudio,
                onCheckedChange = onSaveRawAudioChanged,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Self assessment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SelfAssessmentSkill.entries.forEach { skill ->
                Text(
                    text = skill.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SelfRating.entries.forEach { rating ->
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onSelfRatingSelected(skill, rating) },
                        ) {
                            Text(text = (rating.ordinal + 1).toString())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
}

@Composable
private fun ResultAndPlanSection(uiState: HomeUiState) {
    val result = uiState.assessmentResult
    val plan = uiState.todayPlan
    if (result == null && plan == null) {
        return
    }
    Spacer(modifier = Modifier.height(20.dp))
    result?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Initial profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${it.overallLevel} · ${it.confidencePercent}% confidence",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (it.priorities.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it.priorities.joinToString(" / "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
    plan?.let {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today plan · ${it.totalMinutes} min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it.reasons.joinToString(" "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(10.dp))
                it.tasks.forEach { task ->
                    Text(
                        text = "${task.durationMinutes} min · ${task.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${task.type} · ${task.difficulty} · ${task.skillFocus.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun GoalOption(
    goal: PrimaryGoal,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(text = if (selected) "Selected" else "Choose")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppRootPreview() {
    EnglishTutorAgentTheme {
        AppRoot(
            uiState = HomeUiState(),
            onGoalSelected = {},
            onDailyMinutesSelected = {},
            onCorrectionStyleSelected = {},
            onReminderChanged = {},
            onSaveRawTextChanged = {},
            onSaveRawAudioChanged = {},
            onSelfRatingSelected = { _, _ -> },
            onContinue = {},
        )
    }
}
