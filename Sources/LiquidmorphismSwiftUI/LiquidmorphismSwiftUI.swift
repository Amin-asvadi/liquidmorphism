import SwiftUI

public struct LiquidGlassConfiguration {
    public var cornerRadius: CGFloat
    public var tint: Color?
    public var interactive: Bool
    public var fallback: GlassmorphismConfiguration

    public init(
        cornerRadius: CGFloat = 28,
        tint: Color? = nil,
        interactive: Bool = true,
        fallback: GlassmorphismConfiguration = .init()
    ) {
        self.cornerRadius = cornerRadius
        self.tint = tint
        self.interactive = interactive
        self.fallback = fallback
    }
}

public struct GlassmorphismConfiguration {
    public var cornerRadius: CGFloat
    public var material: Material
    public var tint: Color
    public var tintOpacity: Double
    public var strokeOpacity: Double
    public var highlightOpacity: Double
    public var shadowOpacity: Double
    public var shadowRadius: CGFloat
    public var shadowY: CGFloat
    public var frostedNoiseEnabled: Bool
    public var frostedNoiseOpacity: Double

    public init(
        cornerRadius: CGFloat = 28,
        material: Material = .ultraThinMaterial,
        tint: Color = .white,
        tintOpacity: Double = 0.18,
        strokeOpacity: Double = 0.38,
        highlightOpacity: Double = 0.34,
        shadowOpacity: Double = 0.16,
        shadowRadius: CGFloat = 18,
        shadowY: CGFloat = 10,
        frostedNoiseEnabled: Bool = false,
        frostedNoiseOpacity: Double = 0.12
    ) {
        self.cornerRadius = cornerRadius
        self.material = material
        self.tint = tint
        self.tintOpacity = tintOpacity
        self.strokeOpacity = strokeOpacity
        self.highlightOpacity = highlightOpacity
        self.shadowOpacity = shadowOpacity
        self.shadowRadius = shadowRadius
        self.shadowY = shadowY
        self.frostedNoiseEnabled = frostedNoiseEnabled
        self.frostedNoiseOpacity = frostedNoiseOpacity
    }
}

public extension View {
    @ViewBuilder
    func liquidGlass(
        _ configuration: LiquidGlassConfiguration = .init()
    ) -> some View {
        if #available(iOS 26.0, macOS 26.0, *) {
            self.glassEffect(
                .regular
                    .tint(configuration.tint)
                    .interactive(configuration.interactive),
                in: .rect(cornerRadius: configuration.cornerRadius)
            )
        } else {
            self.glassmorphism(configuration.fallback)
        }
    }

    func glassmorphism(
        _ configuration: GlassmorphismConfiguration = .init()
    ) -> some View {
        modifier(GlassmorphismModifier(configuration: configuration))
    }
}

@available(iOS 26.0, macOS 26.0, *)
public struct LiquidGlassContainer<Content: View>: View {
    private let spacing: CGFloat
    private let content: Content

    public init(
        spacing: CGFloat = 24,
        @ViewBuilder content: () -> Content
    ) {
        self.spacing = spacing
        self.content = content()
    }

    public var body: some View {
        GlassEffectContainer(spacing: spacing) {
            content
        }
    }
}

private struct GlassmorphismModifier: ViewModifier {
    let configuration: GlassmorphismConfiguration

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(
            cornerRadius: configuration.cornerRadius,
            style: .continuous
        )

        content
            .background(configuration.material, in: shape)
            .background(configuration.tint.opacity(configuration.tintOpacity), in: shape)
            .overlay(alignment: .topLeading) {
                shape
                    .fill(
                        LinearGradient(
                            colors: [
                                .white.opacity(configuration.highlightOpacity),
                                .white.opacity(configuration.highlightOpacity * 0.12),
                                .clear,
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .blendMode(.screen)
            }
            .overlay {
                if configuration.frostedNoiseEnabled {
                    FrostedNoise()
                        .opacity(configuration.frostedNoiseOpacity)
                        .clipShape(shape)
                        .allowsHitTesting(false)
                }
            }
            .overlay {
                shape.stroke(
                    LinearGradient(
                        colors: [
                            .white.opacity(configuration.strokeOpacity),
                            .white.opacity(configuration.strokeOpacity * 0.16),
                            .white.opacity(configuration.strokeOpacity * 0.42),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: 1
                )
            }
            .shadow(
                color: .black.opacity(configuration.shadowOpacity),
                radius: configuration.shadowRadius,
                x: 0,
                y: configuration.shadowY
            )
    }
}

private struct FrostedNoise: View {
    var body: some View {
        Canvas { context, size in
            let step: CGFloat = 3
            let columns = Int(size.width / step)
            let rows = Int(size.height / step)

            for x in 0...columns {
                for y in 0...rows {
                    let value = pseudoRandom(x: x, y: y)
                    guard value > 0.68 else { continue }

                    let opacity = 0.18 + Double(value) * 0.22
                    let rect = CGRect(
                        x: CGFloat(x) * step,
                        y: CGFloat(y) * step,
                        width: 1,
                        height: 1
                    )
                    context.fill(
                        Path(ellipseIn: rect),
                        with: .color(.white.opacity(opacity))
                    )
                }
            }
        }
    }

    private func pseudoRandom(x: Int, y: Int) -> CGFloat {
        let seed = UInt32((x &* 73_856_093) ^ (y &* 19_349_663))
        return CGFloat(seed % 10_000) / 10_000
    }
}
