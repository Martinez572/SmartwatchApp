plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.smartwatchapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.smartwatchapp"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.wear:wear-input:1.1.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0")
}