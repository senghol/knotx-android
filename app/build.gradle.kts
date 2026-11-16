import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)

    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)       // ✅ APPLY HERE
}
val secrets = Properties().apply {
    val f = rootProject.file("secrets.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String, fallback: String = "PLACEHOLDER"): String =
    secrets.getProperty(name) ?: fallback

fun prop(name: String, default: String): String =
    (project.findProperty(name) as String?) ?: default

android {
    namespace = "com.senghol.knotx"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.senghol.knotx"
        minSdk = 29
        targetSdk = 37
        versionCode = 4
        versionName = "0.8.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "facebook_app_id", secret("FACEBOOK_APP_ID", "000000000000000"))
        resValue("string", "fb_login_protocol_scheme", "fb" + secret("FACEBOOK_APP_ID", "000000000000000"))
        resValue("string", "facebook_client_token", secret("FACEBOOK_CLIENT_TOKEN"))
        resValue("string", "google_maps_api_key", secret("MAPS_API_KEY"))
    }

    flavorDimensions += "env"
    productFlavors {

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_HOST", "\"${prop("knotx.dev.baseHost", "http://10.0.2.2:3000/")}\"")
            buildConfigField("String", "API_PREFIX", "\"${prop("knotx.dev.apiPrefix", "api/")}\"")
            buildConfigField("String", "WS_URL", "\"${prop("knotx.dev.wsUrl", "ws://10.0.2.2:8080")}\"")
            buildConfigField("String", "SPRING_URL", "\"${prop("knotx.dev.springUrl", "http://10.0.2.2:8081/delivery")}\"")
        }
        create("production") {
            dimension = "env"
            buildConfigField("String", "BASE_HOST", "\"https://media.api.dootawas.com/\"")
            buildConfigField("String", "API_PREFIX", "\"node/\"")
            buildConfigField("String", "WS_URL", "\"wss://websocket.api.dootawas.com\"")
            buildConfigField("String", "SPRING_URL", "\"https://websocket.api.dootawas.com/spring/delivery\"")
        }
    }

    buildTypes {

        release {
            optimization {
                enable = true
            }
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }

        debug {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    packaging {
        resources {
            excludes += setOf(
                "**/*.dll",
                "**/*.dylib",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/{AL2.0,LGPL2.1}"
            )
        }
        jniLibs {
            excludes += "**/libsignal_jni_testing.so"
        }
    }
}


sqldelight {
    databases {
        create("SengholDB") {
            packageName.set("com.senghol.knotx.data.sqldelight")
            verifyMigrations.set(true)
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.checkout)
    implementation(libs.logging.interceptor)
    implementation(libs.integrity)


    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.swipe)
//    implementation("androidx.compose.material3:material3")
    implementation(libs.material.icons)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.libsignal.client)
    implementation(libs.libsignal.android)

    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.exifinterface)
    implementation (libs.androidx.work.runtime.ktx)
    implementation(libs.zip4j)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.libphonenumber)
    implementation(libs.retrofit)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.bcprov.jdk18on)

    implementation(libs.credential.manager)
    implementation(libs.credential.manager.play)
    implementation(libs.google.identity)
    implementation(files("libs/libwebrtc.aar"))

    implementation(libs.google.api.drive)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.auth.library)
    implementation(libs.google.api.client.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines)
//    implementation(libs.retrofit.converter.gson)
    implementation(libs.converter.gson)

    implementation(libs.androidx.emoji2.emojipicker)
//    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.android.maps.utils)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.camera.lifecycle)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)

    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)
    //todo you should remove constraint layout after update audio bubble
    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.barcode.scanning)
    implementation(libs.zxing.android.embedded)
    implementation(libs.colorpicker.compose)

    implementation(libs.jsoup) // for download preview
    implementation(libs.ccp)
    implementation(libs.review)
    implementation(libs.review.ktx)
    implementation(libs.facebook.core)
    // Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Firebase SDKs
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

}

val ensureGoogleServices = tasks.register("ensureGoogleServices") {
    description = "Copies google-services.json.example if no real google-services.json exists"
    group = "setup"
    val real = layout.projectDirectory.file("google-services.json").asFile
    val example = layout.projectDirectory.file("google-services.json.example").asFile
    doLast {
        if (!real.exists()) {
            example.copyTo(real)
            logger.warn("Using placeholder google-services.json. Push disabled.")
        }
    }
}

tasks.named("preBuild") { dependsOn(ensureGoogleServices) }


// Change schemaLocation to schemaDirectory
room {
    schemaDirectory("$projectDir/schemas")
}