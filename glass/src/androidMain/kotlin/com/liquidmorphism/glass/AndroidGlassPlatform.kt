package com.liquidmorphism.glass

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEachIndexed

internal actual fun createPlatformGlassScope(density: Density): GlassScope =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassScopeImpl(density)
    } else {
        GlassScopeFallbackImpl(density)
    }

@Composable
internal actual fun Modifier.platformParentGlass(backdrop: GlassBackdrop): Modifier {
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

internal actual fun supportsNativeBackdrop(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private class GlassScopeImpl(private val density: Density) : GlassRegistryScope {
    override var updateCounter by mutableStateOf(0)
    override var origin: Offset = Offset.Zero
    override val elements = mutableStateListOf<GlassElement>()

    override fun clearElements() {
        if (elements.isNotEmpty()) {
            elements.clear()
            updateCounter++
        }
    }

    private fun removeElement(elementId: String) {
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
        shape: androidx.compose.foundation.shape.CornerBasedShape,
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
                    ),
                )
            }
    }
}

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
        shader,
        "contents",
    ).asComposeRenderEffect()
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
        float blurMask = 0.0;
        float2 surfaceNormal = float2(0.0);
        
        // Process each glass element
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            float2 center = glassPositions[i] + glassSizes[i] * 0.5;
            float2 localCoord = fragCoord - center;
            float2 halfSize = glassSizes[i] * 0.5;
            float cornerRadius = cornerRadii[i];
            
            float sdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
            
            // Feather the shape edge so the backdrop blur stays smooth and anti-aliased.
            float elementMask = 1.0 - smoothstep(-1.25, 1.25, sdf);
            if (elementMask > 0.0 && glassBlurs[i] > 0.0) {
                blurRadius = max(blurRadius, min(glassBlurs[i], 96.0));
                blurMask = max(blurMask, elementMask);
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
        
        // Sample the untouched background first so only the glass shape is blurred.
        float4 originalColor = contents.eval(finalCoord);
        float4 color = originalColor;
        
        // A dense Gaussian kernel avoids the tiled/frosted pattern of sparse sampling.
        if (blurRadius > 0.0) {
            float4 blurredColor = float4(0.0);
            float totalWeight = 0.0;
            
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -6; dy <= 6; dy++) {
                    float2 normalizedOffset = float2(float(dx), float(dy)) / 6.0;
                    float distanceSquared = dot(normalizedOffset, normalizedOffset);
                    float weight = exp(-distanceSquared * 3.2);
                    float2 offset = normalizedOffset * blurRadius;
                    blurredColor += contents.eval(finalCoord + offset) * weight;
                    totalWeight += weight;
                }
            }
            color = mix(originalColor, blurredColor / totalWeight, blurMask);
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
