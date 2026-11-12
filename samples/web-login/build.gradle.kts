plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// 로그인 샘플의 웹 뷰 구현과 진입점. React 앱(react/)이 빌드 결과를 가져다 쓴다(ADR-0009).
kotlin {
    js {
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        useEsModules()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":samples:login-shared"))
            implementation(project(":ribs-web"))
        }
    }
}
