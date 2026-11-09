plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    explicitApi()

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
