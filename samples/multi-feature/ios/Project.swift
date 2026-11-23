import ProjectDescription

// 실행 전에 루트에서 `./gradlew :samples:multi-feature:export:ios-app:assembleAppSharedDebugXCFramework`로
// Kotlin 프레임워크를 만든다. Kotlin은 AppShared 하나뿐이고, 모든 Swift 모듈이 그것을 바라본다.
let appShared: TargetDependency = .xcframework(
    path: "../export/ios-app/build/XCFrameworks/debug/AppShared.xcframework"
)
let swiftAdapter: TargetDependency = .package(product: "RIBsKMPSwiftUI")
let deployment: DeploymentTargets = .iOS("17.0")
let swiftSettings: Settings = .settings(base: ["SWIFT_VERSION": "6.0"])

/// 피처 화면 모듈. 정적 프레임워크라 Kotlin 정적 프레임워크가 앱에 한 번만 링크된다.
func feature(_ name: String) -> Target {
    .target(
        name: "\(name)Feature",
        destinations: .iOS,
        product: .staticFramework,
        bundleId: "com.carti.ribs.samples.multi.\(name.lowercased())",
        deploymentTargets: deployment,
        sources: ["Features/\(name)/**"],
        dependencies: [appShared, swiftAdapter],
        settings: swiftSettings
    )
}

let project = Project(
    name: "MultiFeatureSample",
    packages: [
        .local(path: "../../../swift/RIBsKMPSwiftUI"),
    ],
    targets: [
        feature("Login"),
        feature("Signup"),
        feature("Todo"),
        feature("Settings"),
        .target(
            name: "MultiFeatureSample",
            destinations: .iOS,
            product: .app,
            bundleId: "com.carti.ribs.samples.multi",
            deploymentTargets: deployment,
            infoPlist: .extendingDefault(with: ["UILaunchScreen": [:]]),
            sources: ["App/**"],
            dependencies: [
                appShared,
                swiftAdapter,
                .target(name: "LoginFeature"),
                .target(name: "SignupFeature"),
                .target(name: "TodoFeature"),
                .target(name: "SettingsFeature"),
            ],
            settings: swiftSettings
        ),
        // 로그인 화면만 띄우는 데모 앱. LoginFeature만 빌드하므로 다른 피처를 고쳐도 다시 빌드하지 않는다.
        .target(
            name: "LoginDemo",
            destinations: .iOS,
            product: .app,
            bundleId: "com.carti.ribs.samples.multi.logindemo",
            deploymentTargets: deployment,
            infoPlist: .extendingDefault(with: ["UILaunchScreen": [:]]),
            sources: ["LoginDemo/**"],
            dependencies: [appShared, swiftAdapter, .target(name: "LoginFeature")],
            settings: swiftSettings
        ),
        .target(
            name: "MultiFeatureSampleUITests",
            destinations: .iOS,
            product: .uiTests,
            bundleId: "com.carti.ribs.samples.multi.uitests",
            deploymentTargets: deployment,
            sources: ["UITests/**"],
            dependencies: [.target(name: "MultiFeatureSample")]
        ),
    ]
)
