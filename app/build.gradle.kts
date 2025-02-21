plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.chaquo.python")
    //kotlin("kapt")
}

android {
    namespace = "com.example.youtubeconverter"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.youtubeconverter"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            //abiFilters += listOf("arm64-v8a", "x86_64", "x86")
            abiFilters.add("arm64-v8a")
            //abiFilters.add("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    flavorDimensions += "pyVersion"
}

chaquopy {
    /*productFlavors {
        getByName("py312") {
            version = "3.12"
        }
    }*/
    defaultConfig {
        // Point to your Python 3.8 interpreter
        buildPython("C:/Users/dan_a/AppData/Local/Programs/Python/Python312/python.exe")
        pip {
            // Install the pytubefix package
            install("pytubefix")
        }
    }
    sourceSets {
        getByName("main") {
            srcDir("src/main/python")
        }
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
    implementation(files("libs/mobile-ffmpeg-full-4.4.LTS.aar"))
    implementation(libs.kotlinx.coroutines.android) // Add this line
    implementation(libs.androidx.navigation.compose) // Use the latest version
    implementation(libs.accompanist.insets)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.multidex)
    //implementation(libs.accompanist.navigation.animation.v270alpha01)
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // For Kotlin use kapt instead of annotationProcessor
    //kapt(libs.androidx.lifecycle.compiler)
    // ViewModel and LiveData
    //implementation(libs.androidx.lifecycle.runtime.ktx)
    //implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    //implementation(libs.androidx.runtime.livedata)
    //implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation("androidx.compose.material3:material3:1.2.1")// Check for the latest version
    implementation("androidx.compose.material:material:1.6.3")// Check for the latest version

    // Compose LiveData integration
    //implementation("androidx.compose.runtime:runtime-livedata:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}