import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    // Reads app/google-services.json (not committed), which must list both
    // com.hivend.agatha and com.hivend.agatha.debug.
    alias(libs.plugins.google.services)
}

// Local, machine-specific secrets (never committed — see .gitignore). Falls back to an
// empty key so a fresh checkout still builds; the Mapa de Nodos screen only needs a real
// key once it switches from the current static mock-up to the live Maps SDK composable.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")
// API de tablero del backend (solo lectura, sin autenticación). Se puede cambiar por máquina
// con AGATHA_API_BASE_URL en local.properties; termina en "/" como pide Retrofit.
val agathaApiBaseUrl: String =
    localProperties.getProperty("AGATHA_API_BASE_URL", "https://129.213.118.230/")

android {
    namespace = "com.hivend.agatha"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hivend.agatha"
        // Android 13 (API 33) is the pilot's minimum supported OS: it is also the first
        // version with the native Photo Picker, which HE-06 relies on for gallery access
        // without a runtime storage permission.
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "google_maps_key", mapsApiKey)
        buildConfigField("String", "AGATHA_API_BASE_URL", "\"$agathaApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Genera el locale_config a partir de res/values(-en): habilita el selector de idioma por
    // app de Android 13+. El idioma por defecto (res/values) es español, ver res/resources.properties.
    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// Room exports each schema version as JSON (committed) so migrations can be written and tested.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose BOM aligns every androidx.compose.* artifact to one tested version set.
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.navigation.compose)

    // Dependency injection (see docs/ARQUITECTURA_Y_DISENO.md — patrón Singleton + Inyección de Dependencias)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Persistence for the offline-first flow (HE-07): AgathaDatabase in data/local, one
    // subpackage per feature (see docs/ARQUITECTURA_Y_DISENO.md § 1).
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking against the AGATHA central API (HE-04, HE-07): NetworkModule in data/remote,
    // one subpackage per API (see docs/ARQUITECTURA_Y_DISENO.md § 1).
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Camera capture for field evidence (HE-06).
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Sensor node map (HE-08): com.google.maps.android:maps-compose is declared in the
    // version catalog for the future live-map integration but NOT applied yet — its current
    // release compiles against API 37 and needs AGP 9.1+ (confirmed by a failed build against
    // our AGP 8.13.2 toolchain). The Mapa de Nodos screen renders a static Canvas mock-up
    // today; see docs/ARQUITECTURA_Y_DISENO.md § "Mapa de nodos: mock-up vs. SDK real".

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.datastore.preferences)

    // Alert push notifications (HE-04) through Firebase Cloud Messaging.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
