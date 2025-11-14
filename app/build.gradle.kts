plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.renim.bitirme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.renim.bitirme"
        minSdk = 24
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
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase BoM (zaten vardı, güncel olanı korudum)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")

    // ⭐ Eklenen Firebase bağımlılıkları
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-firestore:25.0.0")

    // UI – Navigation – Lifecycle
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // OpenStreetMap (osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-wms:6.1.18")

    // Preference
    implementation("androidx.preference:preference-ktx:1.2.1")
}

apply(plugin = "com.google.gms.google-services")
