package com.example.videotranslator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.videotranslator.R
import com.example.videotranslator.model.ProcessingStage
import com.example.videotranslator.model.TargetLanguage
import com.example.videotranslator.model.TranslationUiState
import com.example.videotranslator.model.VideoMetadata
import com.example.videotranslator.ui.components.VideoPlayerCard
import com.example.videotranslator.ui.theme.ElectricIndigo
import com.example.videotranslator.ui.theme.LuminousCyan
import com.example.videotranslator.ui.theme.StudioObsidian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    uiState: TranslationUiState,
    selectedLanguage: TargetLanguage,
    dubbingEnabled: Boolean,
    onVideoPicked: (android.net.Uri) -> Unit,
    onLanguageSelected: (TargetLanguage) -> Unit,
    onDubbingToggled: (Boolean) -> Unit,
    onStartTranslation: () -> Unit,
    onSaveToGallery: () -> Unit,
    onViewSubtitles: () -> Unit,
    onReset: () -> Unit
) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onVideoPicked(uri)
            }
        }
    )

    val launchPicker = {
        videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }

    val isProcessing = uiState is TranslationUiState.Processing
    val activeVideo: VideoMetadata? = when (uiState) {
        is TranslationUiState.VideoReady -> uiState.video
        is TranslationUiState.Processing -> uiState.video
        is TranslationUiState.Success -> uiState.video
        is TranslationUiState.Error -> uiState.video
        TranslationUiState.Idle -> null
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricIndigo, LuminousCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = stringResource(R.string.studio_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWide = maxWidth >= 760.dp

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SourceVideoSection(
                            video = activeVideo,
                            enabled = !isProcessing,
                            onPickVideo = launchPicker
                        )
                        LanguageSelectorSection(
                            selectedLanguage = selectedLanguage,
                            enabled = !isProcessing,
                            onLanguageSelected = onLanguageSelected
                        )
                        DubbingToggleCard(
                            enabled = !isProcessing,
                            checked = dubbingEnabled,
                            onCheckedChange = onDubbingToggled
                        )
                        TranslateActionButton(
                            canStart = activeVideo != null && !isProcessing,
                            isProcessing = isProcessing,
                            onClick = onStartTranslation
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WorkspaceStageContent(
                            uiState = uiState,
                            dubbingEnabled = dubbingEnabled,
                            onSaveToGallery = onSaveToGallery,
                            onViewSubtitles = onViewSubtitles,
                            onReset = onReset,
                            onRetry = onStartTranslation
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState !is TranslationUiState.Success) {
                            StudioHeroBanner()
                        }

                        SourceVideoSection(
                            video = activeVideo,
                            enabled = !isProcessing,
                            onPickVideo = launchPicker
                        )

                        LanguageSelectorSection(
                            selectedLanguage = selectedLanguage,
                            enabled = !isProcessing,
                            onLanguageSelected = onLanguageSelected
                        )

                        DubbingToggleCard(
                            enabled = !isProcessing,
                            checked = dubbingEnabled,
                            onCheckedChange = onDubbingToggled
                        )

                        TranslateActionButton(
                            canStart = activeVideo != null && !isProcessing,
                            isProcessing = isProcessing,
                            onClick = onStartTranslation
                        )

                        if (uiState !is TranslationUiState.Idle && uiState !is TranslationUiState.VideoReady) {
                            WorkspaceStageContent(
                                uiState = uiState,
                                dubbingEnabled = dubbingEnabled,
                                onSaveToGallery = onSaveToGallery,
                                onViewSubtitles = onViewSubtitles,
                                onReset = onReset,
                                onRetry = onStartTranslation
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioHeroBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_studio_hero),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                StudioObsidian.copy(alpha = 0.25f),
                                StudioObsidian.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, LuminousCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = LuminousCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.hero_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = LuminousCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.hero_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceVideoSection(
    video: VideoMetadata?,
    enabled: Boolean,
    onPickVideo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = stringResource(R.string.cd_video_icon),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.label_source_video),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (video != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = video.fileName,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val durationSec = (video.durationMs / 1_000L).coerceAtLeast(0L)
                            val mins = durationSec / 60L
                            val secs = durationSec % 60L
                            val sizeMb = video.sizeBytes / (1024f * 1024f)
                            Text(
                                text = String.format("%02d:%02d  •  %.1f MB", mins, secs, sizeMb),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onPickVideo,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("pick_video_button"),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (video == null) {
                        stringResource(R.string.action_pick_video)
                    } else {
                        stringResource(R.string.action_change_video)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSelectorSection(
    selectedLanguage: TargetLanguage,
    enabled: Boolean,
    onLanguageSelected: (TargetLanguage) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.label_target_language),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetLanguage.entries.forEach { language ->
                    val isSelected = language == selectedLanguage
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = enabled) { onLanguageSelected(language) }
                            .testTag("language_chip_${language.code.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = language.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = language.nativeLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DubbingToggleCard(
    enabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (checked) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = stringResource(R.string.cd_dubbing_icon),
                    tint = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_dubbing_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.label_dubbing_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dubbing_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StudioObsidian,
                    checkedTrackColor = LuminousCyan
                )
            )
        }
    }
}

@Composable
private fun TranslateActionButton(
    canStart: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = canStart,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("translate_button"),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.action_processing),
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.action_start_translation),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun WorkspaceStageContent(
    uiState: TranslationUiState,
    dubbingEnabled: Boolean,
    onSaveToGallery: () -> Unit,
    onViewSubtitles: () -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit
) {
    AnimatedVisibility(
        visible = uiState is TranslationUiState.Idle || uiState is TranslationUiState.VideoReady,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        StudioHeroBanner()
    }

    when (uiState) {
        is TranslationUiState.Processing -> {
            ProcessingStatusCard(
                stage = uiState.stage,
                progress = uiState.progress,
                dubbingEnabled = dubbingEnabled
            )
        }
        is TranslationUiState.Success -> {
            SuccessResultSection(
                state = uiState,
                onSaveToGallery = onSaveToGallery,
                onViewSubtitles = onViewSubtitles,
                onReset = onReset
            )
        }
        is TranslationUiState.Error -> {
            ErrorStatusCard(
                message = uiState.message,
                onRetry = onRetry
            )
        }
        TranslationUiState.Idle,
        is TranslationUiState.VideoReady -> Unit
    }
}

@Composable
private fun ProcessingStatusCard(
    stage: ProcessingStage,
    progress: Float,
    dubbingEnabled: Boolean
) {
    val stageText = when (stage) {
        ProcessingStage.PREPARING -> stringResource(R.string.stage_preparing)
        ProcessingStage.EXTRACTING_AUDIO -> stringResource(R.string.stage_extracting_audio)
        ProcessingStage.AI_TRANSLATING -> stringResource(R.string.stage_ai_translating)
        ProcessingStage.SYNTHESIZING_DUB -> stringResource(R.string.stage_synthesizing_dub)
        ProcessingStage.RENDERING_VIDEO -> stringResource(R.string.stage_rendering_video)
    }

    val stages = buildList {
        add(ProcessingStage.PREPARING to stringResource(R.string.stage_preparing))
        add(ProcessingStage.EXTRACTING_AUDIO to stringResource(R.string.stage_extracting_audio))
        add(ProcessingStage.AI_TRANSLATING to stringResource(R.string.stage_ai_translating))
        if (dubbingEnabled) {
            add(ProcessingStage.SYNTHESIZING_DUB to stringResource(R.string.stage_synthesizing_dub))
        }
        add(ProcessingStage.RENDERING_VIDEO to stringResource(R.string.stage_rendering_video))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("processing_status_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stageText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = LuminousCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val activeOrdinal = stage.ordinal
                stages.forEach { (stepStage, label) ->
                    val completed = stepStage.ordinal < activeOrdinal
                    val current = stepStage == stage
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        completed -> MaterialTheme.colorScheme.tertiary
                                        current -> LuminousCyan
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                completed -> MaterialTheme.colorScheme.tertiary
                                current -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessResultSection(
    state: TranslationUiState.Success,
    onSaveToGallery: () -> Unit,
    onViewSubtitles: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = stringResource(R.string.label_preview_result),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = state.targetLanguage.code,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            VideoPlayerCard(videoFile = state.outputVideoFile)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveToGallery,
                    enabled = !state.isSaving && !state.isSavedToGallery,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_gallery_button"),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isSavedToGallery) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isSavedToGallery) {
                            stringResource(R.string.action_saved_gallery)
                        } else {
                            stringResource(R.string.action_save_gallery)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onViewSubtitles,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("view_subtitles_button"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${state.segments.size} SRT")
                }
            }

            state.saveStatusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isSavedToGallery) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("translate_another_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.action_translate_another))
            }
        }
    }
}

@Composable
private fun ErrorStatusCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_status_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("retry_button"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = stringResource(R.string.action_retry),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
