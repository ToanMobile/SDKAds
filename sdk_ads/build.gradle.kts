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
            isMinifyEnabled = true
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
                //val variantName = project.name
                // from(components[variantName])
                from(components.findByName("release"))
                groupId = "com.magic.sdk"
                artifactId = "AdsSdk"
                version = "v1.0.7"
            }
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.ads.identifier)
    api(libs.play.services.ads.lite)
    implementation(libs.ads.gdpr)
    implementation(libs.firebase.ads)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.process)
}
