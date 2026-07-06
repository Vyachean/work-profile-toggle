import org.gradle.api.GradleException

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
        versionCode = 4
        versionName = "0.1.3"
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

        create("release") {
            val releaseKeystorePath = providers.gradleProperty("releaseKeystorePath")
            if (releaseKeystorePath.isPresent) {
                storeFile = file(releaseKeystorePath.get())
                storePassword = requiredGradleProperty("releaseKeystorePassword")
                keyAlias = requiredGradleProperty("releaseKeyAlias")
                keyPassword = requiredGradleProperty("releaseKeyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (providers.gradleProperty("releaseKeystorePath").isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

fun requiredGradleProperty(name: String): String {
    return providers.gradleProperty(name).orNull
        ?: throw GradleException("Gradle property '$name' is required for release signing.")
}
