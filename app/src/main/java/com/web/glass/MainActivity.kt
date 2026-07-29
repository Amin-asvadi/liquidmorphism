package com.web.glass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.glass.GlassBackdropProvider
import com.mrtdk.glass.drawBackdrop
import com.mrtdk.glass.layerBackdrop
import com.mrtdk.glass.rememberLayerBackdrop
import com.web.glass.ui.theme.GlassTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlassTheme(dynamicColor = false) {
                LiquidGlassDemo()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiquidGlassDemo() {
    val navController = rememberNavController()
    val backdrop = rememberLayerBackdrop()
    var homeGlass by remember { mutableStateOf(GlassSettings()) }
    var homePanelOffset by remember { mutableStateOf(Offset.Zero) }
    var visibleSheet by remember { mutableIntStateOf(NoSheet) }
    var detailsGlass by remember {
        mutableStateOf(
            GlassSettings(
                blurDp = 18f,
                scale = 0.16f,
                centerDistortion = 0.1f,
                elevationDp = 16f,
                tintAlpha = 0.18f,
                darkness = 0.14f,
                warpEdges = 0.36f
            )
        )
    }
    var detailsPanelOffset by remember { mutableStateOf(Offset.Zero) }

    Surface(
        color = Color.Black,
        modifier = Modifier
            .layerBackdrop(backdrop)
            .fillMaxSize()
    ) {
        GlassBackdropProvider(backdrop) {
            NavHost(
                navController = navController,
                startDestination = GlassRoute.Home,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None }
            ) {
                composable(GlassRoute.Home) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HomePage(
                            glassSettings = homeGlass,
                            panelOffset = homePanelOffset,
                            onPanelDrag = { homePanelOffset += it },
                            onOpenSettings = { visibleSheet = HomeSheet },
                            onOpenDetails = {
                                backdrop.clear()
                                navController.navigate(GlassRoute.Details)
                            }
                        )
                    }
                }
                composable(GlassRoute.Details) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        DetailsPage(
                            glassSettings = detailsGlass,
                            panelOffset = detailsPanelOffset,
                            onPanelDrag = { detailsPanelOffset += it },
                            onOpenSettings = { visibleSheet = DetailsSheet },
                            onBackHome = {
                                backdrop.clear()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
            when (visibleSheet) {
                HomeSheet -> {
                    ModalBottomSheet(
                        onDismissRequest = { visibleSheet = NoSheet },
                        containerColor = Color(0xFF10141D),
                        contentColor = Color.White
                    ) {
                        GlassSettingsSheet(
                            title = "Liquid Glass",
                            actionText = "Open details",
                            settings = homeGlass,
                            onSettingsChange = { homeGlass = it },
                            onResetPosition = { homePanelOffset = Offset.Zero },
                            onActionClick = {
                                visibleSheet = NoSheet
                                backdrop.clear()
                                navController.navigate(GlassRoute.Details)
                            }
                        )
                    }
                }

                DetailsSheet -> {
                    ModalBottomSheet(
                        onDismissRequest = { visibleSheet = NoSheet },
                        containerColor = Color(0xFF10141D),
                        contentColor = Color.White
                    ) {
                        GlassSettingsSheet(
                            title = "Details Glass",
                            actionText = "Back home",
                            settings = detailsGlass,
                            onSettingsChange = { detailsGlass = it },
                            onResetPosition = { detailsPanelOffset = Offset.Zero },
                            onActionClick = {
                                visibleSheet = NoSheet
                                backdrop.clear()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

private const val NoSheet = -1
private const val HomeSheet = 0
private const val DetailsSheet = 1

private object GlassRoute {
    const val Home = "home"
    const val Details = "details"
}

private data class GlassSettings(
    val sizeDp: Float = 200f,
    val cornerRadiusDp: Float = 28f,
    val blurDp: Float = 18f,
    val scale: Float = 0.18f,
    val centerDistortion: Float = 0.12f,
    val elevationDp: Float = 18f,
    val tintAlpha: Float = 0.2f,
    val darkness: Float = 0.16f,
    val warpEdges: Float = 0.42f
)

@Composable
private fun BoxScope.HomePage(
    glassSettings: GlassSettings,
    panelOffset: Offset,
    onPanelDrag: (Offset) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetails: () -> Unit
) {
    HomeBackground(modifier = Modifier.fillMaxSize())
    Text(
        text = "Home page",
        modifier = Modifier
            .align(Alignment.TopStart)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        color = Color.White,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )
    SettingsButton(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        onClick = onOpenSettings
    )
    GlassPanel(
        title = "Liquid Glass",
        subtitle = "Tap to open details",
        settings = glassSettings,
        panelOffset = panelOffset,
        onPanelDrag = onPanelDrag,
        modifier = Modifier.align(Alignment.Center),
        onActionClick = onOpenDetails
    )
}

@Composable
private fun BoxScope.DetailsPage(
    glassSettings: GlassSettings,
    panelOffset: Offset,
    onPanelDrag: (Offset) -> Unit,
    onOpenSettings: () -> Unit,
    onBackHome: () -> Unit
) {
    DetailsBackground(modifier = Modifier.fillMaxSize())
    Text(
        text = "Details page",
        modifier = Modifier
            .align(Alignment.TopStart)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        color = Color.White,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )
    SettingsButton(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        onClick = onOpenSettings
    )
    GlassPanel(
        title = "Details Glass",
        subtitle = "Tap to go back",
        settings = glassSettings,
        panelOffset = panelOffset,
        onPanelDrag = onPanelDrag,
        modifier = Modifier.align(Alignment.Center),
        onActionClick = onBackHome
    )
}

@Composable
private fun GlassPanel(
    title: String,
    subtitle: String,
    settings: GlassSettings,
    panelOffset: Offset,
    onPanelDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit
) {
    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = panelOffset.x.roundToInt(),
                    y = panelOffset.y.roundToInt()
                )
            }
            .size(settings.sizeDp.dp)
            .drawBackdrop(
                shape = { RoundedCornerShape(settings.cornerRadiusDp.dp) },
                effects = {
                    blur(settings.blurDp.dp.toPx())
                    scale(settings.scale)
                    centerDistortion(settings.centerDistortion)
                    elevation(settings.elevationDp.dp)
                    tint(Color.White.copy(alpha = settings.tintAlpha))
                    darkness(settings.darkness)
                    warpEdges(settings.warpEdges)
                }
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onPanelDrag(dragAmount)
                }
            }
            .clickable(onClick = onActionClick)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.drawBackdrop(
            shape = { RoundedCornerShape(16.dp) },
            effects = {
                blur(10.dp.toPx())
                scale(0.06f)
                elevation(8.dp)
                tint(Color.Black.copy(alpha = 0.22f))
                darkness(0.2f)
                warpEdges(0.18f)
            }
        )
    ) {
        Text("Settings")
    }
}

@Composable
private fun GlassSettingsSheet(
    title: String,
    actionText: String,
    settings: GlassSettings,
    onSettingsChange: (GlassSettings) -> Unit,
    onResetPosition: () -> Unit,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$title settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            GlassSettingSlider(
                label = "Size",
                value = settings.sizeDp,
                valueRange = 120f..320f,
                suffix = "dp",
                onValueChange = { onSettingsChange(settings.copy(sizeDp = it)) }
            )
            GlassSettingSlider(
                label = "Corner radius",
                value = settings.cornerRadiusDp,
                valueRange = 0f..160f,
                suffix = "dp",
                onValueChange = { onSettingsChange(settings.copy(cornerRadiusDp = it)) }
            )
            GlassSettingSlider(
                label = "Blur",
                value = settings.blurDp,
                valueRange = 0f..36f,
                suffix = "dp",
                onValueChange = { onSettingsChange(settings.copy(blurDp = it)) }
            )
            GlassSettingSlider(
                label = "Scale",
                value = settings.scale,
                valueRange = 0f..1f,
                onValueChange = { onSettingsChange(settings.copy(scale = it)) }
            )
            GlassSettingSlider(
                label = "Distortion",
                value = settings.centerDistortion,
                valueRange = 0f..1f,
                onValueChange = { onSettingsChange(settings.copy(centerDistortion = it)) }
            )
            GlassSettingSlider(
                label = "Elevation",
                value = settings.elevationDp,
                valueRange = 0f..32f,
                suffix = "dp",
                onValueChange = { onSettingsChange(settings.copy(elevationDp = it)) }
            )
            GlassSettingSlider(
                label = "Tint",
                value = settings.tintAlpha,
                valueRange = 0f..1f,
                onValueChange = { onSettingsChange(settings.copy(tintAlpha = it)) }
            )
            GlassSettingSlider(
                label = "Darkness",
                value = settings.darkness,
                valueRange = 0f..1f,
                onValueChange = { onSettingsChange(settings.copy(darkness = it)) }
            )
            GlassSettingSlider(
                label = "Warp edges",
                value = settings.warpEdges,
                valueRange = 0f..1f,
                onValueChange = { onSettingsChange(settings.copy(warpEdges = it)) }
            )
            Button(
                onClick = onResetPosition,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Reset position")
            }
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun GlassSettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String = "",
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.84f),
                fontSize = 13.sp
            )
            Text(
                text = if (suffix.isEmpty()) {
                    "${((value * 100).roundToInt())}%"
                } else {
                    "${value.roundToInt()}$suffix"
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFCF5C),
                activeTrackColor = Color(0xFFFFCF5C),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun HomeBackground(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF07111F),
                        Color(0xFF183A47),
                        Color(0xFF63395B),
                        Color(0xFFC9684D)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .drawBehind {
                val stripe = 34.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 10.dp.toPx()
                    )
                    x += stripe
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(7) { index ->
            HomeBand(index)
        }
    }
}

@Composable
private fun DetailsBackground(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF101014),
                        Color(0xFF1D2733),
                        Color(0xFF284B63),
                        Color(0xFF5FB49C)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .drawBehind {
                val step = 42.dp.toPx()
                var y = -step
                while (y < size.height + step) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y + size.width * 0.28f),
                        strokeWidth = 8.dp.toPx()
                    )
                    y += step
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(7) { index ->
            DetailsBand(index)
        }
    }
}

@Composable
private fun HomeBand(index: Int) {
    val palettes = listOf(
        listOf(Color(0xFF35C2B6), Color(0xFFFFCF5C), Color(0xFFEF6F6C)),
        listOf(Color(0xFF7B61FF), Color(0xFF45D483), Color(0xFFFF8F3D)),
        listOf(Color(0xFF00A7E1), Color(0xFFF95D6A), Color(0xFFFFD166))
    )

    ColorBand(
        label = "Home layer ${index + 1}",
        colors = palettes[index % palettes.size],
        textColor = Color.White
    )
}

@Composable
private fun DetailsBand(index: Int) {
    val palettes = listOf(
        listOf(Color(0xFF8BD3DD), Color(0xFFB8F2E6), Color(0xFFFFD6A5)),
        listOf(Color(0xFF5FB49C), Color(0xFF98C1D9), Color(0xFFE0FBFC)),
        listOf(Color(0xFF64DFDF), Color(0xFF80FFDB), Color(0xFFFFE66D))
    )

    ColorBand(
        label = "Details layer ${index + 1}",
        colors = palettes[index % palettes.size],
        textColor = Color(0xFF111827)
    )
}

@Composable
private fun ColorBand(
    label: String,
    colors: List<Color>,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(colors))
            .drawBehind {
                repeat(5) { circle ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f - circle * 0.025f),
                        radius = 18.dp.toPx() + circle * 20.dp.toPx(),
                        center = Offset(
                            x = size.width * (0.12f + circle * 0.19f),
                            y = size.height * if (circle % 2 == 0) 0.35f else 0.72f
                        )
                    )
                }
            }
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(18.dp),
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiquidGlassDemoPreview() {
    GlassTheme(dynamicColor = false) {
        MaterialTheme {
            LiquidGlassDemo()
        }
    }
}
