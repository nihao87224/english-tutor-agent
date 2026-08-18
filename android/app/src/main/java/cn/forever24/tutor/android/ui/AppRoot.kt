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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.forever24.tutor.android.auth.AppLocale
import cn.forever24.tutor.android.auth.AuthMode
import cn.forever24.tutor.android.ui.theme.EnglishTutorAgentTheme

@Composable
fun AppRoot(
    uiState: HomeUiState,
    onAuthModeChanged: (AuthMode) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onAuthSubmitted: () -> Unit,
    onLocaleChanged: (AppLocale) -> Unit,
    onLogout: () -> Unit,
    onRefreshQuota: () -> Unit,
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
    val copy = copyFor(uiState.locale)
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
            LocaleRow(
                locale = uiState.locale,
                onLocaleChanged = onLocaleChanged,
                copy = copy,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (!uiState.isAuthenticated) {
                AuthScreen(
                    uiState = uiState,
                    copy = copy,
                    onAuthModeChanged = onAuthModeChanged,
                    onEmailChanged = onEmailChanged,
                    onPasswordChanged = onPasswordChanged,
                    onAuthSubmitted = onAuthSubmitted,
                )
                return@Column
            }
            AccountHeader(
                uiState = uiState,
                copy = copy,
                onLogout = onLogout,
            )
            Spacer(modifier = Modifier.height(16.dp))
            QuotaCard(
                uiState = uiState,
                copy = copy,
                onRefreshQuota = onRefreshQuota,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OnboardingContent(
                uiState = uiState,
                copy = copy,
                onGoalSelected = onGoalSelected,
                onDailyMinutesSelected = onDailyMinutesSelected,
                onCorrectionStyleSelected = onCorrectionStyleSelected,
                onReminderChanged = onReminderChanged,
                onSaveRawTextChanged = onSaveRawTextChanged,
                onSaveRawAudioChanged = onSaveRawAudioChanged,
                onSelfRatingSelected = onSelfRatingSelected,
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun LocaleRow(
    locale: AppLocale,
    onLocaleChanged: (AppLocale) -> Unit,
    copy: AppCopy,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        OutlinedButton(onClick = { onLocaleChanged(AppLocale.EN) }) {
            Text(text = if (locale == AppLocale.EN) "EN ✓" else "EN")
        }
        OutlinedButton(onClick = { onLocaleChanged(AppLocale.ZH_CN) }) {
            Text(text = if (locale == AppLocale.ZH_CN) "中文 ✓" else "中文")
        }
    }
    Text(
        text = copy.appName,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AuthScreen(
    uiState: HomeUiState,
    copy: AppCopy,
    onAuthModeChanged: (AuthMode) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onAuthSubmitted: () -> Unit,
) {
    Text(
        text = copy.authTitle,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = copy.authSubtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onAuthModeChanged(AuthMode.LOGIN) }) {
            Text(text = if (uiState.authMode == AuthMode.LOGIN) "${copy.login} ✓" else copy.login)
        }
        OutlinedButton(onClick = { onAuthModeChanged(AuthMode.REGISTER) }) {
            Text(text = if (uiState.authMode == AuthMode.REGISTER) "${copy.register} ✓" else copy.register)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.emailInput,
        onValueChange = onEmailChanged,
        label = { Text(copy.email) },
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = uiState.passwordInput,
        onValueChange = onPasswordChanged,
        label = { Text(copy.password) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
    )
    uiState.authError?.let {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = it.localized(uiState.locale), color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = uiState.isAuthFormValid && uiState.authStatus != AuthStatus.SUBMITTING,
        onClick = onAuthSubmitted,
    ) {
        Text(
            text = if (uiState.authStatus == AuthStatus.SUBMITTING) {
                copy.submitting
            } else if (uiState.authMode == AuthMode.REGISTER) {
                copy.register
            } else {
                copy.login
            },
        )
    }
}

@Composable
private fun AccountHeader(
    uiState: HomeUiState,
    copy: AppCopy,
    onLogout: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = copy.accountTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = uiState.authenticatedEmail.orEmpty())
            Text(
                text = "${uiState.authenticatedStatus.orEmpty()} · ${uiState.authenticatedRoles.joinToString()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onLogout) {
                Text(text = copy.logout)
            }
        }
    }
}

@Composable
private fun QuotaCard(
    uiState: HomeUiState,
    copy: AppCopy,
    onRefreshQuota: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = copy.quotaTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            val quota = uiState.quota
            Text(
                text = when {
                    quota == null && uiState.quotaStatus == QuotaLoadStatus.LOADING -> copy.loadingQuota
                    quota == null -> copy.quotaUnavailable
                    quota.unlimited -> copy.unlimited
                    else -> "${quota.remaining} ${copy.remainingSuffix} · ${quota.used} / ${quota.dailyLimit + quota.bonus} ${copy.usedSuffix}"
                },
                color = if (uiState.quotaExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            uiState.quotaError?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it.localized(uiState.locale), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onRefreshQuota) {
                Text(text = copy.refreshQuota)
            }
        }
    }
}

@Composable
private fun OnboardingContent(
    uiState: HomeUiState,
    copy: AppCopy,
    onGoalSelected: (PrimaryGoal) -> Unit,
    onDailyMinutesSelected: (Int) -> Unit,
    onCorrectionStyleSelected: (CorrectionStyle) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onSaveRawTextChanged: (Boolean) -> Unit,
    onSaveRawAudioChanged: (Boolean) -> Unit,
    onSelfRatingSelected: (SelfAssessmentSkill, SelfRating) -> Unit,
    onContinue: () -> Unit,
) {
    Text(
        text = if (uiState.onboardingCompleted) copy.todayReadyTitle else copy.onboardingTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = copy.onboardingSubtitle,
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
            locale = uiState.locale,
            selected = uiState.selectedGoal == goal,
            onClick = { onGoalSelected(goal) },
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    PreferenceSection(
        uiState = uiState,
        copy = copy,
        onDailyMinutesSelected = onDailyMinutesSelected,
        onCorrectionStyleSelected = onCorrectionStyleSelected,
        onReminderChanged = onReminderChanged,
        onSaveRawTextChanged = onSaveRawTextChanged,
        onSaveRawAudioChanged = onSaveRawAudioChanged,
        onSelfRatingSelected = onSelfRatingSelected,
    )
    ResultAndPlanSection(uiState = uiState, copy = copy)
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = ((uiState.canContinue && uiState.canSubmitPreferences) || uiState.canStartFirstTraining) && !uiState.quotaExceeded,
        onClick = { onContinue() },
    ) {
        Text(text = if (uiState.canStartFirstTraining || uiState.onboardingCompleted) copy.startFirstTraining else copy.continueLabel)
    }
}

@Composable
private fun PreferenceSection(
    uiState: HomeUiState,
    copy: AppCopy,
    onDailyMinutesSelected: (Int) -> Unit,
    onCorrectionStyleSelected: (CorrectionStyle) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onSaveRawTextChanged: (Boolean) -> Unit,
    onSaveRawAudioChanged: (Boolean) -> Unit,
    onSelfRatingSelected: (SelfAssessmentSkill, SelfRating) -> Unit,
) {
    Text(
        text = copy.dailyPractice,
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
        text = copy.correctionStyle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    uiState.availableCorrectionStyles.forEach { style ->
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onCorrectionStyleSelected(style) },
        ) {
            val label = style.label.localized(uiState.locale)
            Text(text = if (uiState.correctionStyle == style) "$label selected" else label)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    PreferenceSwitch(copy.reminder, uiState.reminderEnabled, onReminderChanged)
    PreferenceSwitch(copy.saveRawText, uiState.saveRawText, onSaveRawTextChanged)
    PreferenceSwitch(copy.saveRawAudio, uiState.saveRawAudio, onSaveRawAudioChanged)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = copy.selfAssessment,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    SelfAssessmentSkill.entries.forEach { skill ->
        Text(
            text = skill.label.localized(uiState.locale),
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
private fun ResultAndPlanSection(uiState: HomeUiState, copy: AppCopy) {
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
                    text = "Initial profile".localized(uiState.locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${it.overallLevel} · ${it.confidencePercent}% ${copy.confidenceSuffix}",
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
                    text = "${copy.todayPlan} · ${it.totalMinutes} ${copy.minuteSuffix}",
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
    locale: AppLocale,
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
                    text = goal.label.localized(locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description.localized(locale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(text = if (selected) "Selected".localized(locale) else "Choose".localized(locale))
            }
        }
    }
}

private data class AppCopy(
    val appName: String,
    val authTitle: String,
    val authSubtitle: String,
    val login: String,
    val register: String,
    val submitting: String,
    val email: String,
    val password: String,
    val accountTitle: String,
    val logout: String,
    val quotaTitle: String,
    val loadingQuota: String,
    val quotaUnavailable: String,
    val unlimited: String,
    val remainingSuffix: String,
    val usedSuffix: String,
    val confidenceSuffix: String,
    val todayPlan: String,
    val minuteSuffix: String,
    val refreshQuota: String,
    val onboardingTitle: String,
    val onboardingSubtitle: String,
    val todayReadyTitle: String,
    val dailyPractice: String,
    val correctionStyle: String,
    val reminder: String,
    val saveRawText: String,
    val saveRawAudio: String,
    val selfAssessment: String,
    val continueLabel: String,
    val startFirstTraining: String,
)

private fun copyFor(locale: AppLocale): AppCopy =
    if (locale == AppLocale.ZH_CN) {
        AppCopy(
            appName = "英语表达教练",
            authTitle = "登录英语表达教练",
            authSubtitle = "学习记录、每日额度和练习历史会绑定到同一个邮箱账号。",
            login = "登录",
            register = "注册",
            submitting = "提交中...",
            email = "邮箱",
            password = "密码",
            accountTitle = "账号",
            logout = "退出登录",
            quotaTitle = "今日额度",
            loadingQuota = "正在加载额度...",
            quotaUnavailable = "额度暂不可用",
            unlimited = "无限额度",
            remainingSuffix = "次可用",
            usedSuffix = "已使用",
            confidenceSuffix = "置信度",
            todayPlan = "今日计划",
            minuteSuffix = "分钟",
            refreshQuota = "刷新额度",
            onboardingTitle = "设置今日练习",
            onboardingSubtitle = "选择目标、练习时长和纠错强度。设置会保存到你的账号。",
            todayReadyTitle = "今日练习已就绪",
            dailyPractice = "每日练习",
            correctionStyle = "纠错强度",
            reminder = "学习提醒",
            saveRawText = "保存文本",
            saveRawAudio = "保存音频",
            selfAssessment = "自评",
            continueLabel = "继续",
            startFirstTraining = "开始第一次训练",
        )
    } else {
        AppCopy(
            appName = "English Tutor",
            authTitle = "Sign in to English Tutor",
            authSubtitle = "Your learning history, daily quota and practice data are tied to your email account.",
            login = "Log in",
            register = "Sign up",
            submitting = "Submitting...",
            email = "Email",
            password = "Password",
            accountTitle = "Account",
            logout = "Log out",
            quotaTitle = "Today's quota",
            loadingQuota = "Loading quota...",
            quotaUnavailable = "Quota is unavailable",
            unlimited = "Unlimited",
            remainingSuffix = "left",
            usedSuffix = "used",
            confidenceSuffix = "confidence",
            todayPlan = "Today plan",
            minuteSuffix = "min",
            refreshQuota = "Refresh quota",
            onboardingTitle = "Set up today's practice",
            onboardingSubtitle = "Choose your goal, practice time and correction style. The settings are saved to your account.",
            todayReadyTitle = "Today's practice is ready",
            dailyPractice = "Daily practice",
            correctionStyle = "Correction style",
            reminder = "Learning reminder",
            saveRawText = "Save raw text",
            saveRawAudio = "Save raw audio",
            selfAssessment = "Self assessment",
            continueLabel = "Continue",
            startFirstTraining = "Start first training",
        )
    }

private fun String.localized(locale: AppLocale): String =
    if (locale != AppLocale.ZH_CN) {
        this
    } else {
        when (this) {
            "Enter a valid email and an 8+ character password." -> "请输入有效邮箱和至少 8 位密码。"
            "Authentication failed." -> "认证失败，请检查邮箱和密码。"
            "Session expired. Please sign in again." -> "登录已过期，请重新登录。"
            "Daily quota exceeded." -> "今日 AI 学习额度已用完。"
            "Quota is unavailable." -> "额度暂不可用。"
            "Save failed." -> "保存失败，请稍后重试。"
            "Workplace English" -> "职场英语"
            "Meetings, technical discussions and confident work communication." -> "会议、技术讨论和自信的工作沟通。"
            "General English" -> "通用英语"
            "Everyday listening, speaking and flexible real-life expression." -> "日常听说和灵活表达。"
            "IELTS Preparation" -> "雅思备考"
            "Speaking, writing and exam-oriented practice with clear feedback." -> "口语、写作和考试导向练习。"
            "Light" -> "轻量"
            "Standard" -> "标准"
            "Strict" -> "严格"
            "Listening" -> "听力"
            "Speaking" -> "口语"
            "Reading" -> "阅读"
            "Writing" -> "写作"
            "Initial profile" -> "初始画像"
            "Selected" -> "已选择"
            "Choose" -> "选择"
            else -> this
        }
    }

@Preview(showBackground = true)
@Composable
private fun AppRootPreview() {
    EnglishTutorAgentTheme {
        AppRoot(
            uiState = HomeUiState(),
            onAuthModeChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onAuthSubmitted = {},
            onLocaleChanged = {},
            onLogout = {},
            onRefreshQuota = {},
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
