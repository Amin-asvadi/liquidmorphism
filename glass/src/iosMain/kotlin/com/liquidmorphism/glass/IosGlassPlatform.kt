package com.liquidmorphism.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density

internal actual fun createPlatformGlassScope(density: Density): GlassScope =
    GlassScopeFallbackImpl(density)

@Composable
internal actual fun Modifier.platformParentGlass(backdrop: GlassBackdrop): Modifier = this

internal actual fun supportsNativeBackdrop(): Boolean = false
