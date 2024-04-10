plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.bettermoodle"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.example.bettermoodle"
        minSdk = 29
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("com.github.thellmund.Android-Week-View:core:5.3.2")
    implementation("com.github.thellmund.Android-Week-View:jsr310:5.3.2")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1") // or earlier versions
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.6.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

}