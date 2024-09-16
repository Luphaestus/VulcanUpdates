plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    //id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    //id("com.google.devtools.ksp")
}



val releaseStoreFile: String? by rootProject
val releaseStorePassword: String? by rootProject
val releaseKeyAlias: String? by rootProject
val releaseKeyPassword: String? by rootProject


android {
    namespace = "com.vulcanizer.updates"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vulcanizer.updates"
        minSdk = 33
        targetSdk = 34
        versionCode = 3
        versionName = "2.5.6"
        resourceConfigurations += listOf("en")


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        create("release") {
            releaseStoreFile?.also {
                storeFile = rootProject.file(it)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        all {
            signingConfig =
                if (releaseStoreFile.isNullOrEmpty()) {
                    signingConfigs.getByName("debug")
                } else {
                    signingConfigs.getByName("release")
                }
        }

        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false

        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

configurations.configureEach {
    exclude("androidx.appcompat", "appcompat")
    exclude("androidx.fragment", "fragment")
    exclude("androidx.core",  "core")
    exclude("androidx.core",  "core-ktx")
    exclude("androidx.customview",  "customview")
    exclude("androidx.viewpager",  "viewpager")
    exclude("androidx.drawerlayout",  "drawerlayout")
    exclude("androidx.viewpager",  "viewpager")
    exclude("androidx.viewpager2",  "viewpager2")
    exclude("androidx.coordinatorlayout",  "coordinatorlayout")
    exclude("androidx.recyclerview",  "recyclerview")
    exclude("com.google.android.material",  "material")

}

dependencies {
    implementation("io.github.oneuiproject:design:1.2.6")

    implementation("io.github.oneuiproject.sesl:appcompat:1.4.0")
    implementation("io.github.oneuiproject.sesl:preference:1.1.0")
    implementation("io.github.oneuiproject.sesl:recyclerview:1.4.1")
    implementation("io.github.oneuiproject.sesl:swiperefreshlayout:1.0.0")
    implementation("io.github.oneuiproject.sesl:viewpager:1.1.0")
    implementation("io.github.oneuiproject.sesl:viewpager2:1.1.0")

    implementation("io.github.oneuiproject.sesl:material:1.5.0")

    implementation("io.github.oneuiproject.sesl:apppickerview:1.0.0")
    implementation("io.github.oneuiproject.sesl:indexscroll:1.0.3")
    implementation("io.github.oneuiproject.sesl:picker-basic:1.2.0")
    implementation("io.github.oneuiproject.sesl:picker-color:1.1.0")

    implementation("io.github.oneuiproject:icons:1.1.0")

    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.github.varungulatii:Kdownloader:1.0.4")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation ("com.github.khushpanchal:Ketch:2.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    //noinspection GradleDependency
    implementation("androidx.core:core-ktx:1.9.0")
    //noinspection GradleDependency
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation ("com.github.tajchert:WaitingDots:0.6.1")
    //noinspection GradleDependency
    implementation("com.google.dagger:hilt-android:2.42")
    //noinspection GradleDependency
   // kapt("com.google.dagger:hilt-compiler:2.42")
}
