# Cartera

App Android nativa con Jetpack Compose, generada desde Cartera.

## Stack

- Perfil: hb-stack-mobile-kotlin
- Variante: android
- Package base: `com.hacybeyker.cartera`
- Versiones: **siempre** en `gradle/libs.versions.toml`; no las asumas.
- Calidad: ktlint, detekt y Android Lint; gate de cobertura con Kover (90 %); goldens de UI con Roborazzi; SonarCloud en CI.

## Comandos

| Para | Comando |
| --- | --- |
| Compilar | `./gradlew assembleDebug` |
| Tests unitarios y de screenshots (JVM) | `./gradlew test` |
| Formatear y analizar | `./gradlew formatAndAnalyze` |
| Regrabar goldens tras un cambio visual intencional | `./gradlew recordRoborazziDebug` |
| Reportes HTML de Lint, detekt, ktlint y Kover | `./gradlew qualityReports --continue` |
| Hook de calidad (una vez, tras clonar) | `./scripts/setup-quality-hook.sh` |
| Subir Gradle (dos veces; ver Reglas) | `./gradlew wrapper --gradle-version X.Y.Z --gradle-distribution-sha256-sum <sha256>` |

## DoD

Antes de dar una tarea por terminada, en este orden:

1. `./gradlew formatAndAnalyze`
2. `./gradlew test`
3. `./gradlew koverVerifyDebug`
4. `./gradlew verifyRoborazziDebug`
5. `./gradlew assembleDebug`

## Arquitectura

Sigue el perfil (Clean Architecture + MVVM con UDF, por features). Particularidades de este proyecto:

- Un solo módulo, `app`, con los paquetes `core/`, `navigation/` y `feature/<nombre>/`. Los design tokens están en `core/designsystem/`.
- Cada feature declara su NavKey en `ui/<Feature>NavKey.kt` y su entry builder en `ui/<Feature>Navigation.kt`; `navigation/AppNavDisplay.kt` solo arma el back stack y junta los entry builders.
- **Sin tests instrumentados:** no existe `androidTest`. El código que depende de hardware (cámara, GPS, biometría, sensores) se verifica a mano en un dispositivo y se excluye del gate de cobertura.
- `espresso-core` en `testImplementation` no contradice lo anterior: aquí no se escriben tests de Espresso. Lo usa `compose-ui-test` por dentro con Robolectric, y se declara explícito porque la versión que llega transitiva (3.5.1) falla en la API 37, que es la que emulan los tests (`app/src/test/resources/robolectric.properties`). Se puede quitar cuando `compose-ui-test` traiga la 3.7.0 o una superior por su cuenta.
- Los goldens de Roborazzi se versionan en `app/src/test/screenshots/`.

## Reglas del proyecto

- **Nunca sufijes una clase con `Impl`:** nómbrala por lo que la respalda (`RoomBookRepository`, `InMemoryGreetingRepository`). Los dobles de test se llaman `Fake…` o `Stub…`.
- **Comentarios:** por defecto ninguno. Solo el porqué, una decisión o una advertencia; nunca lo que el nombre ya dice.
- Imports con comodín y trailing commas prohibidos (ktlint los rechaza en el build). Sin comentarios en línea en `libs.versions.toml`.
- **Gradle se sube con la tarea `wrapper`, nunca editando `gradle-wrapper.properties` a mano.** `distributionSha256Sum` verifica la distribución que se descarga: si solo cambia `distributionUrl`, el build falla por checksum. El sha256 es el de `gradle-X.Y.Z-bin.zip` (el wrapper usa la distribución `-bin`) y está en `https://downloads.gradle.org/distributions/gradle-X.Y.Z-bin.zip.sha256`. El comando se corre dos veces: la primera fija la versión nueva y la segunda, ya con ella, regenera `gradle-wrapper.jar`, `gradlew` y `gradlew.bat`. Si un PR de Dependabot sube Gradle, revisa que cambie la URL y el checksum juntos.
- **No bajes un gate para que pase el build** (`abortOnError = false`, bajar `minBound`, `ignoreFailures`): arregla la causa.
- **No ejecutes la app** en un emulador ni en un dispositivo. La verificación automática llega hasta `assembleDebug` y `test`; lo que necesita hardware lo prueba la persona, así que indícale qué abrir y qué observar.
- Dependencias solo estables. Una excepción exige autorización explícita y queda documentada aquí con su contención:
  - **`detekt = 2.0.0-alpha.x`:** la rama 1.23.x no soporta Kotlin 2.4, así que la alternativa es quedarse sin detekt. Contención: `config/detekt/detekt.yml` corre con `config.validation: false` para tolerar renombres de reglas entre alphas, y `buildUponDefaultConfig = true` limita los overrides al archivo. Vuelve a estable cuando detekt 2.0.0 sea GA.

## Git

- Commits: Conventional Commits, en español, sin emoji. Agrupados por unidad funcional: cada commit compila y pasa el DoD.
- Rama principal: `main`. Se trabaja en ramas cortas y se integra por PR con squash.
- **La IA no commitea:** deja los cambios sin commitear y entrega el mensaje. Tampoco hace `push`, `tag` ni `merge`.

## Versionado y release

- SemVer. La versión vive en `gradle/libs.versions.toml` (`appVersion`) y `versionCode`, en `app/build.gradle.kts`, sube de 1 en 1.
- Tags `vX.Y.Z`, creados por la persona. Al subir el tag, el workflow `release.yml` compila el APK y el AAB y crea el GitHub Release: no lo crees a mano.
- `CHANGELOG.md` con Keep a Changelog, en español: cada cambio entra en `[Unreleased]`.

## Seguridad

- Sigue el perfil (OWASP MASVS). Ningún secreto en el repo: van en `local.properties` o en los secretos de CI (por ejemplo `SONAR_TOKEN`).
