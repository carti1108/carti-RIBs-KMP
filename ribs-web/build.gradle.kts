plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// 브라우저(React) 어댑터. iOS의 swift/RIBsKMPSwiftUI, Android의 ribs-compose에 해당한다(ADR-0009).
kotlin {
    explicitApi()

    js {
        browser {
            testTask { useKarma { useChromeHeadless() } }
        }
        nodejs()
        binaries.library()
        generateTypeScriptDefinitions()
        useEsModules()
    }

    sourceSets {
        jsMain.dependencies {
            api(project(":ribs-core"))
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
