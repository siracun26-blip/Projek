# Video Translator

An Android application built with Kotlin, Jetpack Compose, Material Design 3, Jetpack Media3, and Firebase AI Logic (`gemini-3.8-flash`).

## Features

- **Zero-Permission Video Selection**: Uses Android's Photo Picker (`PickVisualMedia`) to select videos from the device gallery without broad storage permissions.
- **Audio Extraction & Subtitling**: Extracts audio using Jetpack Media3 `Transformer` and generates structured, timestamped `.srt` subtitle translations via Firebase AI Logic.
- **Supported Target Languages**: Bahasa Indonesia, English, Japanese, Korean, and Arabic.
- **Optional AI Voice Dubbing (TTS)**: Synthesizes translated speech via Android `TextToSpeech` and replaces the video's audio track using a Media3 `Composition`.
- **Burned-In Subtitle Rendering & Preview**: Renders timed subtitle overlays directly onto exported MP4 frames with `BitmapOverlay` and previews the result in-app with ExoPlayer.
- **Subtitle Script Viewer & Gallery Export**: Lets users inspect every generated `.srt` cue and save the final video to `Movies/VideoTranslator` via `MediaStore`.
