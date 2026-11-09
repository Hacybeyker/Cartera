plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

// Solo debug: `-Pcartera.baseUrl=http://10.0.2.2:4010/` (o esa misma línea en ~/.gradle/gradle.properties) apunta la
// app a otro servidor, por ejemplo un mock del contrato en la máquina anfitriona. Demo y release no la leen.
val debugBaseUrl: String =
    providers.gradleProperty("cartera.baseUrl").orNull?.trim()?.also { url ->
        if (!Regex("""https?://[^\s"\\]+/""").matches(url)) {
            throw GradleException(
                "cartera.baseUrl debe ser una URL http(s) que termine en '/' (por ejemplo http://10.0.2.2:4010/); " +
                    "se recibió '$url'."
            )
        }
    } ?: "http://127.0.0.1:8765/api/v1/"

android {
    namespace = "com.hacybeyker.cartera"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hacybeyker.cartera"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = libs.versions.appVersion.get()
        buildConfigField("String", "BASE_URL", "\"https://cartera.invalid/api/v1/\"")
        buildConfigField("long", "HTTP_TIMEOUT_SECONDS", "10L")
        // Interruptores del cliente: false en todas las variantes. En debug y demo los cambia el escenario en tiempo
        // de ejecución; release no tiene cómo, así que quedan apagados.
        listOf(
            "MOVEMENTS_FULL_PAGE",
            "ACCOUNTS_EAGER_REFRESH",
            "BREAKDOWN_RICH_TRANSITION",
            "QUOTES_FULL_REFRESH",
            "CHART_LIVE_PATH"
        ).forEach { flag ->
            buildConfigField("boolean", flag, "false")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            optimization {
                enable = true
            }
        }
        create("demo") {
            initWith(getByName("release"))
            isProfileable = true
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8765/api/v1/\"")
        }
    }
    sourceSets {
        listOf("debug", "demo").forEach { buildType ->
            getByName(buildType) {
                kotlin.directories.add("src/demoTools/java")
                res.directories.add("src/demoTools/res")
                assets.directories.add("$rootDir/api/fixtures")
                manifest.srcFile("src/demoTools/AndroidManifest.xml")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        lintConfig = file("$rootDir/lint.xml")
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    implementation(project(":legacy"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.mockwebserver3)
    "demoImplementation"(libs.okhttp.mockwebserver3)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    annotationProcessor(libs.kotlin.metadata.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.openapi.request.validator.core)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // No es para escribir tests de Espresso: compose-ui-test lo usa por dentro con Robolectric, y la
    // 3.5.1 que trae transitiva falla en la API 37 (llama a InputManager.getInstance(), que ya no existe).
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<Test>().configureEach {
    systemProperty("cartera.fixturesDir", "$rootDir/api/fixtures")
    systemProperty("cartera.rootDir", "$rootDir")
    // Los tests leen estos archivos por ruta: sin declararlos, Gradle daría por buena una corrida vieja.
    inputs.dir("$rootDir/api").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("src") { include("**/res/xml/network_security_config.xml") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

detekt {
    source.setFrom(
        "src/main/java",
        "src/test/java",
        "src/demoTools/java"
    )
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

kover {
    reports {
        filters {
            includes {
                classes(
                    "com.hacybeyker.cartera.*.domain.*",
                    "com.hacybeyker.cartera.*.data.*",
                    "com.hacybeyker.cartera.*ViewModel*",
                    "com.hacybeyker.cartera.demo.backend.*",
                    "com.hacybeyker.cartera.demo.scenario.*"
                )
            }
            excludes {
                classes(
                    "*_Impl",
                    "*_Impl$*",
                    "*_Factory",
                    "*_Factory$*",
                    "*Module",
                    "*Module$*",
                    "*Module_*",
                    "*_HiltModules*",
                    // Keystore, BiometricPrompt y BiometricManager: dependen del hardware y se verifican a mano.
                    "com.hacybeyker.cartera.core.data.biometric.device.*"
                )
            }
        }
        verify {
            rule("Line coverage of measured classes (domain, data, ViewModels, demo backend and scenario)") {
                minBound(90)
            }
        }
    }
}
