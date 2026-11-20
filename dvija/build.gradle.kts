import java.util.Properties

// Madhva Dvija: the practice app (dashboard, Sandhyavandana, Nitya Karma, Japa, Nitya Puja, profile, reminders).
// Its own application ID, data and release signing. No internet permission: Panchanga falls back to local calculation.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}


val uploadProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    // Namespace matches the copied source package during the transition (see docs/split/AUDIT.md, Phase 3).
    namespace = "com.paperdesk.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.hcworks.madhvadvija"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("upload") {
            if (uploadProps.containsKey("storeFile")) {
                storeFile = file(uploadProps.getProperty("storeFile"))
                storePassword = uploadProps.getProperty("storePassword")
                keyAlias = uploadProps.getProperty("keyAlias")
                keyPassword = uploadProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (uploadProps.containsKey("storeFile")) signingConfig = signingConfigs.getByName("upload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation(project(":shared:core"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
