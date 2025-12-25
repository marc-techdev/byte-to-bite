plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.testdesign"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.testdesign"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { viewBinding = true }

    // AGP 8+: use androidResources instead of deprecated aaptOptions
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // Version-catalog deps
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)                  // keep ONE Material dep via catalog
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // UI / utilities
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.browser:browser:1.8.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // Guava (used by some CameraX paths)
    implementation("com.google.guava:guava:31.1-android")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Media3 (pick ONE version; here we use 1.4.1 with new artifact names)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // --- DO NOT keep any of these duplicates ---
    // implementation("com.google.android.material:material:1.12.0'")
    // implementation("com.google.android.material:material:<latest>")
    // implementation("com.google.android.material:material:1.12.0")
    // implementation("androidx.media3:exoplayer:1.3.1")
    // implementation("androidx.media3:ui:1.3.1")
    // implementation("androidx.viewpager2:viewpager2:1.0.0")  // (already added above)
}
