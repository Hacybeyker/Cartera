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
├── api/
│   ├── openapi.yaml       # Contrato de la API (OpenAPI 3.1)
│   ├── fixtures/          # Datos del backend embebido
│   └── tools/             # Generador de movimientos (semilla fija)
├── app/src/main/java/com/hacybeyker/cartera/
│   ├── core/              # Compartido: design system y componentes comunes, dominio y datos de sesión,
│   │                      # configuración, primer uso, cuentas y movimientos, paginación, cliente HTTP
│   │                      # y marcas de contenido visible
│   ├── navigation/        # CarteraApp (raíz), AppNavDisplay (Navigation 3) y la pila de entrada
│   └── feature/<name>/    # splash, login, terms, onboarding, home, movements y legacy (domain / data / ui / di)
├── app/src/demoTools/     # Backend embebido y escenarios (solo debug y demo)
├── AGENTS.md              # Reglas del proyecto para agentes de IA (CLAUDE.md lo importa)
└── gradle/
    └── libs.versions.toml # Catálogo centralizado de versiones
```

---

## ⚡ Ejecutar

```bash
./gradlew assembleDebug    # o Run desde Android Studio
./gradlew assembleDemo     # Variante para medir: R8 como release, profileable y firmada con la clave debug
```

Debug y demo traen un backend embebido que escucha en `http://127.0.0.1:8765/api/v1/` y sirve los
datos de `api/fixtures/` según el contrato de `api/openapi.yaml`. Release apunta a
`https://cartera.invalid/api/v1/`, un marcador que no resuelve.

---

## 👤 Usuario de prueba

| Usuario | Contraseña | Nombre |
|---|---|---|
| `ana.torres` | `Cartera2026` | Ana Torres |

Sin OTP. Son datos de demo, públicos a propósito: solo existen en `api/fixtures/` y en el backend
embebido. La sesión vive en memoria, así que cada arranque en frío pide login.

| Cuenta | Alias | Moneda | Saldo disponible | Movimientos |
|---|---|---|---|---|
| ACC-001 | Cuenta Sueldo | PEN | S/ 8,450.75 | 1.200 |
| ACC-002 | Cuenta Gastos | PEN | S/ 2,300.00 | 30 |
| ACC-003 | Cuenta Dólares | USD | US$ 1,050.20 | 12 |

Destinos de transferencia: `999-0000001-0-01` (Luis Ramírez, PEN), `999-0000002-0-02` (Comercial
Andina S.A.C., PEN) y `999-0000003-0-10` (María Quispe, USD). Cualquier otro número da
`DESTINATION_NOT_FOUND`; enviar soles a la cuenta en dólares da `CURRENCY_MISMATCH`.

---

## 🚪 Recorrido de entrada

```
Splash ──► Login ──► Términos* ──► Onboarding* ──► Home
```

- **Splash** pide `GET /app/config`. Si falla o el backend está caído, muestra el error y Reintentar
  en la misma pantalla.
- **Login**: usuario y contraseña incorrectos se avisan bajo el formulario; un error de red o del
  servidor, en un modal con Reintentar y Cancelar. Mientras ingresa, un progreso bloquea la pantalla.
- **Términos** (\*) aparece si la versión aceptada en el dispositivo difiere de `termsVersion`.
  Aceptar se activa al marcar la casilla; Rechazar (o Atrás) descarta la sesión y vuelve a Login.
- **Onboarding** (\*) aparece una vez por instalación: tres páginas con Siguiente, Omitir en todas y
  Empezar en la última.
- Al terminar, la pila queda en `[Home]` y Atrás sale de la app.

Los términos aceptados y el onboarding visto se guardan en SharedPreferences: `pm clear` (o
`clearState` en Maestro) los vuelve a mostrar. El token vive solo en memoria. Si una llamada
autenticada responde `SESSION_EXPIRED` o `UNAUTHORIZED` (por ejemplo, con `session_ttl` corto o tras
la muerte del proceso), aparece el modal "Tu sesión expiró"; Ingresar deja la pila en `[Login]` y,
tras el nuevo login, no se repiten Términos ni Onboarding.

---

## 🏠 Home y movimientos

```
Home  [barra inferior: Inicio · Mercado · Cambio · Más]
├─ Inicio  [tabs: Cuentas · Movimientos]
│   ├─ Cuentas ──► Movimientos de cuenta ──► Detalle ──► Desglose
│   └─ Movimientos ──► Detalle ──► Desglose
├─ Mercado · Cambio   (próximamente)
└─ Más ──► Servicios clásicos (próximamente) · Cerrar sesión · Versión
```

- Cada tab de Home conserva su estado (scroll y tab superior) mientras se usa otro.
- **Inicio** pide `GET /me` y `GET /accounts` en paralelo. **Movimientos** carga al abrir el tab.
- Las listas de movimientos se paginan por cursor, de a 20. Si falla la carga de más, aparece una fila
  con Reintentar al final y lo ya cargado se mantiene; el scroll no reintenta solo.
- **Movimientos de cuenta** pide la cuenta y su primera página en paralelo. Detalle y Desglose se
  apilan sobre Home (sin barra inferior) y vuelven con `barra_volver` o Atrás. Un id que no existe
  (`NOT_FOUND`) se muestra como error sin Reintentar.
- **Cerrar sesión** pide confirmación y deja la pila en `[Login]`; el próximo login no repite
  Términos ni Onboarding. El botón Transferir de Cuentas está deshabilitado por ahora.

---

## 🏷️ testTags y marcas

La raíz expone los testTags como `resource-id` (`testTagsAsResourceId`), y cada modal hace lo mismo
en su ventana. La convención es snake_case en español, `<pantalla>_<elemento>`, con los estados
`<pantalla>_cargando`, `<pantalla>_error`, `<pantalla>_reintentar` y `<pantalla>_vacio`.

| Pantalla | testTags |
|---|---|
| Comunes | `barra_volver`, `progreso_bloqueante`, `modal_sesion_expirada` (`_aceptar`), `modal_error` (`_reintentar`, `_cancelar`), `modal_confirmacion` (`_aceptar`, `_cancelar`) |
| Splash | `splash_pantalla`, `splash_cargando`, `splash_error`, `splash_reintentar` |
| Login | `login_pantalla`, `login_usuario`, `login_contrasena`, `login_mostrar_contrasena`, `login_entrar`, `login_error` |
| Términos | sin testTags: "Términos y condiciones", "He leído y acepto…", "Aceptar", "Rechazar" |
| Onboarding | sin testTags: "Siguiente", "Omitir", "Empezar" |
| Home | `home_pantalla`, `home_tab_inicio`, `home_tab_mercado`, `home_tab_cambio`, `home_tab_mas` |
| Inicio | `inicio_pantalla`, `inicio_saludo`, `inicio_tab_cuentas`, `inicio_tab_movimientos` |
| Inicio · Cuentas | `cuentas_lista`, `cuentas_item_<id>`, `cuentas_transferir` y estados `cuentas_*` |
| Inicio · Movimientos | `movimientos_lista`, `movimientos_item_<id>`, `movimientos_mas_cargando`, `movimientos_mas_reintentar` y estados `movimientos_*` |
| Movimientos de cuenta | `cuenta_pantalla`, `cuenta_saldo`, `cuenta_movimientos_lista`, `cuenta_movimientos_item_<id>`, `cuenta_movimientos_mas_reintentar` y estados `cuenta_*` |
| Detalle | `movimiento_detalle`, `movimiento_monto`, `movimiento_ver_desglose` y estados `movimiento_*` |
| Desglose | `movimiento_desglose`, `desglose_ilustracion`, `desglose_linea_<n>` (desde 1) y estados `desglose_*` |
| Mercado / Cambio | `mercado_proximamente` / `cambio_proximamente` |
| Más | `mas_pantalla`, `mas_nombre`, `mas_servicios_clasicos`, `mas_cerrar_sesion`, `mas_version` y estados `mas_*` |
| Servicios clásicos | `legacy_proximamente` |
| Escenario (debug y demo) | `escenario_pantalla`, `escenario_<clave>` (las claves de adb), `escenario_failure_mode_<valor>`, `escenario_guardar`, `escenario_valores_defecto`, `escenario_restablecer_datos`, `escenario_mensaje` |

Términos y Onboarding no llevan testTags a propósito: imitan pantallas de terceros y se seleccionan
por texto.

**Marcas de contenido visible.** Cada pantalla avisa con el nombre de su marca tras el primer frame
con contenido real: una vez por cada carga o refresco, nunca por el estado de carga ni por cargar más
páginas. Rotar o volver a una pantalla sin datos nuevos no repite la marca. Marcas: `login`,
`terminos`, `onboarding`, `inicio_cuentas`, `inicio_movimientos`, `cuenta_movimientos`,
`movimiento_detalle`, `movimiento_desglose`, `mercado`, `cambio` y `mas`. Por defecto nadie escucha. Para
recibirlas, agrega un `VisibleContentMarker` al conjunto desde un módulo de Hilt propio, sin tocar las
pantallas:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MarksModule {
    @Provides
    @IntoSet
    fun marker(): VisibleContentMarker = VisibleContentMarker { mark -> Log.i("Marks", mark) }
}
```

---

## 🎛️ Escenarios de red (debug y demo)

La app resuelve el valor de cada endpoint y lo envía al backend como cabecera `X-Demo-*`. El escenario
se guarda en el dispositivo y sobrevive a los reinicios; `pm clear` (o `clearState` de Maestro) lo
devuelve a los valores por defecto. Release no tiene nada de esto.

| Parámetro | Extra | Valores | Por defecto |
|---|---|---|---|
| Latencia | `--es latency "default=400,movements=2000"` | ms, global y por endpoint, sin jitter | 400 |
| Tasa de fallo | `--es failure_rate "default=0,transfers=100"` | 0–100 %, global y por endpoint, con semilla fija | 0 |
| Tipo de fallo | `--es failure_mode timeout` | `http_500` · `timeout` · `disconnect` | `http_500` |
| TTL de la sesión | `--ei session_ttl 60` | segundos; rige desde el próximo login | 900 |
| Backend caído | `--ez backend_down true` | 503 en todos los endpoints | `false` |
| Valores por defecto | `--ez defaults true` | restablece el escenario antes de aplicar el resto | — |

Ids de endpoint: `app_config`, `login`, `me`, `accounts`, `account`, `movements`, `movement`,
`breakdown`, `transfers`.

- Lo que no se envía no cambia. Un valor por endpoint sin `default=` usa el valor de fábrica como global.
- Un valor inválido no se aplica: el escenario efectivo y lo rechazado se imprimen en logcat con la
  etiqueta `CarteraDemo`.

**Por adb.** El receptor está protegido con `android.permission.DUMP`, así que solo adb puede usarlo.
`--include-stopped-packages` hace que el broadcast llegue también después de un `pm clear`.

```bash
adb shell am broadcast --include-stopped-packages -p com.hacybeyker.cartera \
  -a com.hacybeyker.cartera.demo.SET_SCENARIO \
  --es latency "default=400,movements=2000" --ei session_ttl 60

# Volver a los fixtures (saldos, movimientos e idempotencia)
adb shell am broadcast --include-stopped-packages -p com.hacybeyker.cartera \
  -a com.hacybeyker.cartera.demo.RESET_DATA

# Ver el escenario efectivo y cada llamada al backend
adb logcat -s CarteraDemo CarteraBackend
```

**Con Maestro.** Los argumentos de `launchApp` usan las mismas claves y se guardan igual que un
broadcast, así que cada flow lleva su escenario aunque use `clearState`:

```yaml
- launchApp:
    clearState: true
    arguments:
      latency: "default=400,movements=2000"
      session_ttl: 60
```

Si un flow no usa argumentos, el orden es `pm clear` → broadcast → `launchApp` sin `clearState`.

**Desde la app.** En Más, 7 toques en Versión abren la pantalla Escenario, con los mismos campos que
adb (en el mismo formato), más Guardar, Valores por defecto y Restablecer datos. Release no la
compila: ahí los 7 toques no hacen nada.

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
