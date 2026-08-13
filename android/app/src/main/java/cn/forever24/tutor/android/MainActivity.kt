package cn.forever24.tutor.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.forever24.tutor.android.ui.AppRoot
import cn.forever24.tutor.android.ui.theme.EnglishTutorAgentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishTutorAgentTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AppRoot(
                    uiState = uiState,
                    onGoalSelected = viewModel::selectGoal,
                    onDailyMinutesSelected = viewModel::selectDailyMinutes,
                    onCorrectionStyleSelected = viewModel::selectCorrectionStyle,
                    onReminderChanged = viewModel::setReminderEnabled,
                    onSaveRawTextChanged = viewModel::setSaveRawText,
                    onSaveRawAudioChanged = viewModel::setSaveRawAudio,
                    onSelfRatingSelected = viewModel::selectSelfRating,
                    onContinue = viewModel::continueWithSelectedGoal,
                )
            }
        }
    }
}
