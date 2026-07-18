plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
    kotlin("plugin.serialization")
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
    namespace = "com.pim.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.pim.app"
        minSdk = 26
        targetSdk = 34
        versionCode = System.getenv("CI_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("CI_APP_VERSION") ?: "0.0.0(local)"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
    kotlinOptions {
        jvmTarget = "17"
    }
    sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
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
        getByName("debug") {
            if (hasCiSigning) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        getByName("release") {
            if (hasCiSigning) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
    }
}

kapt {
    correctErrorTypes = true
    javacOptions {
        // Ensure kapt has access to internal javac APIs for Dagger/Hilt annotation processing
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED")
        option("-J--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":features:calendar"))

    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Custom PimWorkerFactory replaces @HiltWorker/HiltWorkerFactory — no androidx.hilt:hilt-work needed

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    implementation("androidx.webkit:webkit:1.12.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}
