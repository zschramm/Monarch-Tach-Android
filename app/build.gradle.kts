plugins { id("com.android.application") }
android {
 namespace = "com.lamalo.monarch"
 compileSdk = 35
 defaultConfig { applicationId = "com.lamalo.monarch"; minSdk = 26; targetSdk = 35; versionCode = 6; versionName = "0.2.4" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("com.github.mik3y:usb-serial-for-android:3.9.0"); testImplementation("junit:junit:4.13.2") }
dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.20") }
dependencies { implementation("androidx.core:core:1.13.1") }


