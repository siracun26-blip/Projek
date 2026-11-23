package com.example.videotranslator.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.videotranslator.model.SubtitleSegment
import com.example.videotranslator.model.TargetLanguage
import com.example.videotranslator.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "VideoProcessor"

class VideoProcessor(private val context: Context) {

    suspend fun readVideoMetadata(uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        var fileName = "selected_video.mp4"
        var sizeBytes = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    cursor.getString(nameIndex)?.let { fileName = it }
                }
                if (sizeIndex >= 0) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        val durationMs = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read video duration", e)
            0L
        }

        VideoMetadata(
            uri = uri,
            fileName = fileName,
            durationMs = durationMs,
            sizeBytes = sizeBytes
        )
    }

    suspend fun copyUriToCache(uri: Uri): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open the selected video stream.")
        val tempFile = File(context.cacheDir, "input_${System.currentTimeMillis()}.mp4")
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }

    @OptIn(UnstableApi::class)
    suspend fun extractAudio(
        videoFile: File,
        outputAudioFile: File
    ): File = withContext(Dispatchers.Main) {
        if (outputAudioFile.exists()) outputAudioFile.delete()

        suspendCancellableCoroutine { continuation ->
            val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
            val audioOnlyItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)
                .build()

            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) {
                            continuation.resume(outputAudioFile)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        Log.e(TAG, "Audio extraction failed", exportException)
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    exportException.localizedMessage
                                        ?: "Failed to extract audio track from video."
                                )
                            )
                        }
                    }
                })
                .build()

            continuation.invokeOnCancellation {
                transformer.cancel()
            }

            transformer.start(audioOnlyItem, outputAudioFile.absolutePath)
        }
    }

    suspend fun generateDubbingAudio(
        segments: List<SubtitleSegment>,
        targetLanguage: TargetLanguage,
        outputWavFile: File
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (outputWavFile.exists()) outputWavFile.delete()

        val maxLen = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3500)
        val cleanText = segments.joinToString(". ") { it.text.trim() }
            .take(maxLen)
            .trim()

        if (cleanText.isBlank()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(targetLanguage.locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.w(TAG, "TTS voice missing for ${targetLanguage.displayName}, falling back to default.")
                }
                tts?.setSpeechRate(1.05f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        tts?.shutdown()
                        if (continuation.isActive) {
                            continuation.resume(outputWavFile.exists() && outputWavFile.length() > 0L)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        tts?.shutdown()
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS synthesis error code=$errorCode")
                        tts?.shutdown()
                        if (continuation.isActive) continuation.resume(false)
                    }
                })

                val result = tts?.synthesizeToFile(cleanText, null, outputWavFile, "dubbing_utterance")
                if (result != TextToSpeech.SUCCESS) {
                    tts?.shutdown()
                    if (continuation.isActive) continuation.resume(false)
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status=$status")
                if (continuation.isActive) continuation.resume(false)
            }
        }

        continuation.invokeOnCancellation {
            tts?.stop()
            tts?.shutdown()
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun renderFinalVideo(
        videoFile: File,
        segments: List<SubtitleSegment>,
        dubbingAudioFile: File?,
        outputVideoFile: File,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.Main) {
        if (outputVideoFile.exists()) outputVideoFile.delete()

        val subtitleOverlay = SubtitleBitmapOverlay(segments)
        val overlayEffect = OverlayEffect(listOf(subtitleOverlay))

        val videoItemBuilder = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(videoFile)))
            .setEffects(Effects(emptyList(), listOf(overlayEffect)))

        if (dubbingAudioFile != null && dubbingAudioFile.exists()) {
            videoItemBuilder.setRemoveAudio(true)
        }

        val videoSequence = EditedMediaItemSequence(listOf(videoItemBuilder.build()))

        val composition = if (dubbingAudioFile != null && dubbingAudioFile.exists()) {
            val dubItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(dubbingAudioFile)))
                .build()
            val audioSequence = EditedMediaItemSequence(listOf(dubItem))
            Composition.Builder(listOf(videoSequence, audioSequence)).build()
        } else {
            Composition.Builder(listOf(videoSequence)).build()
        }

        suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            val progressHolder = ProgressHolder()
            var isFinished = false

            lateinit var transformer: Transformer
            val progressRunnable = object : Runnable {
                override fun run() {
                    if (isFinished) return
                    val state = transformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress((progressHolder.progress / 100f).coerceIn(0f, 1f))
                    }
                    handler.postDelayed(this, 200L)
                }
            }

            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        isFinished = true
                        handler.removeCallbacks(progressRunnable)
                        onProgress(1f)
                        if (continuation.isActive) {
                            continuation.resume(outputVideoFile)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        isFinished = true
                        handler.removeCallbacks(progressRunnable)
                        Log.e(TAG, "Video rendering failed", exportException)
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    exportException.localizedMessage ?: "Failed to render final video."
                                )
                            )
                        }
                    }
                })
                .build()

            continuation.invokeOnCancellation {
                isFinished = true
                handler.removeCallbacks(progressRunnable)
                transformer.cancel()
            }

            transformer.start(composition, outputVideoFile.absolutePath)
            handler.post(progressRunnable)
        }
    }

    suspend fun saveVideoToGallery(videoFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val filename = "Translated_${System.currentTimeMillis()}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/VideoTranslator"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false

            resolver.openOutputStream(uri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to gallery", e)
            false
        }
    }
}

@OptIn(UnstableApi::class)
private class SubtitleBitmapOverlay(
    private val segments: List<SubtitleSegment>
) : BitmapOverlay() {

    private val width = 960
    private val height = 200
    private val emptyBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var cachedSegmentIndex: Int = -2
    private var cachedBitmap: Bitmap = emptyBitmap

    private val overlaySettings = StaticOverlaySettings.Builder()
        .setOverlayFrameAnchor(0f, -1f)
        .setBackgroundFrameAnchor(0f, -0.82f)
        .build()

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val strokePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
        style = Paint.Style.STROKE
        strokeWidth = 6f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(175, 11, 15, 23)
        style = Paint.Style.FILL
    }

    override fun getOverlaySettings(presentationTimeUs: Long): StaticOverlaySettings {
        return overlaySettings
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val currentMs = presentationTimeUs / 1_000L
        val activeIndex = segments.indexOfFirst { currentMs in it.startMs..it.endMs }
        if (activeIndex == cachedSegmentIndex) {
            return cachedBitmap
        }
        cachedSegmentIndex = activeIndex
        if (activeIndex < 0) {
            cachedBitmap = emptyBitmap
            return emptyBitmap
        }

        val text = segments[activeIndex].text
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val maxTextWidth = width - 80
        val fillLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1f)
            .setMaxLines(3)
            .build()

        val strokeLayout = StaticLayout.Builder.obtain(text, 0, text.length, strokePaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(4f, 1f)
            .setMaxLines(3)
            .build()

        var maxLineWidth = 0f
        for (line in 0 until fillLayout.lineCount) {
            maxLineWidth = maxOf(maxLineWidth, fillLayout.getLineWidth(line))
        }

        val textHeight = fillLayout.height.toFloat()
        val padX = 28f
        val padY = 16f
        val pillWidth = (maxLineWidth + padX * 2).coerceAtMost(width.toFloat() - 24f)
        val pillLeft = (width - pillWidth) / 2f
        val pillTop = ((height - textHeight) / 2f - padY).coerceAtLeast(8f)
        val pillRight = pillLeft + pillWidth
        val pillBottom = (pillTop + textHeight + padY * 2).coerceAtMost(height.toFloat() - 8f)

        canvas.drawRoundRect(RectF(pillLeft, pillTop, pillRight, pillBottom), 18f, 18f, pillPaint)

        canvas.save()
        canvas.translate(40f, pillTop + padY)
        strokeLayout.draw(canvas)
        fillLayout.draw(canvas)
        canvas.restore()

        cachedBitmap = bitmap
        return bitmap
    }
}
