plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.hacybeyker.cartera.legacy"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        // `app` tiene debug, release y demo; el módulo necesita los mismos tipos para que cada variante de `app`
        // resuelva su par sin reglas de reemplazo.
        create("demo") {
            initWith(getByName("release"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        lintConfig = file("$rootDir/lint.xml")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.retrofit.adapter.rxjava3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    // Roborazzi lo usa por dentro al capturar Views; la 3.5.1 que llega transitiva falla en la API 37 (llama a
    // InputManager.getInstance(), que ya no existe).
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
}

detekt {
    source.setFrom(
        "src/main/java",
        "src/test/java"
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
                    "com.hacybeyker.cartera.legacy.integration.*",
                    "com.hacybeyker.cartera.legacy.*.domain.*",
                    "com.hacybeyker.cartera.legacy.*.data.*",
                    "com.hacybeyker.cartera.legacy.*Presenter*"
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
                    "*_HiltModules*"
                )
            }
        }
        verify {
            rule("Line coverage of measured classes (integration, domain, data and Presenters)") {
                minBound(90)
            }
        }
    }
}
