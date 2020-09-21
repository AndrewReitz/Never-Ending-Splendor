plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

    id("phishin-api-key-provider")
    id("signing-config")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")

    signingConfigs {
        val keystoreLocation: String by project
        val keystorePassword: String by project
        val storeKeyAlias: String by project
        val aliasKeyPassword: String by project

        val debug by getting {
            storeFile = file("$rootDir/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file(keystoreLocation)
            storePassword = keystorePassword
            keyAlias = storeKeyAlias
            keyPassword = aliasKeyPassword
        }
    }

    defaultConfig {
        applicationId = "never.ending.splendor"
        minSdkVersion(23)
        targetSdkVersion(30)
        versionCode = 1 // tood auto update
        versionName = "One"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val phishinApiKey: String by project
        buildConfigField("String", "PHISHIN_API_KEY", "\"$phishinApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        val release by getting {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(project(":networking"))

    implementation("androidx.core:core-ktx:1.3.1")
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")

    implementation("com.google.android.gms:play-services-cast:19.0.0")
    implementation("com.google.android.libraries.cast.companionlibrary:ccl:2.9.1")
    implementation("com.google.android.material:material:1.2.1")

    implementation("com.google.firebase:firebase-analytics:17.5.0")
    implementation("com.google.firebase:firebase-crashlytics:17.2.1")

    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.mediarouter:mediarouter:1.1.0")

    // appears to be a bug in 1.4.10 where you can't use synchronous http anymore
    implementation("com.loopj.android:android-async-http:1.4.9")
    implementation("com.google.guava:guava:29.0-android")

    implementation("org.kodein.di:kodein-di:7.0.0")
    implementation("org.kodein.di:kodein-di-framework-android-x:7.0.0")
    implementation("org.kodein.di:kodein-di-jxinject-jvm:7.0.0")

    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.squareup.okhttp3:logging-interceptor:4.8.1")

    implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13")
    testImplementation("com.google.truth:truth:1.0.1")
    testImplementation("org.mockito:mockito-core:3.5.10")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}