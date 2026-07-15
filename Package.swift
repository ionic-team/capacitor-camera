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
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "9.0.0-alpha.5"),
        .package(url: "https://github.com/ionic-team/ion-ios-camera.git", from: "1.0.4")
    ],
    targets: [
        .target(
            name: "CameraPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "IONCameraLib", package: "ion-ios-camera")
            ],
            path: "ios/Sources/CameraPlugin"),
        .testTarget(
            name: "CameraPluginTests",
            dependencies: ["CameraPlugin"],
            path: "ios/Tests/CameraPluginTests")
    ]
)
