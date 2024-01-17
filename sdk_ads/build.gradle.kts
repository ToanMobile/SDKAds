@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.googleService)
    alias(libs.plugins.crashlytics)
    id("maven-publish")
}

android {
    namespace = "com.sdk.ads"
    compileSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

afterEvaluate {
    publishing {
        repositories {
            mavenLocal()
        }
        publications {
            create<MavenPublication>("maven") {
                // val variantName = project.name
                // from(components[variantName])
                from(components.findByName("release"))
                groupId = "com.magic.sdk"
                artifactId = "AdsSdk"
                version = "v1.1.7"
            }
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    //api("com.google.firebase:firebase-config")
    api("com.google.firebase:firebase-crashlytics-ktx")
    api(libs.ads.billing)
    api(libs.play.services.ads.lite)
    implementation(libs.ads.gdpr)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.firebase.ads)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.preference)
    implementation(libs.material)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.shimmer)
    implementation(libs.utilCore)
}
