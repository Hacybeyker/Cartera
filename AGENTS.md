# Cartera

App Android nativa con Jetpack Compose, generada desde ScaffoldingAndroidCompose. Billetera digital de demo y banco de pruebas para las skills de agent-skills y las pruebas E2E con testigo: importa el comportamiento, no el diseño visual.

## Stack

Lo que el proyecto tiene hoy, literal y con versiones. Los agentes trabajan con este stack; ver `## Modernización`.

- Perfil: hb-stack-mobile-kotlin
- Variante: android
- Package base y applicationId: `com.hacybeyker.cartera`
- Versiones: **siempre** en `gradle/libs.versions.toml`; no las asumas.
- UI: Compose (BOM 2026.09.00) con Material 3 y `material-icons-core`; `activity-compose` 1.13.0, edge-to-edge.
- Presentación: MVVM con UDF según el perfil (`lifecycle-viewmodel-compose` 2.11.0, `lifecycle-runtime-compose`). Features con ViewModel: `splash`, `login`, `terms`, `onboarding`, `home` (Inicio, sus movimientos y Más), `movements` (cuenta, detalle y desglose) y `transfer` (formulario, confirmación y resultado); `legacy` solo reserva su pantalla.
- Asincronía: corrutinas y Flow (llegan con AndroidX lifecycle; no hay `kotlinx-coroutines-core` declarado).
- DI: Hilt 2.60.1 con KSP 2.3.13; `hilt-lifecycle-viewmodel-compose` 1.4.0 (`hiltViewModel`, también con fábrica asistida para los datos de las NavKey).
- Navegación: Navigation 3 1.2.1 (`navigation3-runtime`, `navigation3-ui`) y `lifecycle-viewmodel-navigation3`; NavKey serializables con `kotlinx-serialization-core` 1.12.0.
- Red: Retrofit 3.0.0 (con `converter-kotlinx-serialization`), OkHttp 5.5.1 y `kotlinx-serialization-json` 1.12.0; `BASE_URL` y `HTTP_TIMEOUT_SECONDS` en BuildConfig.
- Backend de demo: `mockwebserver3` 5.5.1 (REST y el canal WebSocket), solo en debug y demo (ver Arquitectura).
- Persistencia: `SharedPreferences` de la plataforma (términos aceptados, onboarding visto y, en debug y demo, el escenario). Imágenes: ninguna.
- Tests (solo JVM): JUnit 4.13.2, Robolectric 4.17 (emula la API 37), Compose UI Test (`ui-test-junit4`), Roborazzi 1.77.0, `kotlinx-coroutines-test` 1.11.0, Turbine 1.2.1, `androidx.test.ext:junit` 1.3.0, `espresso-core` 3.7.0 (ver Arquitectura).
- Calidad: ktlint (plugin 14.2.0), detekt 2.0.0-alpha.6, Android Lint (`abortOnError`); gate de cobertura con Kover 0.9.11 (90 %); goldens de UI con Roborazzi; SonarCloud (plugin 7.5.0.8588) en CI, opcional según `SONAR_TOKEN`.
- Build: AGP 9.5.1, Kotlin 2.4.21, Gradle 9.9.0, KSP; compileSdk y targetSdk 37, minSdk 26, Java 11; buildTypes `debug`, `release` (R8) y `demo` (hereda de release, `profileable`, firmado con la clave debug, mismo applicationId); JDK 21 en CI.

## Comandos

| Para | Comando |
| --- | --- |
| Compilar | `./gradlew assembleDebug` |
| Compilar la variante demo | `./gradlew assembleDemo` |
| Tests unitarios y de screenshots (JVM) | `./gradlew test` |
| Formatear y analizar | `./gradlew formatAndAnalyze` |
| Solo analizar, sin formatear (lo que corre el CI) | `./gradlew codeQuality` |
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
6. `./gradlew assembleDemo`

## Arquitectura

Sigue el perfil (Clean Architecture + MVVM con UDF, por features). Particularidades de este proyecto:

- Un solo módulo, `app`, con los paquetes `core/`, `navigation/` y `feature/<nombre>/`. Los design tokens están en `core/designsystem/`.
- Cada feature declara su NavKey en `ui/<Feature>NavKey.kt` y su entry builder en `ui/<Feature>Navigation.kt`; `navigation/AppNavDisplay.kt` solo arma el back stack y junta los entry builders.
- **Sin tests instrumentados:** no existe `androidTest`. El código que depende de hardware (cámara, GPS, biometría, sensores) se verifica a mano en un dispositivo y se excluye del gate de cobertura.
- `espresso-core` en `testImplementation` no contradice lo anterior: aquí no se escriben tests de Espresso. Lo usa `compose-ui-test` por dentro con Robolectric, y se declara explícito porque la versión que llega transitiva (3.5.1) falla en la API 37, que es la que emulan los tests (`app/src/test/resources/robolectric.properties`). Se puede quitar cuando `compose-ui-test` traiga la 3.7.0 o una superior por su cuenta.
- Los goldens de Roborazzi se versionan en `app/src/test/screenshots/` como `<pantalla>_<estado>_{claro,oscuro}.png`. Se graban con `captureLightAndDark` de `core/testing/` (tema fijo, sin color dinámico); con `withWindows = true` capturan también los diálogos.
- El gate de Kover mide solo `*.domain.*`, `*.data.*`, `*ViewModel*`, `com.hacybeyker.cartera.demo.backend.*` y `com.hacybeyker.cartera.demo.scenario.*` (sin los generados de Hilt); la UI se cubre con goldens, no con ese gate.
- `api/`: el contrato (`openapi.yaml`, OpenAPI 3.1, y `asyncapi.yaml`, AsyncAPI 3.0, para el canal en vivo), los `fixtures/` y sus generadores en `tools/`: `generate_movements.py` (movimientos) y `generate_market.py` (acciones, ticks, historiales, portafolio y tipo de cambio). Se generan con semilla fija y se versionan; si cambia un generador, se regenera con `python3 -I api/tools/<generador>.py`. `FixturesTest` fija sus reglas: el primer tick es el precio inicial y el último punto de cada historial también. Si cambia el contrato, cambian juntos el YAML, los fixtures y el backend.
- `app/src/demoTools/`: el backend embebido (`mockwebserver3` en `127.0.0.1:8765`, arrancado por `DemoToolsProvider` antes que la Application). Lo comparten debug y demo; release no lo compila. `api/fixtures/` entra como assets de esas dos variantes, sin copiarse. `main` no contiene datos ni respuestas simuladas.
- El backend honra solo las cabeceras `X-Demo-*` del contrato y trabaja en el hilo `CarteraBackend-worker`; cada intercambio se registra en logcat con la etiqueta `CarteraBackend`.
- Backend por mesas (`BackendDesks`): `SessionDesk`, `TransferDesk`, `MarketDesk` (acciones, historial y portafolio), `OrderDesk`, `FxDesk` (cotizaciones con vencimiento y cambio) y `BiometricDesk`. `BackendRouter` las une con un mapa de `Endpoint` a handler. El estado en memoria es `BackendState`: saldos, movimientos, tenencias (`HoldingsBook`), cotizaciones, dispositivos, numeración e idempotencia (por usuario y operación). `MarketState` guarda el tick vigente de cada acción y del tipo de cambio: empieza en el primero, solo avanza con `advance`/`advanceFx` (desde el hilo del backend) y vuelve al primero al agotarse. Órdenes y portafolio usan siempre ese precio. `POST /_demo/reset` devuelve todo a los fixtures.
- Canal en vivo `WS /market/stream` (`MarketStream`): el router autentica el handshake como cualquier endpoint y responde un 101 interno que `CarteraBackend` convierte en el upgrade de `mockwebserver3`; `StreamListener` pasa cada evento del socket al hilo `CarteraBackend-worker`, que ahora es un `ScheduledExecutorService` donde corren también los ticks y los temporizadores (corte y vencimiento de sesión). Un solo reloj de mercado global: corre mientras haya conexiones y `tick_ms` > 0, al intervalo del último handshake, y en cada tick avanza `MarketState` entero y envía a cada conexión solo lo suscrito. Cierra con 4401 al vencer el token del handshake; el corte de `stream_drop_s` usa `RealWebSocket.failWebSocket` (el socket TCP se cierra sin trama de cierre). Los tests usan `FakeScheduler` (tiempo virtual) y `FakeStreamSocket`; `CarteraBackendStreamTest` prueba el socket real con un cliente WebSocket de OkHttp.
- Cliente HTTP en `core/data/network/`: `CarteraApi` (Retrofit), DTO en `dto/`, `ApiCaller`, que traduce excepciones y cuerpos RFC 9457 a `ApiResult`/`ApiError` (los repositorios lo usan en vez de atrapar excepciones), y `BearerInterceptor` sobre el token en memoria de `core/data/session/`. Sin reintentos automáticos (`retryOnConnectionFailure = false`).
- Puntos de extensión que release deja vacíos (`@Multibinds`): interceptores `@ExtraInterceptors`, entradas de navegación `NavEntryInstaller` (reciben `onBack`), la pantalla oculta `HiddenScreen` (7 toques en Versión) y marcas de contenido visible `VisibleContentMarker` (`core/ui/marks/`). `demoTools` llena los tres primeros con Hilt desde `demo/di/` (la pantalla Escenario); las marcas no tienen ningún binding. `main` nunca importa nada de `demo`.
- Interruptores del cliente (`FeatureFlag` en `core/domain/flags/`): `MOVEMENTS_FULL_PAGE`, `ACCOUNTS_EAGER_REFRESH` y `BREAKDOWN_RICH_TRANSITION`, con el mismo nombre en BuildConfig (siempre `false`) y su clave en minúsculas en el escenario, adb, launchApp y la pantalla Escenario. `BuildConfigFeatureFlags` toma la primera `FeatureFlagSource` (`@Multibinds`, vacío en release) que decide y si no, BuildConfig; `demoTools` aporta la del escenario, que se lee en cada uso. Los dos caminos de cada uno viven en `main` y se eligen en tiempo de ejecución. Las ilustraciones de `res/drawable-nodpi/` solo las usa la transición enriquecida del desglose.
- Marcas: `ReportContentVisible(mark, contentKey)` avisa una vez por cada clave nueva. Las pantallas que cargan usan el `contentVersion` de su estado, que sube con cada carga o refresco correcto; lo ya avisado vive en el ViewModelStore de la entrada, así que rotar o volver a la entrada no repite la marca.
- Errores de dominio en `core/domain/` (`Outcome`, `AppError`): los repositorios convierten el `ApiResult` con `toOutcome`, y la UI traduce cada `AppError` a texto con `messageRes()` de `core/ui/`.
- Sesión: `ApiCaller` avisa a `SessionExpiryListener` cuando una llamada da `SESSION_EXPIRED`, `UNAUTHORIZED` o un 401 sin cuerpo RFC 9457. `InMemorySessionRepository` borra el token y `AppShellViewModel` abre el modal de sesión expirada desde `navigation/CarteraApp`, que es la raíz (`testTagsAsResourceId`) y dueña de la pila.
- Entrada: `ResolveEntryStepUseCase` (`core/domain/entry/`) decide si tras el login siguen Términos, Onboarding o Home, y `AppNavDisplay` deja la pila solo con el destino (`resetTo`). Términos y Onboarding no llevan testTags a propósito: se prueban por texto.
- Estados y modales comunes en `core/designsystem/component/` (también vacío y próximamente), con sus testTags en `CommonTestTags`. Un `ErrorState` sin `onRetry` no muestra Reintentar: así se ve `NOT_FOUND` (`AppError.isRetryable`). Cada diálogo usa `windowRootTestTag`: vive en otra ventana y no hereda `testTagsAsResourceId` de la raíz.
- Cuentas y movimientos viven en `core/domain/` y `core/data/` porque los usan `home` y `movements`. Los repositorios mapean el DTO dentro de la llamada de `ApiCaller`, así un monto o una fecha mal formados terminan en `InvalidResponse` y no en un cierre; `ApiCaller` solo atrapa `SerializationException`, `NumberFormatException` y `DateTimeException`, para no disfrazar un error de programación. La paginación por cursor (20 por página) se arma con `PagedList` y `NextPageLoader` (`core/ui/paging/`) y se dibuja con `PagedMovementList` (`core/ui/movement/`): si falla la carga de más, queda una fila con Reintentar y el scroll no reintenta solo.
- Caché de cuentas: `RemoteAccountRepository` guarda la lista en memoria mientras no cambien la revisión de `AccountChanges` ni el token. Una transferencia confirmada, o una que terminó sin respuesta o con un error del servidor (pudo cobrar), sube la revisión desde `ConfirmTransferUseCase`. Inicio y Movimientos reciben `Shown` cada vez que entran en composición y vuelven a pedir sus datos solo si la revisión cambió; Inicio refresca solo las cuentas, sin `/me`. Por eso "Volver al inicio" usa `popTo(HomeNavKey)`: reutiliza la entrada de Home en vez de crear otra.
- Transferencia: el formulario valida en el cliente (`ValidateTransferUseCase`) y cada Continuar crea una orden con su `Idempotency-Key`, que viaja en `TransferConfirmNavKey` y se reutiliza en cada Reintentar. Los 422 de negocio son `AppError` propios con un modal cada uno (`NoticeDialog`); `VALIDATION_ERROR` se muestra bajo cada campo del resumen.
- Home guarda el tab elegido y el estado de cada tab con un `SaveableStateHolder`; los ViewModels de los tabs viven en la entrada de Home y se crean al abrir el tab (Movimientos y Más cargan recién entonces). Las pantallas de detalle se apilan sobre Home, así que la barra inferior no se ve.
- Escenario de demo en `demo/scenario/`: se guarda en SharedPreferences y lo cambian el receptor de adb (`ScenarioReceiver`, protegido con DUMP, log `CarteraDemo`) y los argumentos de arranque de Maestro (`LaunchArgumentsCallbacks`). `DemoScenarioInterceptor` lo traduce a cabeceras por endpoint con `Endpoint.resolve`: `tick_ms` y `stream_drop_s` viajan solo en el handshake de `/market/stream`, y `quote_ttl_s` en `GET /fx/rates` y en ese handshake; el backend las lee en `DemoScenario`. `DemoToolsProvider` arranca el backend y registra esos callbacks antes que la Application. La pantalla Escenario (`demo/scenario/ui/`) edita el mismo almacenamiento con el mismo parser.
- Robolectric registra `DemoToolsProvider` (viene en el manifest de debug), pero no lo instancia: los tests de UI no abren el puerto 8765. Un test que lo necesite lo crea a mano.
- Los tests de `demoTools` van en `src/testDebug/`: AGP 9 crea unit tests solo para debug. detekt analiza `main`, `test` y `demoTools`, pero no `src/testDebug`, porque sus reglas de test solo excluyen `**/test/**`; ktlint y Lint sí lo cubren.

## Modernización

- Ninguna decidida.
- Los agentes no migran ni proponen migrar fuera de lo decidido aquí. Cuando una tarea toque una oportunidad clara, una línea al final del entregable, sin plan.

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
- Tráfico sin TLS prohibido (`network_security_config`), salvo hacia `127.0.0.1` y solo en debug y demo.
- La clave JWT del backend embebido y las credenciales del usuario de prueba son de demo y públicas a propósito: viven solo en `api/` y `demoTools`, nunca en `main`.
