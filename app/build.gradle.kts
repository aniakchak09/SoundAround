import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.0"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

android {
    namespace = "licenta.soundaround"
    compileSdk = 36

    defaultConfig {
        applicationId = "licenta.soundaround"
        minSdk = 24
        targetSdk = 36
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
            buildConfigField("String", "LASTFM_API_KEY", "\"${localProperties.getProperty("LASTFM_API_KEY")}\"")
            buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY")}\"")
        }

        debug {
            buildConfigField("String", "LASTFM_API_KEY", "\"${localProperties.getProperty("LASTFM_API_KEY")}\"")
            buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY")}\"")
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
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.camera.camera2.pipe)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.2"))

    // Auth module (This is what you need for 'Email' and 'signUpWith')
    implementation("io.github.jan-tennert.supabase:auth-kt")

    // Postgrest module (This is what you need for '.from("profiles").upsert()')
    implementation("io.github.jan-tennert.supabase:postgrest-kt")

    // Ktor Client (Supabase needs an engine to actually make the network calls)
    implementation("io.ktor:ktor-client-android:3.0.0")

    // Retrofit for Last.fm API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coil for loading Album Art images
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Kotlin Serialization for JSON parsing (if needed)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Multiplatform Settings (needed for Supabase session persistence)
    implementation("com.russhwolf:multiplatform-settings:1.1.1")
    implementation("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
}