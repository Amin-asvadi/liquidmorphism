package com.liquidmorphism.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

private const val LIQUID_GLASS_MAX_BLUR_RADIUS_PX = 20f
private const val GLASSMORPHISM_MAX_BLUR_RADIUS_PX = 96f
private const val MAX_BLUR_CONTROL_DP = 36f

internal data class GlassElement(
    val id: String,
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float,
    val elevation: Float,
    val tint: Color,
    val darkness: Float,
    val warpEdges: Float,
) {
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false

        val tolerance = 0.01f
        val positionDiff = position - other.position
        val positionDistance = sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
            abs(size.width - other.size.width) < tolerance &&
            abs(size.height - other.size.height) < tolerance &&
            abs(scale - other.scale) < tolerance &&
            abs(blur - other.blur) < tolerance &&
            abs(centerDistortion - other.centerDistortion) < tolerance &&
            abs(cornerRadius - other.cornerRadius) < tolerance &&
            abs(elevation - other.elevation) < tolerance &&
            abs(darkness - other.darkness) < tolerance &&
            abs(warpEdges - other.warpEdges) < tolerance &&
            tint == other.tint
    }
}

interface GlassScope {
    fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
        darkness: Float = 0f,
        warpEdges: Float = 0f,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

internal interface GlassRegistryScope : GlassScope {
    val updateCounter: Int
    var origin: Offset
    val elements: List<GlassElement>
    fun clearElements()
}

val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }

class GlassBackdrop internal constructor(internal val glassScope: GlassScope) {
    internal val updateCounter: Int
        get() = registry?.updateCounter ?: 0

    internal val elements: List<GlassElement>
        get() = registry?.elements.orEmpty()

    private val registry: GlassRegistryScope?
        get() = glassScope as? GlassRegistryScope

    internal fun setOrigin(origin: Offset) {
        registry?.origin = origin
    }

    fun clear() {
        registry?.clearElements()
    }
}

class GlassEffectsScope internal constructor(
    density: Density,
) : Density by density {
    internal var blur: Float = 0f
    internal var scale: Float = 0f
    internal var centerDistortion: Float = 0f
    internal var elevation: Dp = 0.dp
    internal var tint: Color = Color.Transparent
    internal var darkness: Float = 0f
    internal var warpEdges: Float = 0f

    fun blur(radiusPx: Float) {
        blur = radiusPx.coerceIn(0f, LIQUID_GLASS_MAX_BLUR_RADIUS_PX)
    }

    internal fun glassmorphismBlur(radiusPx: Float) {
        blur = radiusPx.coerceIn(0f, GLASSMORPHISM_MAX_BLUR_RADIUS_PX)
    }

    fun scale(value: Float) {
        scale = value.coerceIn(0f, 1f)
    }

    fun centerDistortion(value: Float) {
        centerDistortion = value.coerceIn(0f, 1f)
    }

    fun elevation(value: Dp) {
        elevation = value
    }

    fun tint(value: Color) {
        tint = value
    }

    fun darkness(value: Float) {
        darkness = value.coerceIn(0f, 1f)
    }

    fun warpEdges(value: Float) {
        warpEdges = value.coerceIn(0f, 1f)
    }
}

class GlassmorphismEffectsScope internal constructor(
    density: Density,
) : Density by density {
    internal var blur: Float = 0f
    internal var blurPx: Float = 0f
    internal var elevation: Dp = 16.dp
    internal var tint: Color = Color.White.copy(alpha = 0.18f)
    internal var borderAlpha: Float = 0.38f
    internal var highlightAlpha: Float = 0.34f
    internal var shadowAlpha: Float = 0.28f
    internal var frostedEnabled: Boolean = false
    internal var frostIntensity: Float = 0.55f

    fun blur(radiusPx: Float) {
        blurPx = radiusPx.coerceAtLeast(0f)
        blur = (radiusPx / MAX_BLUR_CONTROL_DP.dp.toPx()).coerceIn(0f, 1f)
    }

    fun elevation(value: Dp) {
        elevation = value
    }

    fun tint(value: Color) {
        tint = value
    }

    fun borderAlpha(value: Float) {
        borderAlpha = value.coerceIn(0f, 1f)
    }

    fun highlightAlpha(value: Float) {
        highlightAlpha = value.coerceIn(0f, 1f)
    }

    fun shadowAlpha(value: Float) {
        shadowAlpha = value.coerceIn(0f, 1f)
    }

    fun frosted(enabled: Boolean, intensity: Float = 0.55f) {
        frostedEnabled = enabled
        frostIntensity = intensity.coerceIn(0f, 1f)
    }
}

internal expect fun createPlatformGlassScope(density: Density): GlassScope

@Composable
internal expect fun Modifier.platformParentGlass(backdrop: GlassBackdrop): Modifier

internal expect fun supportsNativeBackdrop(): Boolean

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val density = LocalDensity.current
    return remember { GlassBackdrop(createPlatformGlassScope(density)) }
}

@Composable
fun rememberLayerBackdrop(): GlassBackdrop = rememberGlassBackdrop()

@Composable
fun GlassBackdropProvider(
    backdrop: GlassBackdrop,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        content()
    }
}

@Composable
fun Modifier.parentGlass(backdrop: GlassBackdrop): Modifier = platformParentGlass(backdrop)

@Composable
fun Modifier.layerBackdrop(backdrop: GlassBackdrop): Modifier = parentGlass(backdrop)

@Composable
fun Modifier.parentGlass(): Modifier {
    val backdrop = LocalGlassBackdrop.current
        ?: error("No GlassBackdrop found. Wrap this UI with GlassBackdropProvider.")
    return parentGlass(backdrop)
}

@Composable
fun Modifier.layerBackdrop(): Modifier = parentGlass()

fun Modifier.drawBackdrop(
    backdrop: GlassBackdrop,
    shape: () -> CornerBasedShape,
    effects: GlassEffectsScope.() -> Unit = { },
): Modifier = composed {
    val id = remember { Random.nextLong() }
    val density = LocalDensity.current
    val effectScope = GlassEffectsScope(density).apply(effects)

    with(backdrop.glassScope) {
        glassBackground(
            id = id,
            scale = effectScope.scale,
            blur = effectScope.blur,
            centerDistortion = effectScope.centerDistortion,
            shape = shape(),
            elevation = effectScope.elevation,
            tint = effectScope.tint,
            darkness = effectScope.darkness,
            warpEdges = effectScope.warpEdges,
        )
    }
}

fun Modifier.drawBackdrop(
    shape: () -> CornerBasedShape,
    effects: GlassEffectsScope.() -> Unit = { },
): Modifier = composed {
    val backdrop = LocalGlassBackdrop.current
        ?: error("No GlassBackdrop found. Wrap this UI with GlassBackdropProvider.")

    drawBackdrop(
        backdrop = backdrop,
        shape = shape,
        effects = effects,
    )
}

fun Modifier.glassmorphism(
    shape: () -> CornerBasedShape,
    effects: GlassmorphismEffectsScope.() -> Unit = { },
): Modifier = composed {
    val density = LocalDensity.current
    val effectScope = GlassmorphismEffectsScope(density).apply(effects)
    val glassShape = shape()
    val backdrop = LocalGlassBackdrop.current
    val frost = if (effectScope.frostedEnabled) effectScope.frostIntensity else 0f
    val borderWidth = (1.dp + (effectScope.blur * 0.8f).dp)
    val glowWidth = with(density) { (2.dp + (effectScope.blur * 3f).dp).toPx() }
    val clearTintAlpha = effectScope.tint.alpha * 0.24f
    val frostAlpha = frost * 0.34f
    val baseTint = effectScope.tint.copy(
        alpha = (clearTintAlpha + frostAlpha).coerceIn(0f, 0.52f),
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = effectScope.borderAlpha),
            Color.White.copy(alpha = effectScope.borderAlpha * 0.18f),
            Color.White.copy(alpha = effectScope.borderAlpha * 0.4f),
        ),
    )

    this
        .shadow(
            elevation = effectScope.elevation,
            shape = glassShape,
            ambientColor = Color.Black.copy(alpha = effectScope.shadowAlpha * 0.72f),
            spotColor = Color.Black.copy(alpha = effectScope.shadowAlpha),
        )
        .let { modifier ->
            if (supportsNativeBackdrop() && backdrop != null) {
                modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { glassShape },
                    effects = {
                        glassmorphismBlur(effectScope.blurPx)
                        elevation(effectScope.elevation)
                        tint(
                            effectScope.tint.copy(
                                alpha = effectScope.tint.alpha * (0.28f + frost * 0.42f),
                            ),
                        )
                        darkness(effectScope.shadowAlpha * 0.12f)
                    },
                )
            } else {
                modifier
            }
        }
        .clip(glassShape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(
                        alpha = (baseTint.alpha + effectScope.highlightAlpha * 0.08f)
                            .coerceAtMost(0.62f),
                    ),
                    baseTint,
                    baseTint.copy(alpha = baseTint.alpha * 0.54f),
                ),
            ),
            shape = glassShape,
        )
        .drawWithCache {
            val outline = glassShape.createOutline(size, layoutDirection, this)
            val outlinePath = outline.toPathOrNull()
            val topHighlight = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(
                        alpha = effectScope.highlightAlpha * (0.56f + frost * 0.44f),
                    ),
                    Color.White.copy(
                        alpha = effectScope.highlightAlpha * (0.14f + frost * 0.1f),
                    ),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(size.width * 0.74f, size.height * 0.44f),
            )
            val innerShade = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = effectScope.shadowAlpha * 0.16f),
                ),
                start = Offset(size.width * 0.5f, size.height * 0.2f),
                end = Offset(size.width * 0.5f, size.height),
            )

            onDrawWithContent {
                drawFallbackOutline(outline, outlinePath, innerShade)
                drawFallbackOutline(outline, outlinePath, topHighlight)
                drawContent()
                drawFallbackOutline(
                    outline = outline,
                    path = outlinePath,
                    color = Color.White.copy(alpha = effectScope.highlightAlpha * 0.18f),
                    style = Stroke(width = glowWidth),
                )
            }
        }
        .border(width = borderWidth, brush = borderBrush, shape = glassShape)
}

@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    scale: Float = 0f,
    blur: Float = 0f,
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Transparent,
    darkness: Float = 0f,
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier = modifier.glassBackground(
            id,
            scale.coerceIn(0f, 1f),
            blur.coerceIn(0f, 1f) * LIQUID_GLASS_MAX_BLUR_RADIUS_PX,
            centerDistortion.coerceIn(0f, 1f),
            shape,
            elevation,
            tint,
            darkness.coerceIn(0f, 1f),
            warpEdges.coerceIn(0f, 1f),
        ),
        contentAlignment,
        propagateMinConstraints,
        content,
    )
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val backdrop = rememberGlassBackdrop()
    GlassBackdropProvider(backdrop) {
        Box(modifier = modifier.parentGlass(backdrop)) {
            content()
        }
        Box(modifier = modifier) {
            GlassBoxScopeImpl(this, backdrop.glassScope).glassContent()
        }
    }
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope,
) : GlassBoxScope,
    BoxScope by boxScope,
    GlassScope by glassScope

internal class GlassScopeFallbackImpl(private val density: Density) : GlassScope {
    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier = composed {
        val blurAmount = (blur / LIQUID_GLASS_MAX_BLUR_RADIUS_PX).coerceIn(0f, 1f)
        val fillAlpha = (0.12f + blurAmount * 0.22f + tint.alpha * 0.68f)
            .coerceIn(0.12f, 0.42f)
        val baseTint = if (tint == Color.Transparent) {
            Color.White.copy(alpha = fillAlpha)
        } else {
            tint.copy(alpha = fillAlpha)
        }
        val edgeAlpha = (0.42f + warpEdges * 0.32f + centerDistortion * 0.18f)
            .coerceIn(0.42f, 0.86f)
        val borderWidth = (1f + blurAmount * 0.8f + warpEdges * 1.6f).dp
        val fallbackElevation = with(density) {
            (elevation.toPx() * (0.7f + scale * 0.7f)).toDp()
        }
        val glareWidth = with(density) { (2.dp + (warpEdges * 3f).dp).toPx() }

        val glassFill = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (baseTint.alpha + 0.2f).coerceAtMost(0.56f)),
                baseTint,
                baseTint.copy(alpha = (baseTint.alpha * 0.48f).coerceAtLeast(0.07f)),
                Color.Black.copy(
                    alpha = (darkness * 0.2f + blurAmount * 0.04f).coerceIn(0f, 0.32f),
                ),
            ),
        )
        val edgeBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = edgeAlpha),
                Color.White.copy(alpha = edgeAlpha * 0.18f),
                Color.White.copy(alpha = edgeAlpha * 0.1f),
                Color.White.copy(alpha = edgeAlpha * 0.42f),
            ),
        )

        this
            .graphicsLayer {
                scaleX = 1f + scale * 0.035f
                scaleY = 1f + scale * 0.035f
            }
            .shadow(
                elevation = fallbackElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.28f + darkness * 0.18f),
                spotColor = Color.Black.copy(alpha = 0.34f + darkness * 0.22f),
            )
            .clip(shape)
            .background(brush = glassFill, shape = shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(
                            alpha = 0.04f + centerDistortion * 0.16f + blurAmount * 0.06f,
                        ),
                        Color.White.copy(alpha = centerDistortion * 0.05f),
                        Color.Transparent,
                    ),
                ),
                shape = shape,
            )
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val outlinePath = outline.toPathOrNull()
                val topLeftGlare = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.36f + blurAmount * 0.18f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.72f, size.height * 0.5f),
                )
                val bottomShade = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = (0.08f + darkness * 0.16f).coerceIn(0f, 0.22f)),
                    ),
                    start = Offset(size.width * 0.5f, size.height * 0.24f),
                    end = Offset(size.width * 0.5f, size.height),
                )
                val innerRim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f + warpEdges * 0.12f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.16f + warpEdges * 0.16f),
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )

                onDrawWithContent {
                    drawFallbackOutline(outline, outlinePath, bottomShade)
                    drawFallbackOutline(outline, outlinePath, topLeftGlare)
                    drawContent()
                    drawFallbackOutline(
                        outline = outline,
                        path = outlinePath,
                        brush = innerRim,
                        style = Stroke(width = glareWidth),
                    )
                    drawFallbackOutline(
                        outline = outline,
                        path = outlinePath,
                        color = Color.White.copy(alpha = 0.08f + centerDistortion * 0.08f),
                        style = Stroke(width = glareWidth * 0.45f),
                    )
                }
            }
            .border(width = borderWidth, brush = edgeBrush, shape = shape)
            .border(
                width = 0.5.dp,
                color = Color.Black.copy(alpha = (darkness * 0.22f).coerceIn(0f, 0.22f)),
                shape = shape,
            )
    }
}

private fun Outline.toPathOrNull(): Path? = when (this) {
    is Outline.Generic -> path
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Rectangle -> null
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackOutline(
    outline: Outline,
    path: Path?,
    brush: Brush,
    style: DrawStyle = Fill,
) {
    if (path != null) {
        drawPath(path = path, brush = brush, style = style)
    } else if (outline is Outline.Rectangle) {
        drawRect(
            brush = brush,
            topLeft = outline.rect.topLeft,
            size = outline.rect.size,
            style = style,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackOutline(
    outline: Outline,
    path: Path?,
    color: Color,
    style: DrawStyle = Fill,
) {
    if (path != null) {
        drawPath(path = path, color = color, style = style)
    } else if (outline is Outline.Rectangle) {
        drawRect(
            color = color,
            topLeft = outline.rect.topLeft,
            size = outline.rect.size,
            style = style,
        )
    }
}
