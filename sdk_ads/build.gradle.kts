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
    compileSdk = 33
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
                val variantName = project.name
                //from(components[variantName])
                from(components.findByName("release"))
                groupId = "com.magic.sdk"
                artifactId = "AdsSdk"
                version = "v1.0.1"
            }
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.android.gms:play-services-ads:22.2.0")
    api("com.google.android.gms:play-services-ads-lite:22.2.0")
    implementation("com.google.firebase:firebase-ads:22.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-process:2.6.1")
}

