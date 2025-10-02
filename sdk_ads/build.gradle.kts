@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    id("maven-publish")
}

android {
    namespace = "com.sdk.ads"
    compileSdk = 36
    buildFeatures.dataBinding = true
    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFile("proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    lint {
        checkReleaseBuilds = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
                groupId = "com.magic.sdk"
                artifactId = "AdsSdk"
                version = "v2.5.1"
            }
        }
        repositories {
            mavenLocal()
        }
    }
}

dependencies {
    //Ads
    api(libs.ads.billing)
    api(libs.ads.identifier)
    api(libs.ads.google)
    api(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.ads.gdpr)
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
}