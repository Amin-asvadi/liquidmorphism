package com.mrtdk.glass

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.random.Random

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
    // Check equality with tolerance for Float values
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false

        val tolerance = 0.01f
        val positionDiff = (position - other.position)
        val positionDistance =
            kotlin.math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
                kotlin.math.abs(size.width - other.size.width) < tolerance &&
                kotlin.math.abs(size.height - other.size.height) < tolerance &&
                kotlin.math.abs(scale - other.scale) < tolerance &&
                kotlin.math.abs(blur - other.blur) < tolerance &&
                kotlin.math.abs(centerDistortion - other.centerDistortion) < tolerance &&
                kotlin.math.abs(cornerRadius - other.cornerRadius) < tolerance &&
                kotlin.math.abs(elevation - other.elevation) < tolerance &&
                kotlin.math.abs(darkness - other.darkness) < tolerance &&
                kotlin.math.abs(warpEdges - other.warpEdges) < tolerance &&
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

val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }

class GlassBackdrop internal constructor(internal val glassScope: GlassScope) {
    internal val updateCounter: Int
        get() = (glassScope as? GlassScopeImpl)?.updateCounter ?: 0

    internal val elements: List<GlassElement>
        get() = (glassScope as? GlassScopeImpl)?.elements.orEmpty()

    internal fun setOrigin(origin: Offset) {
        (glassScope as? GlassScopeImpl)?.origin = origin
    }

    fun clear() {
        (glassScope as? GlassScopeImpl)?.clearElements()
    }
}

class GlassEffectsScope internal constructor(
    density: Density
) : Density by density {
    internal var blur: Float = 0f
    internal var scale: Float = 0f
    internal var centerDistortion: Float = 0f
    internal var elevation: Dp = 0.dp
    internal var tint: Color = Color.Transparent
    internal var darkness: Float = 0f
    internal var warpEdges: Float = 0f

    fun blur(radiusPx: Float) {
        blur = (radiusPx / 20f).coerceIn(0f, 1f)
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

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val density = LocalDensity.current
    return remember {
        val scope: GlassScope = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GlassScopeImpl(density)
        } else {
            GlassScopeFallbackImpl(density)
        }
        GlassBackdrop(scope)
    }
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
fun Modifier.parentGlass(backdrop: GlassBackdrop): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return this
    }

    val shader = remember(backdrop) {
        RuntimeShader(GLASS_DISPLACEMENT_SHADER)
    }

    DisposableEffect(backdrop) {
        onDispose {
            backdrop.clear()
        }
    }

    return this
        .onGloballyPositioned { coordinates ->
            backdrop.setOrigin(coordinates.positionInRoot())
        }
        .glassRuntimeShader(shader, backdrop.elements)
}

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

@SuppressLint("NewApi")
private fun Modifier.glassRuntimeShader(
    shader: RuntimeShader,
    elements: List<GlassElement>,
): Modifier = graphicsLayer {
    shader.setFloatUniform("resolution", size.width, size.height)

    val maxElements = 10
    val positions = FloatArray(maxElements * 2)
    val sizes = FloatArray(maxElements * 2)
    val scales = FloatArray(maxElements)
    val radii = FloatArray(maxElements)
    val elevations = FloatArray(maxElements)
    val centerDistortions = FloatArray(maxElements)
    val tints = FloatArray(maxElements * 4)
    val darkness = FloatArray(maxElements)
    val warpEdges = FloatArray(maxElements)
    val blurs = FloatArray(maxElements)

    val elementsCount = minOf(elements.size, maxElements)
    shader.setIntUniform("elementsCount", elementsCount)

    elements.take(elementsCount).fastForEachIndexed { index, element ->
        positions[index * 2] = element.position.x
        positions[index * 2 + 1] = element.position.y
        sizes[index * 2] = element.size.width
        sizes[index * 2 + 1] = element.size.height
        scales[index] = element.scale
        radii[index] = element.cornerRadius
        elevations[index] = element.elevation
        centerDistortions[index] = element.centerDistortion

        tints[index * 4] = element.tint.red
        tints[index * 4 + 1] = element.tint.green
        tints[index * 4 + 2] = element.tint.blue
        tints[index * 4 + 3] = element.tint.alpha

        darkness[index] = element.darkness
        warpEdges[index] = element.warpEdges
        blurs[index] = element.blur
    }

    shader.setFloatUniform("glassPositions", positions)
    shader.setFloatUniform("glassSizes", sizes)
    shader.setFloatUniform("glassScales", scales)
    shader.setFloatUniform("cornerRadii", radii)
    shader.setFloatUniform("elevations", elevations)
    shader.setFloatUniform("centerDistortions", centerDistortions)
    shader.setFloatUniform("glassTints", tints)
    shader.setFloatUniform("glassDarkness", darkness)
    shader.setFloatUniform("glassWarpEdges", warpEdges)
    shader.setFloatUniform("glassBlurs", blurs)

    renderEffect = RenderEffect.createRuntimeShaderEffect(
        shader, "contents"
    ).asComposeRenderEffect()
}

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
            warpEdges = effectScope.warpEdges
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
        effects = effects
    )
}

@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0)
    scale: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    blur: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Transparent,
    @FloatRange(from = 0.0, to = 1.0)
    darkness: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier = modifier.glassBackground(
            id, 
            scale.coerceIn(0f, 1f), 
            blur.coerceIn(0f, 1f), 
            centerDistortion.coerceIn(0f, 1f), 
            shape, 
            elevation, 
            tint, 
            darkness.coerceIn(0f, 1f), 
            warpEdges.coerceIn(0f, 1f)
        ),
        contentAlignment, propagateMinConstraints, content
    )
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope
) : GlassBoxScope, BoxScope by boxScope,
    GlassScope by glassScope {

}

private class GlassScopeImpl(private val density: Density) : GlassScope {

    var updateCounter by mutableStateOf(0)
    var origin: Offset = Offset.Zero
    val elements = mutableStateListOf<GlassElement>()

    fun clearElements() {
        if (elements.isNotEmpty()) {
            elements.clear()
            updateCounter++
        }
    }

    fun removeElement(elementId: String) {
        if (elements.removeAll { it.id == elementId }) {
            updateCounter++
        }
    }

    private fun upsertElement(element: GlassElement) {
        val existingIndex = elements.indexOfFirst { it.id == element.id }
        if (existingIndex == -1) {
            elements.add(element)
            updateCounter++
            return
        }

        val existing = elements[existingIndex]
        if (!existing.equalsWithTolerance(element)) {
            elements[existingIndex] = element
            updateCounter++
        }
    }

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
        val elementId = remember(id) { "glass_$id" }

        DisposableEffect(elementId) {
            onDispose {
                removeElement(elementId)
            }
        }

        background(color = Color.Transparent, shape = shape)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot() - origin
                val size = coordinates.size.toSize()

                upsertElement(
                    GlassElement(
                        id = elementId,
                        position = position,
                        size = size,
                        cornerRadius = shape.topStart
                            .toPx(size, density)
                            .coerceAtMost(minOf(size.width, size.height) / 2f),
                        scale = scale,
                        blur = blur,
                        centerDistortion = centerDistortion,
                        elevation = with(density) { elevation.toPx() },
                        tint = tint,
                        darkness = darkness,
                        warpEdges = warpEdges,
                    )
                )
            }
    }
}

/**
 * Fallback implementation for Android versions < 13 (API 33)
 * Uses standard Compose drawing to approximate the glass effect when AGSL is unavailable.
 */
private class GlassScopeFallbackImpl(private val density: Density) : GlassScope {

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
        val fillAlpha = (0.12f + blur * 0.22f + tint.alpha * 0.68f).coerceIn(0.12f, 0.42f)
        val baseTint = if (tint == Color.Transparent) {
            Color.White.copy(alpha = fillAlpha)
        } else {
            tint.copy(alpha = fillAlpha)
        }
        val edgeAlpha = (0.42f + warpEdges * 0.32f + centerDistortion * 0.18f)
            .coerceIn(0.42f, 0.86f)
        val borderWidth = (1f + blur * 0.8f + warpEdges * 1.6f).dp
        val fallbackElevation = with(density) {
            (elevation.toPx() * (0.7f + scale * 0.7f)).toDp()
        }
        val glareWidth = with(density) { (2.dp + (warpEdges * 3f).dp).toPx() }

        val glassFill = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (baseTint.alpha + 0.2f).coerceAtMost(0.56f)),
                baseTint,
                baseTint.copy(alpha = (baseTint.alpha * 0.48f).coerceAtLeast(0.07f)),
                Color.Black.copy(alpha = (darkness * 0.2f + blur * 0.04f).coerceIn(0f, 0.32f))
            )
        )
        val edgeBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = edgeAlpha),
                Color.White.copy(alpha = edgeAlpha * 0.18f),
                Color.White.copy(alpha = edgeAlpha * 0.1f),
                Color.White.copy(alpha = edgeAlpha * 0.42f)
            )
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
                spotColor = Color.Black.copy(alpha = 0.34f + darkness * 0.22f)
            )
            .clip(shape)
            .background(brush = glassFill, shape = shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f + centerDistortion * 0.16f + blur * 0.06f),
                        Color.White.copy(alpha = centerDistortion * 0.05f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val outlinePath = outline.toPathOrNull()
                val topLeftGlare = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.36f + blur * 0.18f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.72f, size.height * 0.5f)
                )
                val bottomShade = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = (0.08f + darkness * 0.16f).coerceIn(0f, 0.22f))
                    ),
                    start = Offset(size.width * 0.5f, size.height * 0.24f),
                    end = Offset(size.width * 0.5f, size.height)
                )
                val innerRim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f + warpEdges * 0.12f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.16f + warpEdges * 0.16f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )

                onDrawWithContent {
                    drawFallbackOutline(outline, outlinePath, bottomShade)
                    drawFallbackOutline(outline, outlinePath, topLeftGlare)
                    drawContent()
                    drawFallbackOutline(
                        outline = outline,
                        path = outlinePath,
                        brush = innerRim,
                        style = Stroke(width = glareWidth)
                    )
                    drawFallbackOutline(
                        outline = outline,
                        path = outlinePath,
                        color = Color.White.copy(alpha = 0.08f + centerDistortion * 0.08f),
                        style = Stroke(width = glareWidth * 0.45f)
                    )
                }
            }
            .border(width = borderWidth, brush = edgeBrush, shape = shape)
            .border(
                width = 0.5.dp,
                color = Color.Black.copy(alpha = (darkness * 0.22f).coerceIn(0f, 0.22f)),
                shape = shape
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
            style = style
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
            style = style
        )
    }
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    // Check if AGSL is supported (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassContainerWithShader(modifier, content, glassContent)
    } else {
        GlassContainerFallback(modifier, content, glassContent)
    }
}

@SuppressLint("NewApi") // Version check is performed in GlassContainer
@Composable
private fun GlassContainerWithShader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeImpl(density) }

    val shader = remember {
        RuntimeShader(GLASS_DISPLACEMENT_SHADER)
    }

    DisposableEffect(Unit) {
        onDispose {
            glassScope.clearElements()
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                glassScope.origin = coordinates.positionInRoot()
            }
            .glassRuntimeShader(shader, glassScope.elements)
    ) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

@Composable
private fun GlassContainerFallback(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeFallbackImpl(density) }

    Box(modifier = modifier) {
        content()
    }
    Box(modifier = modifier) {
        GlassBoxScopeImpl(this, glassScope).glassContent()
    }
}

private val GLASS_DISPLACEMENT_SHADER = """
    uniform float2 resolution;
    uniform shader contents;
    uniform int elementsCount;
    uniform float2 glassPositions[10];
    uniform float2 glassSizes[10];
    uniform float glassScales[10];
    uniform float cornerRadii[10];
    uniform float elevations[10];
    uniform float centerDistortions[10];
    uniform float glassTints[40]; // 10 elements * 4 components (r,g,b,a)
    uniform float glassDarkness[10];
    uniform float glassWarpEdges[10];
    uniform float glassBlurs[10];

    // Calculate signed distance field for rounded rectangle
    float sdfRoundedRect(float2 p, float2 halfSize, float radius) {
        float2 d = abs(p) - halfSize + radius;
        return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
    }

    // Check if pixel is in warp region (0.0 = inner, 1.0 = warp zone)
    float getWarpRegion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return 0.0;
        
        float outerSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (outerSdf >= 0.0) return 0.0;
        
        // Calculate inner boundary
        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);
        
        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        return step(0.0, innerSdf);
    }

    // Apply barrel distortion in warp regions
    float2 applyWarpDistortion(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return localCoord;
        
        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);
        
        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        if (innerSdf <= 0.0) return localCoord; // No distortion in inner region
        
        // Normalize distance for smooth distortion
        float normalizedDist = clamp(innerSdf / inset, 0.0, 1.0);
        float warpIntensity = normalizedDist * normalizedDist * warpEdges;
        
        // Apply barrel distortion
        float pullStrength = warpIntensity * 0.8;
        float targetScale = max(0.1, 1.0 - pullStrength);
        float2 pulledCoord = localCoord * targetScale;
        
        // Add radial distortion
        float2 centerDir = normalize(localCoord);
        float2 radialOffset = centerDir * (warpIntensity * 0.03 * length(localCoord));
        
        // Add swirl for strong warp
        if (warpEdges > 0.7 && normalizedDist > 0.8) {
            float angle = atan(localCoord.y, localCoord.x) + normalizedDist * warpEdges * 0.5;
            float r = length(pulledCoord);
            pulledCoord = float2(cos(angle), sin(angle)) * r;
        }
        
        return pulledCoord + radialOffset;
    }

    // Apply lens magnification effect
    float2 applyLensEffect(float2 fragCoord, float2 center, float2 size, float cornerRadius, float scale, float centerDistortion) {
        if (scale <= 0.0) return fragCoord;
        
        float2 localCoord = fragCoord - center;
        float2 halfSize = size * 0.5;
        
        float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (sdf >= 0.0) return fragCoord; // Outside lens
        
        // Calculate distortion based on distance from center
        float2 rel = localCoord / halfSize;
        float normalizedDist = length(rel) / 1.414; // Normalize to diagonal
        
        float baseScale = 1.0 + scale;
        float distortionFactor = 1.0;
        
        if (centerDistortion > 0.0) {
            float profile = 1.0 - smoothstep(0.0, 1.0, normalizedDist);
            distortionFactor = 1.0 + centerDistortion * profile;
        }
        
        float finalScale = baseScale * distortionFactor;
        return center + (fragCoord - center) / finalScale;
    }

    // Calculate shadow intensity
    float getShadowIntensity(float2 localCoord, float2 halfSize, float cornerRadius, float elevation) {
        if (elevation <= 0.0) return 0.0;
        
        float shadowOffset = elevation * 0.5;
        float shadowBlur = elevation * 2.0;
        
        float2 shadowCoord = localCoord - float2(0.0, shadowOffset);
        float shadowSdf = sdfRoundedRect(shadowCoord, halfSize, cornerRadius);
        float originalSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        
        // Shadow only outside original element and within blur range
        if (originalSdf <= 0.0 || shadowSdf > shadowBlur) return 0.0;
        
        return (1.0 - shadowSdf / shadowBlur) * 0.15;
    }

    // Calculate rim highlight intensity
    float getRimHighlight(float2 localCoord, float2 halfSize, float cornerRadius) {
        float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        float rimWidth = 5.0;
        
        if (sdf <= 0.0 || sdf >= rimWidth) return 0.0;
        
        float intensity = (rimWidth - sdf) / rimWidth;
        float verticalPos = localCoord.y / halfSize.y;
        float lightingFactor = mix(1.2, 0.7, (verticalPos + 1.0) * 0.5);
        
        return intensity * 0.8 * lightingFactor;
    }

    float4 main(float2 fragCoord) {
        float2 finalCoord = fragCoord;
        float shadowAlpha = 0.0;
        float rimHighlight = 0.0;
        float4 tintColor = float4(0.0);
        float darknessEffect = 0.0;
        float blurRadius = 0.0;
        float2 surfaceNormal = float2(0.0);
        
        // Process each glass element
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            float2 center = glassPositions[i] + glassSizes[i] * 0.5;
            float2 localCoord = fragCoord - center;
            float2 halfSize = glassSizes[i] * 0.5;
            float cornerRadius = cornerRadii[i];
            
            float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
            
            // Apply blur inside element
            if (sdf < 0.0 && glassBlurs[i] > 0.0) {
                blurRadius = max(blurRadius, glassBlurs[i] * 20.0);
            }
            
            // Apply warp and lens effects
            float warpRegion = getWarpRegion(localCoord, halfSize, cornerRadius, glassWarpEdges[i]);
            if (warpRegion > 0.0) {
                float2 warpedCoord = applyWarpDistortion(localCoord, halfSize, cornerRadius, glassWarpEdges[i]);
                float2 warpedFragCoord = center + warpedCoord;
                finalCoord = applyLensEffect(warpedFragCoord, glassPositions[i] + glassSizes[i] * 0.5, 
                                           glassSizes[i], cornerRadius, glassScales[i], centerDistortions[i]);
            } else {
                finalCoord = applyLensEffect(finalCoord, center, glassSizes[i], cornerRadius, 
                                           glassScales[i], centerDistortions[i]);
            }
            
            // Accumulate effects
            shadowAlpha = max(shadowAlpha, getShadowIntensity(localCoord, halfSize, cornerRadius, elevations[i]));
            rimHighlight = max(rimHighlight, getRimHighlight(localCoord, halfSize, cornerRadius));
            
            // Store surface normal for rim highlight
            if (sdf > 0.0 && sdf < 4.0 && surfaceNormal.x == 0.0 && surfaceNormal.y == 0.0) {
                float epsilon = 1.0;
                float sdfX = sdfRoundedRect(localCoord + float2(epsilon, 0.0), halfSize, cornerRadius);
                float sdfY = sdfRoundedRect(localCoord + float2(0.0, epsilon), halfSize, cornerRadius);
                surfaceNormal = normalize(float2(sdfX - sdf, sdfY - sdf));
            }
            
            // Apply tint and darkness inside element
            if (sdf < 0.0) {
                float4 elementTint = float4(glassTints[i * 4], glassTints[i * 4 + 1], 
                                          glassTints[i * 4 + 2], glassTints[i * 4 + 3]);
                if (elementTint.a > 0.0) {
                    tintColor = mix(tintColor, elementTint, elementTint.a);
                }
                
                // Apply darkness from edges inward
                float currentDarkness = glassDarkness[i];
                if (currentDarkness > 0.0) {
                    float maxRadius = min(halfSize.x, halfSize.y) * 0.8;
                    float distanceFromEdge = abs(sdf);
                    if (distanceFromEdge < maxRadius) {
                        float intensity = smoothstep(0.0, 1.0, (maxRadius - distanceFromEdge) / maxRadius);
                        darknessEffect = max(darknessEffect, currentDarkness * intensity);
                    }
                }
            }
        }
        
        // Sample background
        float4 color = contents.eval(finalCoord);
        
        // Apply  blur
        if (blurRadius > 0.0) {
            float4 blurredColor = float4(0.0);
            float totalWeight = 0.0;
            float invRadius = 1.0 / max(blurRadius, 1.0);
            
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -5; dy <= 5; dy++) {
                    float2 offset = float2(float(dx), float(dy)) * blurRadius * 0.4;
                    float distance = length(offset) * invRadius;
                    float weight = exp(-distance * distance * 2.0);
                    blurredColor += contents.eval(finalCoord + offset) * weight;
                    totalWeight += weight;
                }
            }
            color = blurredColor / totalWeight;
        }
        
        if (tintColor.a > 0.0) {
            color.rgb = mix(color.rgb, tintColor.rgb, tintColor.a * 0.9);
        }
        
        if (darknessEffect > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), darknessEffect * 0.5);
        }
        
        // Apply rim highlight with reflection
        if (rimHighlight > 0.0) {
            float2 reflectionOffset = surfaceNormal * 24.0;
            float4 reflectedColor = contents.eval(fragCoord + reflectionOffset);
            reflectedColor.rgb = max(reflectedColor.rgb * 1.8 + 0.35, 0.15);
            color = mix(color, reflectedColor, rimHighlight);
        }
        
        if (shadowAlpha > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), shadowAlpha);
        }
        
        return color;
    }
""".trimIndent()
