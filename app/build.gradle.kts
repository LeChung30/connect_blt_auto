import org.apache.tools.ant.util.JavaEnvUtils.VERSION_1_8
import org.gradle.api.JavaVersion.VERSION_1_8

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.connect_blt_auto"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.connect_blt_auto"
        minSdk = 26
        targetSdk = 29 // Đảm bảo tương thích Android 10
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.3.2") // Giữ phiên bản tương thích Android 10
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.2.0") // Phiên bản cũ phù hợp Android 10
    implementation ("com.google.android.material:material:1.9.0")
    implementation(libs.games.activity)
    implementation(libs.monitor)
    implementation(libs.ext.junit)
    testImplementation(libs.junit.junit)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.testng)
}
