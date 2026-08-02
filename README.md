# Liquidmorphism

Liquidmorphism is a Kotlin Multiplatform Compose glass and liquid glass effects library.

## Screenshot

![Liquidmorphism demo screenshot](docs/screenshots/liquidmorphism-home.png)

## Versions

Use the version that matches your project type:

```text
1.0.2: Android-only Jetpack Compose projects
1.0.3: Kotlin Multiplatform Compose projects
```

## Installation

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### Android-only

Use `1.0.2` for regular Android Jetpack Compose projects:

```kotlin
dependencies {
    implementation("com.github.Amin-asvadi:liquidmorphism:1.0.2")
}
```

### Kotlin Multiplatform

Use `1.0.3` for Kotlin Multiplatform Compose projects. Add it to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.Amin-asvadi:liquidmorphism:1.0.3")
        }
    }
}
```

Supported targets:

```text
Android: native liquid glass shader on Android 13+ with Compose fallback on older versions
iOS: Compose fallback glass rendering
```

## Usage

### Kotlin Multiplatform Setup

Put shared UI code in `commonMain`, then use the same composables on Android and iOS:

```kotlin
// commonMain
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liquidmorphism.glass.GlassBackdropProvider
import com.liquidmorphism.glass.drawBackdrop
import com.liquidmorphism.glass.glassmorphism
import com.liquidmorphism.glass.parentGlass
import com.liquidmorphism.glass.rememberGlassBackdrop

@Composable
fun SharedGlassScreen() {
    val backdrop = rememberGlassBackdrop()

    GlassBackdropProvider(backdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .parentGlass()
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .drawBackdrop(
                        shape = { RoundedCornerShape(32.dp) },
                        effects = {
                            blur(16f)
                            scale(0.55f)
                            centerDistortion(0.35f)
                            elevation(12.dp)
                            tint(Color.White.copy(alpha = 0.10f))
                            darkness(0.08f)
                            warpEdges(0.25f)
                        }
                    )
            )

            Box(
                modifier = Modifier
                    .size(260.dp, 180.dp)
                    .glassmorphism(
                        shape = { RoundedCornerShape(32.dp) },
                        effects = {
                            blur(28f)
                            elevation(18.dp)
                            tint(Color.White.copy(alpha = 0.18f))
                            frosted(enabled = true, intensity = 0.55f)
                        }
                    )
            )
        }
    }
}
```

Android uses the native shader effect on Android 13+ and falls back to Compose drawing on older Android versions. iOS uses the Compose fallback implementation.

### Liquid Glass

Use `Modifier.parentGlass()` or `Modifier.layerBackdrop()` on the parent container that should provide the backdrop, then use `Modifier.drawBackdrop()` on each glass layer.

```kotlin
val backdrop = rememberGlassBackdrop()

GlassBackdropProvider(backdrop) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .parentGlass()
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .drawBackdrop(
                    shape = { RoundedCornerShape(32.dp) },
                    effects = {
                        blur(16f)
                        scale(0.55f)
                        centerDistortion(0.35f)
                        elevation(12.dp)
                        tint(Color.White.copy(alpha = 0.10f))
                        darkness(0.08f)
                        warpEdges(0.25f)
                    }
                )
        )
    }
}
```

Liquid Glass extension functions:

```kotlin
Modifier.parentGlass()
Modifier.parentGlass(backdrop)
Modifier.layerBackdrop()
Modifier.layerBackdrop(backdrop)
Modifier.drawBackdrop(shape = { RoundedCornerShape(32.dp) })
Modifier.drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(32.dp) })
```

Liquid Glass effect controls:

```kotlin
blur(radiusPx)
scale(value)
centerDistortion(value)
elevation(value)
tint(value)
darkness(value)
warpEdges(value)
```

### Glassmorphism

Use `Modifier.glassmorphism()` when you want a frosted glass card-style effect.

```kotlin
Box(
    modifier = Modifier
        .size(260.dp, 180.dp)
        .glassmorphism(
            shape = { RoundedCornerShape(32.dp) },
            effects = {
                blur(28f)
                elevation(18.dp)
                tint(Color.White.copy(alpha = 0.18f))
                borderAlpha(0.38f)
                highlightAlpha(0.34f)
                shadowAlpha(0.28f)
                frosted(enabled = true, intensity = 0.55f)
            }
        )
)
```

Glassmorphism extension function:

```kotlin
Modifier.glassmorphism(shape = { RoundedCornerShape(32.dp) })
```

Glassmorphism effect controls:

```kotlin
blur(radiusPx)
elevation(value)
tint(value)
borderAlpha(value)
highlightAlpha(value)
shadowAlpha(value)
frosted(enabled, intensity)
```
