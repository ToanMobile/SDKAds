@Suppress("DSL_SCOPE_VIOLATION") // Giữ lại để tránh cảnh báo của IDE
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.googleService) apply false
}
