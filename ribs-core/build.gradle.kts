import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

/** Swift에서 `import RIBsKMP`로 쓰는 프레임워크 이름. */
val APPLE_FRAMEWORK_NAME = "RIBsKMP"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }

    jvm()
    android {
        namespace = "com.carti.ribs"
        compileSdk = 37
        minSdk = 24
        withHostTestBuilder {}
    }
    js {
        nodejs()
    }
    // Swift 연동용 xcframework (ADR-0006). `assembleRIBsKMPXCFramework`로 만든다.
    val xcFramework = XCFramework(APPLE_FRAMEWORK_NAME)
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = APPLE_FRAMEWORK_NAME
            isStatic = true
            xcFramework.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

/**
 * 공유 코드 규칙 검사 (docs/architecture.md, ADR-0004).
 * - 플랫폼·UI 툴킷 타입 import 금지
 * - expect/actual 선언 금지
 * - `!!` 금지
 */
val checkRibsRules by tasks.registering {
    group = "verification"
    description = "Checks shared-code rules: no platform imports, no expect/actual, no !!."
    val sources = fileTree("src/commonMain/kotlin") { include("**/*.kt") }
    inputs.files(sources)
    val sourceRoot = file("src/commonMain/kotlin")
    doLast {
        val rules = listOf(
            Regex("""^\s*import\s+(android|androidx|java|javax|platform|kotlinx\.browser|org\.w3c|UIKit|AppKit|SwiftUI)\.""")
                to "platform/UI type import",
            Regex("""^\s*(public\s+|internal\s+)?(expect|actual)\s""") to "expect/actual declaration",
            Regex("""!!""") to "non-null assertion (!!)",
        )
        val violations = sources.files.sorted().flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val code = line.substringBefore("//")
                rules.firstOrNull { (regex, _) -> regex.containsMatchIn(code) }
                    ?.let { (_, reason) -> "${file.relativeTo(sourceRoot)}:${index + 1}: $reason: ${line.trim()}" }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Shared-code rule violations:\n" + violations.joinToString("\n"))
        }
    }
}

tasks.named("check") { dependsOn(checkRibsRules) }
