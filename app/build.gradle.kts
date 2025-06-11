plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.myworkoutlog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myworkoutlog"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.3")
    // --- Add these lines for Room Database ---
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    // Annotation processor for Room. This is important!
    ksp("androidx.room:room-compiler:$room_version")
    // --- End of Room lines ---
    implementation("com.google.code.gson:gson:2.10.1")
    // --- ViewModel ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    // --- For collecting Flows in a lifecycle-aware manner ---
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // --- Charting Library (Vico) ---
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")
    // --- For User Preferences ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}