plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "moe.lsgtky.leafisland.hidden_api"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
