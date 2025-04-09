plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin") // Safe Args plugin HERE

}

android {
    namespace = "com.mints.mobilehealthapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mints.mobilehealthapplication"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true

    }



    buildTypes {
        debug {
            buildConfigField("String", "NHS_API_KEY", "\"${project.properties["NHS_API_KEY"] ?: ""}\"")
        }
        release {
            buildConfigField("String", "NHS_API_KEY", "\"${project.properties["NHS_API_KEY"] ?: ""}\"")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.junit.junit)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation (libs.material)
    implementation(libs.shimmer)
    implementation(libs.play.services.auth)
    implementation (libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.mpandroidchart)
    testImplementation (libs.mockito.mockito.core)
    testImplementation (libs.kotlin.mockito.kotlin)
    testImplementation (libs.jetbrains.kotlinx.coroutines.test)
    testImplementation (libs.androidx.core.testing)
    testImplementation (libs.robolectric)
    testImplementation (libs.mockk)
    testImplementation (libs.kotlinx.coroutines.test.v173)
    testImplementation (libs.androidx.work.testing)

}