@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    id("maven-publish")
}

android {
    namespace = "com.sdk.ads"
    compileSdk = 36
    //noinspection DataBindingWithoutKapt
    buildFeatures.dataBinding = true
    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFile("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

afterEvaluate {
    publishing {
        repositories {
            mavenLocal()
        }
        publications {
            create<MavenPublication>("release") {
                from(components.findByName("release"))
                groupId = "com.magic.sdk"
                artifactId = "AdsSdk"
                version = "v2.5.0"
            }
        }
    }
}

dependencies {
    //Ads
    api(libs.ads.billing)
    api(libs.ads.identifier)
    api(libs.ads.services.lite)
    api(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.ads.gdpr)
    implementation(libs.ads.firebase.ads)
    //Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.preference)
    implementation(libs.material)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.shimmer)
    //implementation(libs.utilCore)
}
