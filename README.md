# Liquidmorphism

Liquidmorphism is a Jetpack Compose glass and liquid glass effects library.

## Screenshot

![Liquidmorphism demo screenshot](docs/screenshots/liquidmorphism-home.png)

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

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.Amin-asvadi:liquidmorphism:1.0.2")
}
```

## Usage

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
