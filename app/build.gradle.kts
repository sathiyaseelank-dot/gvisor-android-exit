plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.celzero"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.vpnapp"
        minSdk = 24
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    
    lint {
        disable += listOf("ProtectedPermissions", "ForegroundServicePermission")
    }
    
    packaging {
        jniLibs {
            pickFirsts.add("**/libgojni.so")
        }
        resources {
            pickFirsts.addAll(listOf(
                "**/go.class",
                "**/go/*.class",
                "**/go/Seq.class",
                "**/go/Universe.class", 
                "**/go/error.class"
            ))
            excludes.addAll(listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            ))
        }
    }
}

dependencies {
//    implementation(files("libs/govpn.aar"))  // Temporarily disabled due to duplicate classes
    implementation(files("libs/tun2socks.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    configurations.all {
        resolutionStrategy {
            preferProjectModules()
        }
    }
}