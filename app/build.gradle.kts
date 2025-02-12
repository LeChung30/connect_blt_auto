plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.connect_blt_auto"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.connect_blt_auto"
        minSdk = 29
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

    packaging {
        resources.excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation.jvm)
    implementation(libs.identity.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation ("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation ("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation ("org.bouncycastle:bcutil-jdk18on:1.78.1")
}