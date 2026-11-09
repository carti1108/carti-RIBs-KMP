plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }

    jvm()
    js {
        nodejs()
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

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
