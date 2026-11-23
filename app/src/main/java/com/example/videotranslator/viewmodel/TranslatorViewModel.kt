package com.example.videotranslator.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videotranslator.ai.GeminiTranslatorService
import com.example.videotranslator.media.VideoProcessor
import com.example.videotranslator.model.ProcessingStage
import com.example.videotranslator.model.TargetLanguage
import com.example.videotranslator.model.TranslationUiState
import com.example.videotranslator.model.VideoMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "TranslatorViewModel"

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val videoProcessor = VideoProcessor(application.applicationContext)
    private val translatorService = GeminiTranslatorService()

    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(TargetLanguage.INDONESIAN)
    val selectedLanguage: StateFlow<TargetLanguage> = _selectedLanguage.asStateFlow()

    private val _dubbingEnabled = MutableStateFlow(false)
    val dubbingEnabled: StateFlow<Boolean> = _dubbingEnabled.asStateFlow()

    private var currentVideo: VideoMetadata? = null

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val metadata = videoProcessor.readVideoMetadata(uri)
                currentVideo = metadata
                _uiState.value = TranslationUiState.VideoReady(metadata)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read selected video", e)
                _uiState.value = TranslationUiState.Error(
                    video = currentVideo,
                    message = e.localizedMessage ?: "Failed to read selected video."
                )
            }
        }
    }

    fun selectLanguage(language: TargetLanguage) {
        _selectedLanguage.value = language
    }

    fun setDubbingEnabled(enabled: Boolean) {
        _dubbingEnabled.value = enabled
    }

    fun startTranslation() {
        val video = currentVideo ?: run {
            _uiState.value = TranslationUiState.Error(null, "Please select a video first.")
            return
        }
        val targetLang = _selectedLanguage.value
        val enableDubbing = _dubbingEnabled.value

        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            try {
                _uiState.value = TranslationUiState.Processing(
                    video = video,
                    stage = ProcessingStage.PREPARING,
                    progress = 0.08f
                )
                val cachedVideo = videoProcessor.copyUriToCache(video.uri)

                _uiState.value = TranslationUiState.Processing(
                    video = video,
                    stage = ProcessingStage.EXTRACTING_AUDIO,
                    progress = 0.24f
                )
                val extractedAudio = videoProcessor.extractAudio(
                    videoFile = cachedVideo,
                    outputAudioFile = File(cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                )

                _uiState.value = TranslationUiState.Processing(
                    video = video,
                    stage = ProcessingStage.AI_TRANSLATING,
                    progress = 0.45f
                )
                val translationResult = translatorService.translateAudioToSubtitles(
                    audioFile = extractedAudio,
                    mimeType = "audio/mp4",
                    targetLanguage = targetLang
                )

                val srtFile = File(cacheDir, "subtitle_${System.currentTimeMillis()}.srt")
                srtFile.writeText(translationResult.srtContent)

                var dubbingFile: File? = null
                if (enableDubbing) {
                    _uiState.value = TranslationUiState.Processing(
                        video = video,
                        stage = ProcessingStage.SYNTHESIZING_DUB,
                        progress = 0.65f
                    )
                    val wavFile = File(cacheDir, "dubbing_${System.currentTimeMillis()}.wav")
                    val ttsOk = videoProcessor.generateDubbingAudio(
                        segments = translationResult.segments,
                        targetLanguage = targetLang,
                        outputWavFile = wavFile
                    )
                    if (ttsOk) {
                        dubbingFile = wavFile
                    } else {
                        Log.w(TAG, "TTS dubbing synthesis unavailable; continuing with subtitles only.")
                    }
                }

                _uiState.value = TranslationUiState.Processing(
                    video = video,
                    stage = ProcessingStage.RENDERING_VIDEO,
                    progress = 0.72f
                )
                val outputVideo = File(cacheDir, "translated_${System.currentTimeMillis()}.mp4")
                videoProcessor.renderFinalVideo(
                    videoFile = cachedVideo,
                    segments = translationResult.segments,
                    dubbingAudioFile = dubbingFile,
                    outputVideoFile = outputVideo,
                    onProgress = { renderProgress ->
                        val mapped = 0.72f + (renderProgress * 0.28f)
                        _uiState.value = TranslationUiState.Processing(
                            video = video,
                            stage = ProcessingStage.RENDERING_VIDEO,
                            progress = mapped.coerceIn(0.72f, 0.99f)
                        )
                    }
                )

                _uiState.value = TranslationUiState.Success(
                    video = video,
                    outputVideoFile = outputVideo,
                    srtContent = translationResult.srtContent,
                    segments = translationResult.segments,
                    targetLanguage = targetLang,
                    dubbingEnabled = dubbingFile != null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Translation timed out", e)
                _uiState.value = TranslationUiState.Error(
                    video = video,
                    message = "Gemini translation timed out after 60 seconds. Try a shorter clip."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Translation pipeline failed", e)
                _uiState.value = TranslationUiState.Error(
                    video = video,
                    message = e.localizedMessage ?: "Translation failed. Please try again."
                )
            }
        }
    }

    fun saveToGallery() {
        val currentState = _uiState.value as? TranslationUiState.Success ?: return
        if (currentState.isSaving || currentState.isSavedToGallery) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isSaving = true, saveStatusMessage = null)
            val saved = videoProcessor.saveVideoToGallery(currentState.outputVideoFile)
            val latest = _uiState.value as? TranslationUiState.Success ?: return@launch
            _uiState.value = if (saved) {
                latest.copy(
                    isSaving = false,
                    isSavedToGallery = true,
                    saveStatusMessage = "Saved to Movies/VideoTranslator"
                )
            } else {
                latest.copy(
                    isSaving = false,
                    isSavedToGallery = false,
                    saveStatusMessage = "Could not save video to gallery."
                )
            }
        }
    }

    fun resetToReady() {
        val video = currentVideo
        _uiState.value = if (video != null) {
            TranslationUiState.VideoReady(video)
        } else {
            TranslationUiState.Idle
        }
    }
}
