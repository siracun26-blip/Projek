package com.example.videotranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videotranslator.appcheck.configureAppCheck
import com.example.videotranslator.model.TranslationUiState
import com.example.videotranslator.navigation.StudioRoute
import com.example.videotranslator.navigation.SubtitlesRoute
import com.example.videotranslator.ui.screens.StudioScreen
import com.example.videotranslator.ui.screens.SubtitlesScreen
import com.example.videotranslator.ui.theme.VideoTranslatorTheme
import com.example.videotranslator.viewmodel.TranslatorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            VideoTranslatorTheme {
                val navController = rememberNavController()
                val viewModel: TranslatorViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
                val dubbingEnabled by viewModel.dubbingEnabled.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = StudioRoute
                ) {
                    composable<StudioRoute> {
                        StudioScreen(
                            uiState = uiState,
                            selectedLanguage = selectedLanguage,
                            dubbingEnabled = dubbingEnabled,
                            onVideoPicked = viewModel::onVideoSelected,
                            onLanguageSelected = viewModel::selectLanguage,
                            onDubbingToggled = viewModel::setDubbingEnabled,
                            onStartTranslation = viewModel::startTranslation,
                            onSaveToGallery = viewModel::saveToGallery,
                            onViewSubtitles = {
                                navController.navigate(SubtitlesRoute)
                            },
                            onReset = viewModel::resetToReady
                        )
                    }

                    composable<SubtitlesRoute> {
                        val successState = uiState as? TranslationUiState.Success
                        SubtitlesScreen(
                            segments = successState?.segments.orEmpty(),
                            languageDisplayName = successState?.targetLanguage?.displayName
                                ?: selectedLanguage.displayName,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
