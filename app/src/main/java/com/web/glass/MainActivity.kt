package com.web.glass

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.glass.drawBackdrop
import com.mrtdk.glass.layerBackdrop
import com.mrtdk.glass.rememberLayerBackdrop
import com.web.glass.ui.theme.GlassTheme
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

@Composable
private fun LiquidGlassDemo() {
    var blur by remember { mutableFloatStateOf(0.45f) }
    var scale by remember { mutableFloatStateOf(0.28f) }
    var distortion by remember { mutableFloatStateOf(0.18f) }
    var darkness by remember { mutableFloatStateOf(0.18f) }
    var warpEdges by remember { mutableFloatStateOf(0.62f) }
    val backdrop = rememberLayerBackdrop()

    Surface(color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            DemoBackground(
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize()
            )
            HeroGlassCard(
                backdrop = backdrop,
                blur = blur,
                scale = scale,
                distortion = distortion,
                darkness = darkness,
                warpEdges = warpEdges
            )
            ControlDock(
                backdrop = backdrop,
                blur = blur,
                onBlurChange = { blur = it },
                scale = scale,
                onScaleChange = { scale = it },
                distortion = distortion,
                onDistortionChange = { distortion = it },
                darkness = darkness,
                onDarknessChange = { darkness = it },
                warpEdges = warpEdges,
                onWarpEdgesChange = { warpEdges = it }
            )
        }
    }
}

@Composable
private fun DemoBackground(modifier: Modifier = Modifier) {
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
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Liquid Glass",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
        Text(
            text = "AGSL shader glass on Android 13+, with a Compose fallback for older devices.",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 16.sp,
            lineHeight = 22.sp
        )
        repeat(8) { index ->
            ColorBand(index)
        }
        Spacer(modifier = Modifier.height(260.dp))
    }
}

@Composable
private fun ColorBand(index: Int) {
    val palettes = listOf(
        listOf(Color(0xFF35C2B6), Color(0xFFFFCF5C), Color(0xFFEF6F6C)),
        listOf(Color(0xFF7B61FF), Color(0xFF45D483), Color(0xFFFF8F3D)),
        listOf(Color(0xFF00A7E1), Color(0xFFF95D6A), Color(0xFFFFD166))
    )
    val colors = palettes[index % palettes.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(colors))
            .drawBehind {
                val radius = 18.dp.toPx()
                repeat(5) { circle ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f - circle * 0.025f),
                        radius = radius + circle * 20.dp.toPx(),
                        center = Offset(
                            x = size.width * (0.12f + circle * 0.19f),
                            y = size.height * if (circle % 2 == 0) 0.35f else 0.72f
                        )
                    )
                }
            }
    ) {
        Text(
            text = "Layer ${index + 1}",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(18.dp),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BoxScope.HeroGlassCard(
    backdrop: com.mrtdk.glass.GlassBackdrop,
    blur: Float,
    scale: Float,
    distortion: Float,
    darkness: Float,
    warpEdges: Float
) {
    val blurAmount = blur
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(190.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    blur((blurAmount * 36).dp.toPx())
                    scale(scale)
                    centerDistortion(distortion)
                    elevation(18.dp)
                    tint(Color.White.copy(alpha = 0.22f))
                    darkness(darkness)
                    warpEdges(warpEdges)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "AGSL shader active"
                } else {
                    "Compose fallback active"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "GlassBox + GlassContainer",
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Blur, lens scale, edge warp, tint, darkness, and elevation are rendered through the library.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BoxScope.ControlDock(
    backdrop: com.mrtdk.glass.GlassBackdrop,
    blur: Float,
    onBlurChange: (Float) -> Unit,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    distortion: Float,
    onDistortionChange: (Float) -> Unit,
    darkness: Float,
    onDarknessChange: (Float) -> Unit,
    warpEdges: Float,
    onWarpEdgesChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(22.dp) },
                effects = {
                    blur(10.dp.toPx())
                    scale(0.08f)
                    centerDistortion(0.08f)
                    elevation(12.dp)
                    tint(Color.Black.copy(alpha = 0.22f))
                    darkness(0.28f)
                    warpEdges(0.35f)
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live controls",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "API ${Build.VERSION.SDK_INT}",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp
                )
            }
            GlassSlider("Blur", blur, onBlurChange)
            GlassSlider("Scale", scale, onScaleChange)
            GlassSlider("Distortion", distortion, onDistortionChange)
            GlassSlider("Darkness", darkness, onDarknessChange)
            GlassSlider("Warp edges", warpEdges, onWarpEdgesChange)
        }
    }
}

@Composable
private fun GlassSlider(
    label: String,
    value: Float,
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
                text = "${(value * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFCF5C),
                activeTrackColor = Color(0xFFFFCF5C),
                inactiveTrackColor = Color.White.copy(alpha = 0.22f)
            )
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
