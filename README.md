# Cartera


[![CI](https://github.com/Hacybeyker/Cartera/actions/workflows/ci.yml/badge.svg)](https://github.com/Hacybeyker/Cartera/actions/workflows/ci.yml)
[![Release](https://github.com/Hacybeyker/Cartera/actions/workflows/release.yml/badge.svg)](https://github.com/Hacybeyker/Cartera/actions/workflows/release.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.hacybeyker.cartera&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=com.hacybeyker.cartera)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2026.09.00-4285F4?logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-minSdk_26-3DDC84?logo=android&logoColor=white)

> App Android demo de billetera digital y banco de pruebas realista para las skills de [agent-skills](https://github.com/Hacybeyker/agent-skills) y las pruebas E2E con [testigo](https://github.com/Hacybeyker/testigo). Fea pero fiel: importa el comportamiento (esperas de red, fallos, sesión que expira, pantallas que se interponen), no el diseño visual.

---

## 📁 Estructura

```
.
├── app/src/main/java/com/hacybeyker/cartera/
│   ├── core/              # Compartido (design tokens en core/designsystem/)
│   ├── navigation/        # AppNavDisplay (Navigation 3)
│   └── feature/<name>/    # Por features: domain / data / ui / di
├── AGENTS.md              # Reglas del proyecto para agentes de IA (CLAUDE.md lo importa)
└── gradle/
    └── libs.versions.toml # Catálogo centralizado de versiones
```

---

## ⚡ Ejecutar

```bash
./gradlew assembleDebug    # o Run desde Android Studio
```

---

## 🧪 Tests y quality gates

Todo corre en la JVM: no hay tests instrumentados. Lo que necesita hardware se verifica a mano en un
dispositivo.

```bash
./gradlew test                    # Unitarios + Robolectric (JVM, sin emulador)
./gradlew koverVerifyDebug        # Gate de cobertura (>=90% sobre domain/data/ViewModels)
./gradlew verifyRoborazziDebug    # Gate de screenshots (compara contra los goldens del repo)
./gradlew recordRoborazziDebug    # Regenera los goldens tras un cambio visual intencional
```

El CI encadena `test koverVerifyDebug verifyRoborazziDebug` en una sola invocación: los tests corren
una vez y los tres gates reutilizan esa corrida.

---

## 🎨 Calidad de código

ktlint + detekt + Android Lint están preconfigurados. Reglas en `.editorconfig`, `config/detekt/detekt.yml` y `lint.xml`.

```bash
# Antes de cada commit: formatea y verifica todo
./gradlew formatAndAnalyze

# Solo verificación (CI / pre-push)
./gradlew codeQuality

# Reportes HTML navegables (imprime las rutas file:// al terminar)
./gradlew qualityReports --continue
```

| Herramienta | HTML | Formato para Sonar |
|-------------|------|--------------------|
| Android Lint | `app/build/reports/lint-results-debug.html` | `lint-results-debug.xml` |
| detekt | `app/build/reports/detekt/detekt.html` | `detekt/detekt.xml` |
| ktlint | `app/build/reports/ktlint/<sourceSet>/*.html` | `*.xml` (Checkstyle) |
| Kover | `app/build/reports/kover/htmlDebug/index.html` | `kover/reportDebug.xml` |

### SonarQube / SonarCloud

El análisis está configurado y **bloquea el CI** si el Quality Gate falla (`sonar.qualitygate.wait`).
Para activarlo: importa el repo en [sonarcloud.io](https://sonarcloud.io), **desactiva Automatic
Analysis** y añade el secret `SONAR_TOKEN` al repositorio. Mientras el secret no exista, el paso se
salta y el CI sigue en verde. Para una instancia self-hosted, exporta `SONAR_HOST_URL`.

El proyecto incluye un **pre-commit hook** que ejecuta `formatAndAnalyze` automáticamente antes de cada commit. Instálalo una sola vez tras clonar:

```bash
chmod +x scripts/setup-quality-hook.sh && ./scripts/setup-quality-hook.sh
```

---

## 🤖 Desarrollo con IA

Las reglas del proyecto están en `AGENTS.md` (Claude Code lo lee a través de `CLAUDE.md`). Con las skills [hb-*](https://github.com/Hacybeyker/agent-skills) instaladas, pídele a tu agente lo que necesites:

> *"Implementa la feature de perfil de usuario."*

El agente aplica Clean Architecture + MVVM (UDF) por features con el perfil `hb-stack-mobile-kotlin`, y corre el DoD de `AGENTS.md`.

---

## 📄 Changelog

Ver [CHANGELOG.md](./CHANGELOG.md) para el historial de cambios.

---

## 📄 Licencia

[MIT](./LICENSE)
