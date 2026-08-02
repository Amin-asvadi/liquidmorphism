// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "Liquidmorphism",
    platforms: [
        .iOS(.v15),
        .macOS(.v12),
    ],
    products: [
        .library(
            name: "LiquidmorphismSwiftUI",
            targets: ["LiquidmorphismSwiftUI"],
        ),
    ],
    targets: [
        .target(
            name: "LiquidmorphismSwiftUI",
            path: "Sources/LiquidmorphismSwiftUI",
        ),
    ],
)
