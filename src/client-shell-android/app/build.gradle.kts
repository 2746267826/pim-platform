plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ciKeystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
val ciKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val ciKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val ciKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasCiSigning = listOf(
    ciKeystoreFile,
    ciKeystorePassword,
    ciKeyAlias,
    ciKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.pim.shell"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.pim.shell"
        minSdk = 26
        targetSdk = 34
        versionCode = System.getenv("CI_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("CI_APP_VERSION") ?: "0.0.0(local)"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        if (hasCiSigning) {
            create("ci") {
                storeFile = file(ciKeystoreFile!!)
                storePassword = ciKeystorePassword
                keyAlias = ciKeyAlias
                keyPassword = ciKeyPassword
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasCiSigning) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        debug { isMinifyEnabled = false }
    }
    buildFeatures { buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")
}
