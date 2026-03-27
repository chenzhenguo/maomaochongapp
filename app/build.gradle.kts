plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.kapt")
}

android {
  namespace = "com.maomaochongapp"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.maomaochongapp"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Enable test options
    testOptions {
      unitTests {
        isIncludeAndroidResources = true
        isReturnDefaultValues = true
      }
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
      val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
      val keyAlias = System.getenv("KEY_ALIAS")
      val keyPassword = System.getenv("KEY_PASSWORD")
      if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
        storeFile = file(keystorePath)
        storePassword = keystorePassword
        this.keyAlias = keyAlias
        this.keyPassword = keyPassword
      } else {
        // Fall back to debug signing when secrets are not available (local dev)
        val debugConfig = signingConfigs.getByName("debug")
        storeFile = debugConfig.storeFile
        storePassword = debugConfig.storePassword
        this.keyAlias = debugConfig.keyAlias
        this.keyPassword = debugConfig.keyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
      signingConfig = signingConfigs.getByName("release")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
    freeCompilerArgs += listOf(
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
      "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
    )
  }

  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
  implementation("androidx.documentfile:documentfile:1.0.1")

  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.foundation:foundation")
  implementation("com.google.android.material:material:1.12.0")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  // Unit test dependencies
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.12.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
  testImplementation("io.mockk:mockk:1.13.10")
  testImplementation("app.cash.turbine:turbine:1.1.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
  testImplementation("androidx.arch.core:core-testing:2.2.0")
  testImplementation("com.google.truth:truth:1.4.2")
  testImplementation("org.robolectric:robolectric:4.11.1")

  // Android test dependencies
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  androidTestImplementation("androidx.test:core-ktx:1.6.1")
  androidTestImplementation("androidx.test:runner:1.6.1")
  androidTestImplementation("androidx.test:rules:1.6.1")
  androidTestImplementation("org.robolectric:annotations:4.11.1")
  androidTestImplementation("androidx.room:room-testing:2.6.1")
  androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
  androidTestImplementation("io.mockk:mockk-android:1.13.10")
  androidTestImplementation("com.google.truth:truth:1.4.2")
  androidTestImplementation("app.cash.turbine:turbine:1.1.0")

  // Room database for picture book persistence
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  kapt("androidx.room:room-compiler:2.6.1")

  // Coil for image loading
  implementation("io.coil-kt:coil-compose:2.6.0")
  implementation("io.coil-kt:coil-gif:2.6.0")
}

