plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "es.cargacerca.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = "CargaCercaShared"
            isStatic = true
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "CargaCercaShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.5.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.5.2")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.5.2")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
