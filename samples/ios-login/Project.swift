import ProjectDescription

// 실행 전에 루트에서 `./gradlew :samples:login-shared:assembleLoginSharedDebugXCFramework`로
// Kotlin 프레임워크를 만든다.
let project = Project(
    name: "LoginSample",
    packages: [
        .local(path: "../../swift/RIBsKMPSwiftUI"),
    ],
    targets: [
        .target(
            name: "LoginSample",
            destinations: .iOS,
            product: .app,
            bundleId: "com.carti.ribs.samples.login",
            deploymentTargets: .iOS("17.0"),
            infoPlist: .extendingDefault(with: ["UILaunchScreen": [:]]),
            sources: ["Sources/**"],
            dependencies: [
                .xcframework(path: "../login-shared/build/XCFrameworks/debug/LoginShared.xcframework"),
                .package(product: "RIBsKMPSwiftUI"),
            ],
            settings: .settings(base: ["SWIFT_VERSION": "6.0"])
        ),
        .target(
            name: "LoginSampleUITests",
            destinations: .iOS,
            product: .uiTests,
            bundleId: "com.carti.ribs.samples.login.uitests",
            deploymentTargets: .iOS("17.0"),
            sources: ["UITests/**"],
            dependencies: [.target(name: "LoginSample")]
        ),
    ]
)
