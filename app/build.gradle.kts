import java.io.File
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val androidSigningEnv = loadAndroidSigningEnv(rootProject.file(".env.android-signing"))
val localProps = loadLocalProperties(rootProject.file("local.properties"))
val debugGoogleWebClientId = localProps["GOOGLE_WEB_CLIENT_ID"].orEmpty()
val releaseGoogleWebClientId = System.getenv("GOOGLE_WEB_CLIENT_ID")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: debugGoogleWebClientId

fun signingValue(name: String): String? = System.getenv(name) ?: androidSigningEnv[name]

val releaseKeystoreBase64 = signingValue("ANDROID_KEYSTORE_BASE64")
val releaseKeystorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("ANDROID_KEY_ALIAS")
val releaseKeyPassword = signingValue("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystoreBase64,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    val buildVersionName = System.getenv("FITNS_VERSION_NAME")
        ?.removePrefix("v")
        ?.takeIf { it.matches(Regex("\\d+\\.\\d+\\.\\d+")) }
        ?: "0.0.1"
    val buildVersionCode = System.getenv("FITNS_VERSION_CODE")
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
        ?: 1

    namespace = "com.raysix.fitns"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raysix.fitns"
        minSdk = 26
        targetSdk = 36
        versionCode = buildVersionCode
        versionName = buildVersionName

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            asBuildConfigString(debugGoogleWebClientId)
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val keystoreFile = layout.buildDirectory.file("generated/signing/release.jks").get().asFile
                keystoreFile.parentFile.mkdirs()
                keystoreFile.writeBytes(Base64.getDecoder().decode(releaseKeystoreBase64!!))

                storeFile = keystoreFile
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField(
                "String",
                "GOOGLE_WEB_CLIENT_ID",
                asBuildConfigString(releaseGoogleWebClientId)
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}

val releaseArtifactTaskNames = setOf(
    "assembleRelease",
    "bundleRelease",
    "packageRelease",
    "packageReleaseBundle",
    "signReleaseBundle"
)

gradle.taskGraph.whenReady {
    val buildsReleaseArtifact = allTasks.any { task ->
        task.project == project && task.name in releaseArtifactTaskNames
    }
    if (buildsReleaseArtifact) {
        val missingConfiguration = buildList {
            if (releaseKeystoreBase64.isNullOrBlank()) add("ANDROID_KEYSTORE_BASE64")
            if (releaseKeystorePassword.isNullOrBlank()) add("ANDROID_KEYSTORE_PASSWORD")
            if (releaseKeyAlias.isNullOrBlank()) add("ANDROID_KEY_ALIAS")
            if (releaseKeyPassword.isNullOrBlank()) add("ANDROID_KEY_PASSWORD")
        }
        if (missingConfiguration.isNotEmpty()) {
            throw GradleException(
                "Release build configuration is incomplete. Missing: " +
                    missingConfiguration.joinToString()
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("com.google.dagger:hilt-android:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:2.52")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

fun asBuildConfigString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
}

fun loadAndroidSigningEnv(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val key = line.substringBefore("=").trim()
            val value = line.substringAfter("=").trim().trim('"').trim('\'')
            key to value
        }
}

fun loadLocalProperties(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val key = line.substringBefore("=").trim()
            val value = line.substringAfter("=").trim().trim('"').trim('\'')
            key to value
        }
}
