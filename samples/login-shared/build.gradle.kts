import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

/** 샘플 iOS 앱이 `import LoginShared`로 쓰는 프레임워크 이름. ribs-core를 함께 내보낸다. */
val APPLE_FRAMEWORK_NAME = "LoginShared"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
    js {
        nodejs()
    }

    // 앱에는 Kotlin 프레임워크를 하나만 넣을 수 있으므로 ribs-core를 이 프레임워크에 export한다.
    val xcFramework = XCFramework(APPLE_FRAMEWORK_NAME)
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = APPLE_FRAMEWORK_NAME
            isStatic = true
            export(project(":ribs-core"))
            xcFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":ribs-core"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
