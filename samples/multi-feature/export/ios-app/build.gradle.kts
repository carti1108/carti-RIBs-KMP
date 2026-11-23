import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

/** iOS 앱이 `import AppShared`로 쓰는 프레임워크 이름. */
val APPLE_FRAMEWORK_NAME = "AppShared"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// 코드 없이 export 설정만 둔다. 앱에는 Kotlin 프레임워크를 하나만 넣을 수 있으므로
// Swift가 다루는 모듈(ribs-core, 루트, 피처 -api)을 이 프레임워크 하나로 묶는다. -impl은 export하지 않는다.
val exported = listOf(
    ":ribs-core",
    ":samples:multi-feature:app-root",
    ":samples:multi-feature:feature:login-api",
    ":samples:multi-feature:feature:signup-api",
    ":samples:multi-feature:feature:todo-api",
    ":samples:multi-feature:feature:settings-api",
)

kotlin {
    val xcFramework = XCFramework(APPLE_FRAMEWORK_NAME)
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = APPLE_FRAMEWORK_NAME
            isStatic = true
            exported.forEach { export(project(it)) }
            xcFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            exported.forEach { api(project(it)) }
        }
    }
}
