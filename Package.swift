// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorCamera",
    platforms: [.iOS(.v16)],
    products: [
        .library(
            name: "CapacitorCamera",
            targets: ["CameraPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor.git", from: "9.0.0-alpha.7"),
        .package(url: "https://github.com/ionic-team/ion-ios-camera.git", from: "1.0.5")
    ],
    targets: [
        .target(
            name: "CameraPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor"),
                .product(name: "IONCameraLib", package: "ion-ios-camera")
            ],
            path: "ios/Sources/CameraPlugin"),
        .testTarget(
            name: "CameraPluginTests",
            dependencies: ["CameraPlugin"],
            path: "ios/Tests/CameraPluginTests")
    ]
)
