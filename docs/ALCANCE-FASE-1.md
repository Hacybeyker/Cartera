# Cartera · Alcance de la fase 1

> **Estado:** aprobado el 2026-11-07, con todas las propuestas (P1–P6) y decisiones (D1–D6) de la sección 1.
> Fija qué se construye, no cómo se escribe el código.

**Datos fijos:** app `Cartera` · paquete y applicationId `com.hacybeyker.cartera` · repo propio, público, MIT · código en inglés; UI y docs en español · base: ScaffoldingAndroidCompose (minSdk 26, targetSdk 37, Hilt, Navigation 3, un módulo `app`, DoD de la plantilla).

**Fase 1 en una línea:** el recorrido completo de una billetera (splash → login → términos → onboarding → Home → cuentas → movimientos → detalle → desglose → transferencia → Home). Corre contra un backend embebido que sigue un contrato OpenAPI, con escenarios de red controlables por adb y tres defectos de rendimiento detrás de interruptores.

---

## 1. Decisiones (aprobadas)

### 1.1 Lo que la plantilla no trae

Revisión de la plantilla (`main` al 2026-11-07): en dos puntos difería de lo que se daba por hecho, y le faltaban piezas que la fase 1 necesita. Se aplica la columna "Propuesta".

| # | Hueco | Propuesta | Alternativa |
|---|---|---|---|
| P1 | **No trae Retrofit, OkHttp ni JSON.** Solo `kotlinx-serialization-core`, que usan las NavKey. | En `main`: Retrofit 3, OkHttp 5, `kotlinx-serialization-json` y el converter oficial de Retrofit. `mockwebserver3` (el artefacto de OkHttp 5 sin JUnit) solo en debug, demo y tests. | Ninguna: sin cliente HTTP no hay contrato. |
| P2 | **Usa Navigation 3** (`NavKey` + entry builders), no Navigation Compose 2.x. | Se mantiene Navigation 3 tal cual. | — |
| P3 | **Solo tiene debug y release.** | Nuevo buildType `demo`: hereda de release (R8, no depurable), es `profileable` y se firma con la clave debug. Mismo applicationId en todas las variantes, sin sufijo. `assembleDemo` se suma al DoD; las demás tareas del DoD no cambian (siguen sobre debug). Demo no corre tests propios. | Flavor `backend` (embedded/remote): más limpio en Android, pero renombra todas las tareas del DoD y del CI. |
| P4 | **No trae almacenamiento local.** | `SharedPreferences` de la plataforma (sin dependencia nueva) para los términos aceptados, el onboarding visto y el escenario. | DataStore Preferences. |
| P5 | **No trae cargador de imágenes**, y el defecto 3 pide "imágenes sin redimensionar". | Ilustraciones de categoría empaquetadas como drawables de ≈2.400 px. Sin dependencia nueva y determinista. | Coil 3 con imágenes servidas por el backend embebido: más realista, pero la caché lo vuelve menos determinista. |
| P6 | **Es de un solo módulo** (`app`). | El backend y las herramientas demo van en un directorio de código compartido por debug y demo (`app/src/demoTools/`), dentro de `app`. El módulo legacy de la fase 3 rompe esta regla; eso se decide en la fase 3. | Módulo Gradle `:demo-backend`. |

### 1.2 Decisiones de diseño

| # | Decisión | Por qué |
|---|---|---|
| D1 | Agregué `GET /app/config` (público) a los endpoints que listaste. Lo llama la splash. | La splash tiene una espera de red real, el "backend caído" se prueba desde la primera pantalla y el endpoint trae `termsVersion`. |
| D2 | Términos: aparecen si la versión aceptada localmente difiere de `termsVersion`. Onboarding: una vez por instalación. Los dos se guardan en el dispositivo. | No hacen falta endpoints de escritura. |
| D3 | El token vive solo en memoria: cada arranque en frío pide login, como en la banca móvil. No hay refresh token. | No queda nada sensible guardado (MASVS). Si el sistema mata el proceso, la primera llamada al volver da 401 y aparece el modal de sesión expirada, que es un caso más para testigo. |
| D4 | Los interruptores de los defectos llevan nombres neutros, de feature flag (§5). El mapeo a cada defecto vive solo en este documento. | Si se llaman "defecto", hb-performance los encuentra leyendo el código, no midiendo. Para una prueba a ciegas, el agente no debe leer este documento. |
| D5 | Un tercer canal de escenario, opcional: los argumentos de `launchApp` de Maestro, que la app lee al abrirse (solo en debug y demo). | `clearState` de Maestro borra el escenario guardado. Con argumentos, cada flow de testigo lleva su escenario. |
| D6 | Las pantallas sin testTag son Términos y Onboarding. | En el mercado suelen venir de terceros (WebView, SDK) sin ids, así que se seleccionan por texto. |

---

## 2. Pantallas y navegación

```
Splash ──► Login ──► Términos* ──► Onboarding* ──► Home        (* solo si corresponde, D2)

Home  [barra inferior]
├─ Inicio  [tabs superiores]
│   ├─ Cuentas ──► Movimientos de cuenta ──► Detalle ──► Desglose
│   │     └─ Transferir ──► Confirmación ──► Resultado ──► Home (Inicio · Cuentas)
│   └─ Movimientos ──► Detalle ──► Desglose
├─ Mercado   (próximamente)
├─ Cambio    (próximamente)
└─ Más ──► Servicios clásicos (reservado, fase 3) · Escenario (oculta) · Cerrar sesión
```

| Pantalla | NavKey | Llamadas | Contenido |
|---|---|---|---|
| Splash | `SplashNavKey` | `GET /app/config` | Logo. Ante un error o el backend caído, muestra Reintentar en la misma pantalla. |
| Login | `LoginNavKey` | `POST /auth/login` | Usuario, contraseña (mostrar/ocultar), Entrar. Un 401 se muestra bajo el formulario; un error de red, en un modal. |
| Términos | `TermsNavKey` | — | Texto largo con scroll, casilla "He leído y acepto", Aceptar (se activa al marcar la casilla) y Rechazar (vuelve a Login). |
| Onboarding | `OnboardingNavKey` | — | 3 páginas deslizables. Siguiente; Omitir en las tres; Empezar en la última. |
| Home | `HomeNavKey` | — | Barra inferior Inicio · Mercado · Cambio · Más. Cada tab conserva su estado. |
| Inicio | tab de Home | `GET /me` | Saludo ("Hola, Ana") y tabs superiores Cuentas · Movimientos. |
| Inicio · Cuentas | tab de Inicio | `GET /accounts` (en paralelo con `/me`) | Lista de cuentas con saldo y botón Transferir. |
| Inicio · Movimientos | tab de Inicio | `GET /movements` | Movimientos de todas las cuentas, paginados. Se cargan al abrir el tab. |
| Movimientos de cuenta | `AccountMovementsNavKey(accountId)` | `GET /accounts/{id}` y `GET /movements?accountId=` en paralelo | Cabecera con el saldo y lista paginada. |
| Detalle | `MovementDetailNavKey(movementId)` | `GET /movements/{id}` | Datos del movimiento y botón Ver desglose. |
| Desglose | `MovementBreakdownNavKey(movementId)` | `GET /movements/{id}/breakdown` | Conceptos, comisiones, impuestos, comercio e ilustración de categoría. |
| Transferir · formulario | `TransferFormNavKey` | `GET /accounts` | Cuenta de origen (propia), número de destino, monto y descripción opcional. Continuar valida en el cliente. |
| Transferir · confirmación | `TransferConfirmNavKey` | `POST /transfers` | Resumen, Confirmar y Cancelar. |
| Transferir · resultado | `TransferResultNavKey` | — | Éxito (n.º de operación y nuevo saldo) o error, y Volver al inicio. |
| Mercado / Cambio | tabs de Home | — | "Próximamente". |
| Más | tab de Home | `GET /me` | Nombre, Servicios clásicos, Cerrar sesión y Versión. |
| Servicios clásicos | `LegacyNavKey` | — | Reservada para el módulo legacy de la fase 3. Hoy muestra "Próximamente". |
| Escenario | `ScenarioNavKey` | `POST /_demo/reset` | Oculta: se abre con 7 toques en Versión. Solo existe en debug y demo; release no la compila. |

**Pila y retroceso**
- Al terminar el login, la pila queda en `[Home]` y Atrás en Home sale de la app.
- Las pantallas de detalle se apilan sobre Home y ocultan la barra inferior.
- "Volver al inicio" (y Atrás en Resultado) deja la pila en `[Home]`, en Inicio · Cuentas, con un solo refresco de cuentas.
- Atrás en el formulario con datos abre el modal "¿Salir sin transferir?". Atrás en Confirmación vuelve al formulario con los datos.
- Rechazar en Términos deja la pila en `[Login]`, sin sesión.

**Modales y errores**
- Carga de una pantalla: Cargando → Contenido | Vacío | Error a pantalla completa con Reintentar.
- Paginación: si falla la carga de más, aparece una fila al final con Reintentar y lo ya cargado se mantiene.
- Acciones (login, confirmar transferencia): progreso bloqueante. Ante un error de red o 5xx, modal con Reintentar y Cancelar. Ante un 422, mensaje por campo o modal según el código.
- Un 401 en cualquier llamada autenticada abre el modal "Tu sesión expiró"; Ingresar deja la pila en `[Login]`. Tras el nuevo login no se repiten Términos ni Onboarding.
- Hay confirmaciones para cerrar sesión, salir del formulario con datos y cancelar en Confirmación.
- La app no reintenta sola (`retryOnConnectionFailure = false`), así que cada fallo inyectado se ve. Timeouts del cliente: 10 s, en BuildConfig.

---

## 3. Contrato (`api/openapi.yaml`, OpenAPI 3.1)

**Reglas generales**
- `BASE_URL` va en BuildConfig. En debug y demo es `http://127.0.0.1:8765/api/v1/`; en release, `https://cartera.invalid/api/v1/`, un marcador que no resuelve. Migrar a un servidor real es cambiar solo ese valor.
- JSON en camelCase. Fechas ISO 8601 con zona (`2026-11-06T14:32:00-05:00`). Montos como texto decimal con moneda ISO 4217 (`{"value":"150.00","currency":"PEN"}`). Ids como texto.
- Autenticación con `Authorization: Bearer <JWT>`: HS256 con una clave de demo publicada en el contrato, claims `sub`, `iat` y `exp`. TTL por defecto: 900 s.
- Toda respuesta trae `X-Request-Id`.
- Paginación por cursor: `?cursor=&limit=` (de 1 a 1000, 20 por defecto) responde `{ items, nextCursor }`, con `nextCursor` en `null` al final, en orden de fecha descendente. Va por cursor y no por offset para que el movimiento nuevo de una transferencia no desplace las páginas.

**Endpoints**

| Método y ruta | Auth | Respuesta 2xx | Errores |
|---|---|---|---|
| `GET /app/config` | no | `minSupportedVersion`, `termsVersion` | 5xx |
| `POST /auth/login` | no | `accessToken`, `tokenType`, `expiresIn` | 401 `INVALID_CREDENTIALS`, 422 |
| `GET /me` | sí | `id`, `fullName`, `documentMasked`, `email` | 401 |
| `GET /accounts` | sí | `items[]`: `id`, `alias`, `type`, `currency`, `numberMasked`, `availableBalance`, `accountingBalance` | 401 |
| `GET /accounts/{accountId}` | sí | una cuenta | 401, 404 |
| `GET /movements?accountId=&cursor=&limit=` | sí | `items[]`: `id`, `accountId`, `date`, `description`, `category`, `amount` (con signo), `type` · `nextCursor`. Sin `accountId`, trae todas las cuentas. | 401, 422 |
| `GET /movements/{movementId}` | sí | el movimiento más `operationNumber`, `channel`, `status`, `counterparty` | 401, 404 |
| `GET /movements/{movementId}/breakdown` | sí | `lines[]` (concepto, monto), `fees`, `taxes`, `exchangeRate?`, `merchant`, `category` | 401, 404 |
| `POST /transfers` + `Idempotency-Key` | sí | 201: `transferId`, `operationNumber`, `createdAt`, `sourceAccountId`, `sourceAvailableBalance`, `movementId` | 401, 422, 5xx |
| `POST /_demo/reset` | solo entornos demo | 204: vuelve el estado a los fixtures | — |

**Errores** con cuerpo uniforme RFC 9457 (`application/problem+json`): `type`, `title`, `status`, `detail`, `code`, `traceId` y, en los 422, `errors[]` con `field`, `code` y `message`.

| HTTP | `code` | Cuándo | Qué hace la app |
|---|---|---|---|
| 401 | `INVALID_CREDENTIALS` | Login con datos incorrectos | Mensaje en Login |
| 401 | `SESSION_EXPIRED` | Token vencido (TTL) | Modal de sesión expirada |
| 401 | `UNAUTHORIZED` | Sin token o con un token inválido, p. ej. tras la muerte del proceso | Igual que `SESSION_EXPIRED` |
| 404 | `NOT_FOUND` | Id inexistente | Error sin reintento |
| 422 | `VALIDATION_ERROR` | Campos inválidos | Mensaje por campo |
| 422 | `INSUFFICIENT_FUNDS` · `DESTINATION_NOT_FOUND` · `CURRENCY_MISMATCH` | Transferencia | Modal específico |
| 500 | `INTERNAL_ERROR` | Fallo inyectado | Error con reintento |
| 503 | `SERVICE_UNAVAILABLE` (+ `Retry-After`) | Backend caído | Error con reintento |
| — | sin cuerpo | Timeout o desconexión | Error de red con reintento |

**Regla de negocio mínima, documentada en `POST /transfers`**
- Se valida que el origen sea una cuenta propia, que el destino exista en `third-party-accounts.json`, que ambos tengan la misma moneda y que 0 < monto ≤ saldo disponible.
- Si pasa, se descuenta el monto del saldo disponible del origen y se agrega un movimiento `DEBIT` al inicio de esa cuenta, con un desglose mínimo. No hay comisiones ni ITF.
- Un `Idempotency-Key` repetido devuelve la misma respuesta sin volver a cobrar.
- El estado vive en memoria: vuelve a los fixtures al reiniciar el proceso o con `POST /_demo/reset`.

**Lo único que decide el fake:** las credenciales, la emisión y validación del JWT con su TTL, la regla de transferencia y las reglas de escenario. Todo lo demás es leer fixtures, proyectarlos y paginar.

---

## 4. Backend embebido y escenarios

**Implementación**
- `mockwebserver3` arranca antes que la app, desde un ContentProvider declarado solo en debug y demo (sin dependencias), y escucha en `127.0.0.1:8765`. El tráfico sin TLS se permite solo hacia `127.0.0.1` y solo en esas dos variantes.
- Los fixtures viven en `api/fixtures/` y entran como assets solo en debug y demo, sin copiarse: `app-config.json`, `users.json`, `accounts.json`, `movements/ACC-00X.json` (cada registro completo: lista, detalle y desglose) y `third-party-accounts.json`. Los 1.200 movimientos de ACC-001 se generan una sola vez con semilla fija y quedan versionados; el generador queda en `api/tools/`.
- `main` no contiene datos ni respuestas simuladas. El servidor, el interceptor X-Demo-\*, el receptor de adb y la pantalla Escenario viven en `src/demoTools/`, que release no compila. `main` expone dos puntos de extensión vacíos (interceptores extra y entradas de navegación extra) que `demoTools` llena vía Hilt.
- Aislamiento para medir:
  - Los fixtures se cargan de forma perezosa, así que no inflan el arranque.
  - El servidor trabaja en hilos `CarteraBackend-*`, y su trabajo aparece en las trazas como TTFB.
  - Cada intercambio se registra en logcat con la etiqueta `CarteraBackend`: método, ruta, estado, ms y `X-Request-Id`.

**Escenarios.** El cliente resuelve el valor que corresponde a cada endpoint y lo envía como cabecera. El servidor solo honra la cabecera, igual que lo haría un servidor real en su entorno de demo.

| Parámetro | Valores | Por defecto | Cabecera | Extra de adb |
|---|---|---|---|---|
| Latencia | ms, global y por endpoint, sin jitter | 400 | `X-Demo-Latency-Ms` | `--es latency "default=400,movements=2000"` |
| Tasa de fallo | 0–100 %, global y por endpoint, con semilla fija | 0 | `X-Demo-Failure-Rate` | `--es failure_rate "default=0,transfers=100"` |
| Tipo de fallo | `http_500` · `timeout` · `disconnect` | `http_500` | `X-Demo-Failure-Mode` | `--es failure_mode timeout` |
| TTL de la sesión | s; rige desde el próximo login | 900 | `X-Demo-Session-Ttl` (solo en el login) | `--ei session_ttl 60` |
| Backend caído | 503 en todos los endpoints | off | `X-Demo-Backend-Down` | `--ez backend_down true` |
| Flags de rendimiento (§5) | on/off; solo afectan al cliente y no viajan | off | — | `--ez movements_full_page true` … |
| Valores por defecto | acción | — | — | `--ez defaults true` |
| Restablecer datos | acción | — | `POST /_demo/reset` | acción `…demo.RESET_DATA` |

Ids de endpoint: `app_config`, `login`, `me`, `accounts`, `account`, `movements`, `movement`, `breakdown`, `transfers`.

**Cómo se cambian**
- Por adb:
  ```
  adb shell am broadcast --include-stopped-packages -p com.hacybeyker.cartera -a com.hacybeyker.cartera.demo.SET_SCENARIO --es latency "default=400,movements=2000" --ei session_ttl 60
  ```
  - El receptor está exportado, pero protegido con `android.permission.DUMP`: solo adb puede enviarle algo. `--include-stopped-packages` hace que el broadcast llegue también después de un `pm clear`.
  - Lo que no se envía no cambia. El escenario se guarda y sobrevive a los reinicios; `pm clear` y `clearState` lo devuelven a los valores por defecto. El receptor imprime el escenario efectivo en logcat, con la etiqueta `CarteraDemo`.
- Desde la pantalla Escenario: los mismos campos, más Guardar, Valores por defecto y Restablecer datos.
- Con los argumentos de `launchApp` de Maestro (D5), con las mismas claves.
- Si un flow no usa argumentos, el orden para testigo es: `pm clear` → broadcast → `launchApp` sin `clearState`.

---

## 5. Defectos sembrados

Todos están apagados por defecto, y release no puede encenderlos. Cada interruptor tiene el mismo nombre en BuildConfig, en la pantalla Escenario y en adb (D4).

| # | Defecto | Interruptor | Dónde se ve | Qué mide hb-performance | Camino correcto (interruptor apagado) |
|---|---|---|---|---|---|
| 1 | Movimientos de cuenta piden una página de 1.000 y parsean el JSON en el hilo principal. | `MOVEMENTS_FULL_PAGE` / `movements_full_page` | Cuentas → Cuenta Sueldo (ACC-001) | Tramo NAV → marca `cuenta_movimientos`; `hilo_max_ms` | Página de 20 y parseo fuera del hilo principal. |
| 2 | Al volver al Home desde una transferencia, las cuentas se piden dos veces, sin caché. | `ACCOUNTS_EAGER_REFRESH` / `accounts_eager_refresh` | Resultado → Volver al inicio | `GET /accounts` entre `transferir_volver_inicio` y la marca `inicio_cuentas`: 2 contra 1 | Un solo refresco, con una caché en memoria que la transferencia invalida. |
| 3 | El desglose compone toda la pantalla de golpe dentro de un Crossfade, con la ilustración decodificada a ≈2.400 px. | `BREAKDOWN_RICH_TRANSITION` / `breakdown_rich_transition` | Detalle de MOV-002-0001 → Ver desglose | Ventana hasta la marca `movimiento_desglose`; `hilo_max_ms`, `frame_p90_ms` | Lista perezosa, Crossfade solo entre estados e ilustración al tamaño mostrado. |

Con los fixtures fijos, la latencia sin jitter y la variante demo (R8, profileable), antes y después se miden sobre el mismo recorrido y los mismos datos. El defecto 2 es exacto: se cuentan llamadas.

---

## 6. Testabilidad

- En la raíz: `Modifier.semantics { testTagsAsResourceId = true }`.
- Convención de testTags: snake_case en español, `<pantalla>_<elemento>`. Los ítems de lista usan `<lista>_item_<id del fixture>` y los estados, `<pantalla>_cargando`, `_error` y `_reintentar`.
- **Marca de contenido visible:** un punto de extensión en `core`. Cada pantalla avisa una sola vez, con el nombre de su marca, tras el primer frame con contenido real (no el estado de carga). Por defecto no hace nada; hb-performance lo reemplaza en su setup con un binding de Hilt, sin tocar las pantallas.

| Pantalla | Contenedor | Elementos | Marca |
|---|---|---|---|
| Comunes | — | `barra_volver`, `progreso_bloqueante`, `modal_sesion_expirada` (`_aceptar`), `modal_error` (`_reintentar`, `_cancelar`), `modal_confirmacion` (`_aceptar`, `_cancelar`) | — |
| Splash | `splash_pantalla` | `splash_reintentar` | — (la mide TTID/TTFD) |
| Login | `login_pantalla` | `login_usuario`, `login_contrasena`, `login_mostrar_contrasena`, `login_entrar`, `login_error` | `login` |
| **Términos** | **sin testTag** | por texto: "Términos y condiciones", "He leído y acepto…", "Aceptar", "Rechazar" | `terminos` |
| **Onboarding** | **sin testTag** | por texto: "Siguiente", "Omitir", "Empezar" | `onboarding` |
| Home | `home_pantalla` | `home_tab_inicio`, `home_tab_mercado`, `home_tab_cambio`, `home_tab_mas` | — |
| Inicio | `inicio_pantalla` | `inicio_saludo`, `inicio_tab_cuentas`, `inicio_tab_movimientos` | — |
| Inicio · Cuentas | `cuentas_lista` | `cuentas_item_<id>`, `cuentas_transferir` + estados | `inicio_cuentas` |
| Inicio · Movimientos | `movimientos_lista` | `movimientos_item_<id>`, `movimientos_mas_reintentar` + estados | `inicio_movimientos` |
| Movimientos de cuenta | `cuenta_pantalla` | `cuenta_saldo`, `cuenta_movimientos_lista`, `cuenta_movimientos_item_<id>` + estados | `cuenta_movimientos` |
| Detalle | `movimiento_detalle` | `movimiento_monto`, `movimiento_ver_desglose` + estados | `movimiento_detalle` |
| Desglose | `movimiento_desglose` | `desglose_ilustracion`, `desglose_linea_<n>` + estados | `movimiento_desglose` |
| Transferir · formulario | `transferir_pantalla` | `transferir_origen`, `transferir_origen_item_<id>`, `transferir_destino`, `transferir_monto`, `transferir_descripcion`, `transferir_continuar` | `transferir_formulario` |
| Transferir · confirmación | `transferir_confirmacion` | `transferir_confirmar`, `transferir_cancelar` | `transferir_confirmacion` |
| Transferir · resultado | `transferir_resultado` | `transferir_resultado_operacion`, `transferir_resultado_saldo`, `transferir_volver_inicio` | `transferir_resultado` |
| Mercado / Cambio | `mercado_proximamente` / `cambio_proximamente` | — | `mercado` / `cambio` |
| Más | `mas_pantalla` | `mas_nombre`, `mas_servicios_clasicos`, `mas_cerrar_sesion`, `mas_version` | `mas` |
| Servicios clásicos | `legacy_proximamente` | — | — |
| Escenario | `escenario_pantalla` | `escenario_<parámetro>`, `escenario_guardar`, `escenario_valores_defecto`, `escenario_restablecer_datos` | — |

**Usuario de prueba** (en el README, sin OTP): usuario `ana.torres` · contraseña `Cartera2026` · Ana Torres.

**Datos de referencia** (los valores definitivos para los flows de testigo: si alguno cambia, cambia primero aquí)

| Cuenta | Alias | Moneda | Número | Saldo disponible | Movimientos |
|---|---|---|---|---|---|
| ACC-001 | Cuenta Sueldo | PEN | \*\*\*\*4821 | S/ 8,450.75 | 1.200 (la lista grande, defecto 1) |
| ACC-002 | Cuenta Gastos | PEN | \*\*\*\*9034 | S/ 2,300.00 | 30, incluido MOV-002-0001 (desglose de 40 líneas, defecto 3) |
| ACC-003 | Cuenta Dólares | USD | \*\*\*\*1177 | US$ 1,050.20 | 12 |

| Destino (terceros) | Titular | Moneda |
|---|---|---|
| 999-0000001-0-01 | Luis Ramírez | PEN |
| 999-0000002-0-02 | Comercial Andina S.A.C. | PEN |
| 999-0000003-0-10 | María Quispe | USD |

- **Transferencia de referencia:** ACC-002 → 999-0000001-0-01 por S/ 150.00 deja ACC-002 en S/ 2,150.00 y agrega el movimiento nuevo al inicio.
- **Casos negativos:** un monto mayor al saldo lo frena el formulario ("El monto supera el saldo disponible"): valida contra un saldo siempre fresco, así que el modal `INSUFFICIENT_FUNDS` del servidor solo se cubre con tests; enviar de PEN a 999-0000003-0-10 da `CURRENCY_MISMATCH`; cualquier otro número da `DESTINATION_NOT_FOUND`.

---

## 7. Fuera de alcance

- **Fase 2:** acciones (mercado con precios en vivo, detalle con gráfico, órdenes de compra y venta con confirmación), cambio de divisas con calculadora en vivo, biometría con "Omitir", coach marks, bottom sheets y menú lateral.
- **Fase 3:** módulo legacy Fragment + XML + RxJava (aquí solo se reserva `LegacyNavKey`; con Navigation 3, un Fragment entra vía `AndroidFragment` o una Activity aparte, y se decide entonces), pantallas sin ids de ese módulo y contrato listo para un servidor externo. La fase 1 ya lo deja migrable, pero sin servidor ni despliegue.
- **No entran en la fase 1:** refresh token, cierre por inactividad, cifrado en reposo (no se guarda nada sensible), deep links (útiles para hb-performance; candidatos a la fase 2), push, diseño visual, i18n (solo español), CI de la variante demo, la instrumentación de hb-performance (solo se deja el punto de extensión) y los flows de testigo.

---

## 8. Bloques de implementación

Cada bloque termina con el DoD de la plantilla en verde (más `assembleDemo`, desde que exista la variante demo), con tests de la lógica nueva, sin commitear y con el mensaje del commit en Conventional Commits. La cobertura de Kover (90 % sobre `domain`, `data` y ViewModels) rige desde el bloque (a).

| Bloque | Cómo | Contenido | Tests | Tamaño |
|---|---|---|---|---|
| 0 | Comandos de a uno, luego un chat con hb-setup | Proyecto con `init-project.sh` (Cartera, `com.hacybeyker.cartera`), LICENSE MIT (la plantilla no trae), este documento en `docs/` y AGENTS.md con hb-setup (literal, sin modernización). | DoD de la plantilla | S |
| a | hb-implement | Dependencias (P1, P3, P4); contrato; fixtures; servidor (rutas, JWT, escenarios, regla de transferencia, idempotencia, reset); cliente Retrofit en `main` (servicios, DTO, errores RFC 9457, Bearer); interceptor X-Demo-\*; receptor de adb; README (usuario de prueba y escenarios); `## Stack` de AGENTS.md al día. | El servidor por endpoint y por regla; los fixtures decodifican con los DTO; el mapeo de errores; los interceptores | L (si se alarga: a1 servidor, a2 cliente) |
| b | hb-implement | Splash, login, términos y onboarding; sesión en memoria y 401 global; estados y modales comunes; raíz con `testTagsAsResourceId`; punto de extensión de marcas. | ViewModels y casos de uso; goldens de los `*Content` nuevos | M |
| c | hb-implement | Home con sus tabs, Inicio, cuentas, movimientos (tab y por cuenta, con paginación), detalle, desglose, Más, pantallas de próximamente, Servicios clásicos reservado y pantalla Escenario. | Lo mismo, más la paginación | L |
| d | hb-implement | Transferencia: formulario, confirmación y resultado; errores 422 por código; `Idempotency-Key`; modales; vuelta al Home con un solo refresco. | Validaciones, ViewModels y el flujo de resultado | M |
| e | hb-implement | Los tres defectos con sus interruptores (BuildConfig, Escenario y adb), las ilustraciones grandes y los flags en el README, solo con su nombre neutro. | Los dos caminos de cada flag | S |
