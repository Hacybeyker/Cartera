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
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8765/api/v1/\"")
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
                kotlin.srcDir("src/demoTools/java")
                res.srcDir("src/demoTools/res")
                assets.srcDir("$rootDir/api/fixtures")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.maxHeapSize = "2g"
                test.systemProperty("cartera.fixturesDir", "$rootDir/api/fixtures")
                // Robolectric 4.17+ en JDK 17+ necesita abrir paquetes internos del JDK
                // (https://robolectric.org/getting-started/). Sin jdk.internal.access, el arranque del
                // entorno de SDK 36+ falla en ApplicationSharedMemory.create().
                test.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
                )
            }
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        checkReleaseBuilds = true
        lintConfig = file("$rootDir/lint.xml")
        htmlReport = true
        sarifReport = true
        textReport = true
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
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
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    annotationProcessor(libs.kotlin.metadata.jvm)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
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
    failOnNoDiscoveredTests = false
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom(
        "src/main/java",
        "src/test/java",
        "src/demoTools/java"
    )
    basePath = rootDir
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

tasks.register("codeQuality") {
    group = "verification"
    description = "Ejecuta Android Lint + ktlint + detekt en un solo comando."
    dependsOn("ktlintCheck", "detekt", "lint")
}

listOf("ktlintCheck", "detekt", "lint").forEach { check ->
    tasks.named(check) { mustRunAfter("ktlintFormat") }
}

tasks.register("formatAndAnalyze") {
    group = "verification"
    description = "Formatea el codigo (ktlintFormat) y luego verifica todo (ktlintCheck + detekt + lint)."
    dependsOn("ktlintFormat", "codeQuality")
}

val htmlReports =
    mapOf(
        "Android Lint" to "lint-results-debug.html",
        "detekt" to "detekt/detekt.html",
        "ktlint (main)" to "ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.html",
        "ktlint (test)" to "ktlint/ktlintTestSourceSetCheck/ktlintTestSourceSetCheck.html",
        "ktlint (scripts)" to "ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.html",
        "Kover (cobertura)" to "kover/htmlDebug/index.html"
    )
val reportsRoot = layout.buildDirectory.dir("reports")

tasks.register("qualityReports") {
    group = "verification"
    description = "Genera los reportes HTML de Lint, detekt, ktlint y Kover. Usa --continue si un gate falla."
    dependsOn("lint", "detekt", "ktlintCheck", "koverHtmlReportDebug")

    val root = reportsRoot
    val reports = htmlReports
    doLast {
        logger.lifecycle("\nReportes HTML:")
        reports.forEach { (name, relativePath) ->
            val file = root.get().file(relativePath).asFile
            val status = if (file.exists()) file.toURI().toString() else "(no generado)"
            logger.lifecycle("  %-20s %s".format(name, status))
        }
    }
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

sonar {
    properties {
        property("sonar.androidLint.reportPaths", "build/reports/lint-results-debug.xml")
        property("sonar.kotlin.detekt.reportPaths", "build/reports/detekt/detekt.xml")
        property(
            "sonar.kotlin.ktlint.reportPaths",
            listOf(
                "build/reports/ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.xml",
                "build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.xml",
                "build/reports/ktlint/ktlintTestSourceSetCheck/ktlintTestSourceSetCheck.xml"
            ).joinToString(",")
        )
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/reportDebug.xml")
        property("sonar.junit.reportPaths", "build/test-results/testDebugUnitTest")
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/ui/**",
                "**/navigation/**",
                "**/di/**",
                "**/*Module.kt",
                "**/MainActivity.kt",
                "**/MainApplication.kt"
            ).joinToString(",")
        )
    }
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
                    "*_HiltModules*"
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
