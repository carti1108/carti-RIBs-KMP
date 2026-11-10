// swift-tools-version: 6.0
import PackageDescription

// Kotlin 프레임워크에 의존하지 않는 순수 Swift 어댑터(ADR-0006).
// 앱에는 Kotlin 프레임워크를 하나만 넣을 수 있으므로, 앱의 Kotlin 타입과의 연결은 앱 쪽 코드가 맡는다.
let package = Package(
    name: "RIBsKMPSwiftUI",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "RIBsKMPSwiftUI", targets: ["RIBsKMPSwiftUI"]),
    ],
    targets: [
        .target(name: "RIBsKMPSwiftUI"),
        .testTarget(name: "RIBsKMPSwiftUITests", dependencies: ["RIBsKMPSwiftUI"]),
    ]
)
