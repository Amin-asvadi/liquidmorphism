# Liquidmorphism

Liquidmorphism is a Jetpack Compose glass and liquid glass effects library.

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
    implementation("com.github.Amin-asvadi:liquidmorphism:v1.0.0")
}
```

## Usage

```kotlin
val backdrop = rememberGlassBackdrop()

GlassBackdropProvider(backdrop) {
    // Your Compose UI
}
```
