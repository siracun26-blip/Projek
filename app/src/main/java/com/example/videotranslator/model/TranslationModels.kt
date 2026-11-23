package com.example.videotranslator.model

import android.net.Uri
import java.io.File
import java.util.Locale

enum class TargetLanguage(
    val displayName: String,
    val nativeLabel: String,
    val code: String,
    val locale: Locale
) {
    INDONESIAN(
        displayName = "Bahasa Indonesia",
        nativeLabel = "Indonesia",
        code = "ID",
        locale = Locale.forLanguageTag("id-ID")
    ),
    ENGLISH(
        displayName = "Bahasa Inggris",
        nativeLabel = "English",
        code = "EN",
        locale = Locale.US
    ),
    JAPANESE(
        displayName = "Bahasa Jepang",
        nativeLabel = "日本語",
        code = "JA",
        locale = Locale.JAPAN
    ),
    KOREAN(
        displayName = "Bahasa Korea",
        nativeLabel = "한국어",
        code = "KO",
        locale = Locale.KOREA
    ),
    ARABIC(
        displayName = "Bahasa Arab",
        nativeLabel = "العربية",
        code = "AR",
        locale = Locale.forLanguageTag("ar")
    )
}

data class SubtitleSegment(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val startTimestamp: String,
    val endTimestamp: String,
    val text: String
)

data class VideoMetadata(
    val uri: Uri,
    val fileName: String,
    val durationMs: Long,
    val sizeBytes: Long
)

enum class ProcessingStage {
    PREPARING,
    EXTRACTING_AUDIO,
    AI_TRANSLATING,
    SYNTHESIZING_DUB,
    RENDERING_VIDEO
}

sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data class VideoReady(val video: VideoMetadata) : TranslationUiState
    data class Processing(
        val video: VideoMetadata,
        val stage: ProcessingStage,
        val progress: Float
    ) : TranslationUiState
    data class Success(
        val video: VideoMetadata,
        val outputVideoFile: File,
        val srtContent: String,
        val segments: List<SubtitleSegment>,
        val targetLanguage: TargetLanguage,
        val dubbingEnabled: Boolean,
        val isSaving: Boolean = false,
        val isSavedToGallery: Boolean = false,
        val saveStatusMessage: String? = null
    ) : TranslationUiState
    data class Error(
        val video: VideoMetadata?,
        val message: String
    ) : TranslationUiState
}
