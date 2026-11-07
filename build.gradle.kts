// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
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
        ":app:koverXmlReportDebug"
    )
}
