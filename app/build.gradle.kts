plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.vyachean.workprofiletoggle"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.vyachean.workprofiletoggle"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        getByName("debug") {
            val ciDebugKeystorePath = providers.gradleProperty("ciDebugKeystorePath")
            if (ciDebugKeystorePath.isPresent) {
                storeFile = file(ciDebugKeystorePath.get())
                storePassword = "android"
                keyAlias = "AndroidDebugKey"
                keyPassword = "android"
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
