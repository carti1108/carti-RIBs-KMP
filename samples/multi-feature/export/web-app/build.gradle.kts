plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// 웹 뷰 구현과 진입점을 JS 산출물 하나로 낸다. React 앱(react/)이 가져다 쓴다(ADR-0009).
kotlin {
    js {
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        useEsModules()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":samples:multi-feature:app-root"))
            implementation(project(":ribs-web"))
        }
    }
}
