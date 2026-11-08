import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestOptions
import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.sonarqube.gradle.SonarExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.sonarqube)
}

val sonarHostUrl =
    providers.environmentVariable("SONAR_HOST_URL").orNull
        ?.takeIf { it.isNotBlank() }
        ?: "https://sonarcloud.io"

sonar {
    properties {
        property("sonar.projectKey", "com.hacybeyker.cartera")
        property("sonar.organization", "Hacybeyker")
        property("sonar.projectName", "Cartera")
        property("sonar.host.url", sonarHostUrl)
        property("sonar.projectVersion", libs.versions.appVersion.get())
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.exclusions", "**/*.webp,**/*.png,**/*.jar")
        property("sonar.qualitygate.wait", "true")
    }
}

tasks.named("sonar") {
    dependsOn(
        ":app:lint",
        ":app:detekt",
        ":app:ktlintCheck",
        ":app:testDebugUnitTest",
        ":app:koverXmlReportDebug",
        ":legacy:lint",
        ":legacy:detekt",
        ":legacy:ktlintCheck",
        ":legacy:testDebugUnitTest",
        ":legacy:koverXmlReportDebug"
    )
}

// Calidad común a :app y :legacy. Cada módulo declara sus plugins y conserva lo propio: las fuentes de detekt, los
// filtros de Kover, Lint y los goldens de Roborazzi.
val unitTestJvmArgs =
    listOf(
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

val htmlReports =
    mapOf(
        "Android Lint" to "lint-results-debug.html",
        "detekt" to "detekt/detekt.html",
        "ktlint (main)" to "ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.html",
        "ktlint (test)" to "ktlint/ktlintTestSourceSetCheck/ktlintTestSourceSetCheck.html",
        "ktlint (scripts)" to "ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.html",
        "Kover (cobertura)" to "kover/htmlDebug/index.html"
    )

fun TestOptions.configureUnitTests() {
    unitTests {
        isIncludeAndroidResources = true
        all { test ->
            test.maxHeapSize = "2g"
            // Robolectric 4.17+ en JDK 17+ necesita abrir paquetes internos del JDK
            // (https://robolectric.org/getting-started/). Sin jdk.internal.access, el arranque del
            // entorno de SDK 36+ falla en ApplicationSharedMemory.create().
            test.jvmArgs(unitTestJvmArgs)
        }
    }
}

subprojects {
    val module = this

    pluginManager.withPlugin("dev.detekt") {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            parallel = true
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            basePath = rootDir
        }
    }

    pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<KtlintExtension> {
            android.set(true)
            ignoreFailures.set(false)
            reporters {
                reporter(ReporterType.PLAIN)
                reporter(ReporterType.HTML)
                reporter(ReporterType.CHECKSTYLE)
            }
        }

        tasks.register("codeQuality") {
            group = "verification"
            description = "Ejecuta Android Lint + ktlint + detekt en un solo comando."
            dependsOn("ktlintCheck", "detekt", "lint")
        }

        tasks.matching { it.name in listOf("ktlintCheck", "detekt", "lint") }.configureEach {
            mustRunAfter("ktlintFormat")
        }

        tasks.register("formatAndAnalyze") {
            group = "verification"
            description = "Formatea el codigo (ktlintFormat) y luego verifica todo (ktlintCheck + detekt + lint)."
            dependsOn("ktlintFormat", "codeQuality")
        }

        val reportsRoot = module.layout.buildDirectory.dir("reports")
        tasks.register("qualityReports") {
            group = "verification"
            description = "Genera los reportes HTML de Lint, detekt, ktlint y Kover. Usa --continue si un gate falla."
            dependsOn("lint", "detekt", "ktlintCheck", "koverHtmlReportDebug")

            val root = reportsRoot
            val reports = htmlReports
            val name = module.path
            doLast {
                logger.lifecycle("\nReportes HTML ($name):")
                reports.forEach { (title, relativePath) ->
                    val file = root.get().file(relativePath).asFile
                    val status = if (file.exists()) file.toURI().toString() else "(no generado)"
                    logger.lifecycle("  %-20s %s".format(title, status))
                }
            }
        }
    }

    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> { testOptions { configureUnitTests() } }
    }
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> { testOptions { configureUnitTests() } }
    }

    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests = false
    }

    pluginManager.withPlugin("org.sonarqube") {
        extensions.configure<SonarExtension> {
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
    }
}
