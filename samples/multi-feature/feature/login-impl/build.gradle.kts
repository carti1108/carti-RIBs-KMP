plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvm()
    android {
        namespace = "com.carti.ribs.samples.multi.login.impl"
        compileSdk = 37
        minSdk = 24
    }
    js {
        nodejs()
        browser()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":samples:multi-feature:feature:login-api"))
        }
    }
}
