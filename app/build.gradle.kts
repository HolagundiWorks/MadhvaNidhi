plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.paperdesk.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.hcworks.madhvanidhi"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    val uploadStoreFile = providers.gradleProperty("MADHVANIDHI_UPLOAD_STORE_FILE")
    val uploadStorePassword = providers.gradleProperty("MADHVANIDHI_UPLOAD_STORE_PASSWORD")
    val uploadKeyAlias = providers.gradleProperty("MADHVANIDHI_UPLOAD_KEY_ALIAS")
    val uploadKeyPassword = providers.gradleProperty("MADHVANIDHI_UPLOAD_KEY_PASSWORD")
    val uploadSigningReady = listOf(
        uploadStoreFile,
        uploadStorePassword,
        uploadKeyAlias,
        uploadKeyPassword
    ).all { it.isPresent }

    signingConfigs {
        if (uploadSigningReady) {
            create("upload") {
                storeFile = file(uploadStoreFile.get())
                storePassword = uploadStorePassword.get()
                keyAlias = uploadKeyAlias.get()
                keyPassword = uploadKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (uploadSigningReady) signingConfig = signingConfigs.getByName("upload")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    testImplementation("junit:junit:4.13.2")
}
