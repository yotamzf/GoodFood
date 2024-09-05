plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
//    id("androidx.navigation.safeargs.kotlin")
//    id("kotlin-kapt")
}


android {
    namespace = "com.example.goodfoodapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.goodfoodapp"
        minSdk = 26
        targetSdk = 34
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
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth")
//    implementation("androidx.legacy:legacy-support-v4:1.0.0")
//    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
//    implementation("androidx.fragment:fragment-ktx:1.5.6")
////    kapt("androidx.room:room-compiler:2.6.1")
//    implementation("androidx.room:room-ktx:2.6.1")
//    implementation("com.squareup.retrofit2:retrofit:2.11.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
//    implementation("androidx.navigation:navigation-fragment-ktx:laster_version")
//    implementation("androidx.navigation:navigation-ui-ktx:laster_version")
}

//kapt {
//    correctErrorTypes = true
//}