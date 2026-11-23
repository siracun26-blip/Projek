package com.example.videotranslator.ai

import android.util.Log
import com.example.videotranslator.model.SubtitleSegment
import com.example.videotranslator.model.TargetLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

private const val TAG = "GeminiTranslatorService"
private const val MAX_INLINE_BYTES = 19 * 1024 * 1024

data class TranslationResult(
    val segments: List<SubtitleSegment>,
    val srtContent: String
)

class GeminiTranslatorService {

    private val subtitleSchema = Schema.obj(
        properties = mapOf(
            "segments" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "index" to Schema.integer(description = "1-based sequential cue number"),
                        "startTime" to Schema.string(description = "Start timestamp in SRT format HH:MM:SS,mmm"),
                        "endTime" to Schema.string(description = "End timestamp in SRT format HH:MM:SS,mmm"),
                        "text" to Schema.string(description = "Translated subtitle line")
                    )
                ),
                description = "Ordered list of timed subtitle segments covering all speech in the audio"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = subtitleSchema
            }
        )
    }

    suspend fun translateAudioToSubtitles(
        audioFile: File,
        mimeType: String,
        targetLanguage: TargetLanguage
    ): TranslationResult = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw IllegalArgumentException("Extracted audio file is missing or empty.")
        }
        if (audioFile.length() > MAX_INLINE_BYTES) {
            throw IllegalArgumentException("Audio track exceeds 19 MB limit for inline AI translation. Please choose a shorter video.")
        }

        val audioBytes = audioFile.readBytes()
        val prompt = """
            Listen to the entire audio track carefully.
            Transcribe all spoken dialogue and translate it naturally into ${targetLanguage.displayName} (${targetLanguage.nativeLabel}).
            Split the translation into concise, readable subtitle segments with accurate timestamps in SRT format (HH:MM:SS,mmm).
            Ensure startTime is strictly before endTime and segments are ordered chronologically starting at index 1.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        inlineData(audioBytes, mimeType)
                        text(prompt)
                    }
                )
            }

            val rawJson = response.text?.trim()
                ?: throw IllegalStateException("Gemini returned an empty response.")

            val segments = parseStructuredSegments(rawJson)
            if (segments.isEmpty()) {
                throw IllegalStateException("No speech or subtitle segments were detected in this video.")
            }

            val srtText = buildSrtContent(segments)
            TranslationResult(segments = segments, srtContent = srtText)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini translation timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini translation failed", e)
            throw e
        }
    }

    private fun parseStructuredSegments(jsonString: String): List<SubtitleSegment> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("segments") ?: return emptyList()
        val result = mutableListOf<SubtitleSegment>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val text = item.optString("text", "").trim()
            if (text.isBlank()) continue

            val rawStart = item.optString("startTime", "00:00:00,000").trim()
            val rawEnd = item.optString("endTime", "00:00:02,000").trim()
            val startMs = parseSrtTimestampToMs(rawStart)
            val parsedEndMs = parseSrtTimestampToMs(rawEnd)
            val endMs = if (parsedEndMs > startMs) parsedEndMs else startMs + 2_000L

            result += SubtitleSegment(
                index = item.optInt("index", result.size + 1).coerceAtLeast(1),
                startMs = startMs,
                endMs = endMs,
                startTimestamp = formatMsToSrtTimestamp(startMs),
                endTimestamp = formatMsToSrtTimestamp(endMs),
                text = text
            )
        }
        return result.sortedBy { it.startMs }.mapIndexed { idx, seg ->
            seg.copy(index = idx + 1)
        }
    }

    private fun buildSrtContent(segments: List<SubtitleSegment>): String {
        return segments.joinToString("\n\n") { seg ->
            "${seg.index}\n${seg.startTimestamp} --> ${seg.endTimestamp}\n${seg.text}"
        }
    }

    private fun parseSrtTimestampToMs(timestamp: String): Long {
        val normalized = timestamp.replace('.', ',').trim()
        val parts = normalized.split(":", ",")
        return when (parts.size) {
            4 -> {
                val hours = parts[0].toLongOrNull() ?: 0L
                val minutes = parts[1].toLongOrNull() ?: 0L
                val seconds = parts[2].toLongOrNull() ?: 0L
                val millis = parts[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
            }
            3 -> {
                val minutes = parts[0].toLongOrNull() ?: 0L
                val seconds = parts[1].toLongOrNull() ?: 0L
                val millis = parts[2].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                minutes * 60_000L + seconds * 1_000L + millis
            }
            else -> 0L
        }
    }

    private fun formatMsToSrtTimestamp(totalMs: Long): String {
        val safeMs = totalMs.coerceAtLeast(0L)
        val hours = safeMs / 3_600_000L
        val minutes = (safeMs % 3_600_000L) / 60_000L
        val seconds = (safeMs % 60_000L) / 1_000L
        val millis = safeMs % 1_000L
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
