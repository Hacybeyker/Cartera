# Cartera · Alcance de la fase 2

> **Estado:** aprobado el 2026-11-08, con todas las propuestas (Q1–Q4) y decisiones (E1–E9) de la sección 1 · parte de la fase 1 cerrada (`43df1ce`, CI en verde).
> Fija qué se construye, no cómo se escribe el código.

**Fase 2 en una línea:** mercado de acciones con precios en vivo, detalle con gráfico y órdenes de compra y venta; cambio de divisas con calculadora en vivo; ingreso con biometría (con "Omitir"); coach marks de primera vez; bottom sheets y menú lateral. Todo corre contra el mismo backend embebido, con el contrato extendido.

Siguen vigentes todas las reglas de la fase 1: contrato primero, fixtures como único origen de datos, escenarios por cabeceras `X-Demo-*`, testTags y marcas de contenido visible, `main` sin nada de demo y D4 para cualquier caso sembrado.

---

## 1. Decisiones (aprobadas)

### 1.1 Lo que el stack actual no trae

| # | Hueco | Propuesta | Alternativa |
|---|---|---|---|
| Q1 | **Biometría:** no está `androidx.biometric`. | Agregarla: es la única dependencia nueva de la fase. La clave vive en el Android Keystore de la plataforma. | Solo la pantalla interpuesta con Activar y Omitir, sin ingreso biométrico real (sin dependencia). |
| Q2 | **Tiempo real:** no hay canal en vivo. | WebSocket de OkHttp: ya está en el stack y `mockwebserver3` lo soporta. El canal se documenta en `api/asyncapi.yaml` (AsyncAPI 3.0), porque OpenAPI no describe WebSockets. | Polling REST cada N segundos: todo HTTP y más simple, pero menos fiel a una app de inversión. |
| Q3 | **Gráfico:** no hay librería de gráficos. | Dibujado propio con el Canvas de Compose, sin dependencia. | Vico, una librería de gráficos para Compose. |
| Q4 | **Pendientes de la fase 1:** los `.webp` del launcher que no se usan; Gradle 9.9.0, AGP 9.5.1 y navigation3 1.2.1. | Un bloque 0 de mantenimiento: borrar los `.webp` y subir las versiones con los PR de Dependabot cuando su CI esté en verde y la app arranque. | Dejarlos para después de la fase 3. |

### 1.2 Decisiones de diseño

| # | Decisión | Por qué |
|---|---|---|
| E1 | Ocho acciones ficticias, cotizadas en USD, que se pagan y cobran en Cuenta Dólares (ACC-003). | Sin marcas reales y sin cambio de moneda dentro de una orden. |
| E2 | Los precios cambian por ticks pregenerados con semilla (`api/tools/`), que el backend reproduce en orden a un intervalo del escenario. Con intervalo 0 los precios quedan congelados. | Los fixtures siguen siendo el único origen de datos, y los flows de testigo pueden fijar montos exactos. |
| E3 | Solo órdenes a mercado, por cantidad entera y con confirmación. Se ejecutan al precio del último tick del servidor. | Es lo habitual en apps de inversión minorista; las órdenes límite quedan fuera. |
| E4 | El cambio de divisas es entre cuentas propias, con una cotización que vence (`quoteId` con TTL). Si vence, el servidor responde 422 `QUOTE_EXPIRED` y la app ofrece la cotización nueva. | Así funcionan los bancos, y suma un modal realista que probar. |
| E5 | Biometría: tras el onboarding aparece "Ingresa con tu huella", con Activar y Omitir, solo si el dispositivo tiene una huella registrada. Activar registra el dispositivo y guarda un `deviceToken` cifrado con una clave del Keystore que exige biometría. En los arranques siguientes, Login ofrece "Ingresar con huella". | Ajusta D3 en un punto: lo único persistido es el `deviceToken` cifrado. El token de sesión sigue solo en memoria. |
| E6 | Coach marks la primera vez que se entra a Home: 3 pasos (Transferir, tab Mercado, botón de menú), con Siguiente y Omitir, una vez por instalación. | Otra capa que se interpone antes del contenido. |
| E7 | Menú lateral desde un botón en la barra de Inicio: Mi portafolio, Cambio, Seguridad, Ayuda (próximamente) y Cerrar sesión. | Duplica accesos, como en las apps reales. |
| E8 | Tres bottom sheets: selector de cuenta (órdenes y cambio), acciones del detalle de una acción (Comprar y Vender) y "Cómo calculamos el tipo de cambio". | Cubren tres usos distintos: elegir, actuar e informar. |
| E9 | (Opcional) Dos casos de rendimiento sembrados más, con nombres neutros (§5). | La fase trae dos focos naturales: listas que se actualizan con cada tick y gráficos. |

---

## 2. Pantallas y navegación

```
Entrada: Splash ──► Login [Ingresar con huella*] ──► Términos* ──► Onboarding* ──► Oferta de biometría* ──► Home [coach marks*]
                                                                                   (* solo si corresponde)
Home  [barra inferior; Inicio con botón de menú ──► Menú lateral]
├─ Inicio      (fase 1)
├─ Mercado ──► Detalle de acción ──► [sheet Comprar/Vender] ──► Orden: formulario ──► confirmación ──► resultado ──► Mercado
├─ Cambio  ──► confirmación ──► resultado ──► Cambio
└─ Más     (fase 1) + Seguridad
Menú lateral ──► Mi portafolio · Cambio · Seguridad · Ayuda (próximamente) · Cerrar sesión
```

| Pantalla | NavKey | Llamadas | Contenido |
|---|---|---|---|
| Oferta de biometría | `BiometricOfferNavKey` | `POST /auth/biometric/enroll` | Activar (abre el diálogo del sistema) y Omitir. |
| Login (cambia) | `LoginNavKey` | `POST /auth/biometric/login` | Botón "Ingresar con huella" si el dispositivo está registrado. |
| Mercado | tab de Home | `GET /market/stocks`, `GET /portfolio`, canal | Cabecera de Mi portafolio (valor y variación) y lista de acciones con precio y variación que se actualizan solos. Si el canal se cae, banner "Reconectando…". |
| Detalle de acción | `StockDetailNavKey(symbol)` | `GET /market/stocks/{symbol}`, `GET …/history`, canal | Precio en vivo, gráfico de línea con rangos 1D · 1S · 1M · 1A, datos del día y tenencia propia. Operar abre el sheet Comprar/Vender. |
| Orden · formulario | `OrderFormNavKey(symbol, side)` | cuentas (caché) | Cantidad, cuenta (sheet) y total estimado en vivo. Valida fondos (compra) o tenencia (venta). |
| Orden · confirmación | `OrderConfirmNavKey` | `POST /orders` | Resumen con "el precio final puede variar". Confirmar y Cancelar; `Idempotency-Key`. |
| Orden · resultado | `OrderResultNavKey` | — | Precio ejecutado, total, nuevo saldo y nueva tenencia; Volver a Mercado. |
| Cambio | tab de Home | `GET /fx/rates`, canal | Compra y venta en vivo; calculadora con monto, dirección PEN↔USD y cuentas de origen y destino (sheet); "Cómo calculamos" (sheet). |
| Cambio · confirmación | `FxConfirmNavKey` | `POST /fx/exchanges` | Resumen con la cotización y su vencimiento. Si vence, modal con la nueva. |
| Cambio · resultado | `FxResultNavKey` | — | Monto debitado, monto acreditado y nuevos saldos. |
| Mi portafolio | `PortfolioNavKey` | `GET /portfolio`, canal | Tenencias con valor de mercado en vivo y ganancia o pérdida. |
| Seguridad | `SecurityNavKey` | `DELETE /auth/biometric` | Interruptor "Ingresar con huella": activarlo vuelve a registrar el dispositivo; desactivarlo borra el `deviceToken`. |
| Menú lateral | drawer de Home | — | Mi portafolio, Cambio, Seguridad, Ayuda y Cerrar sesión. |
| Coach marks | capa sobre Home | — | 3 pasos con Siguiente, Omitir y Entendido. |

**Reglas**
- El canal en vivo es una sola conexión compartida, con suscripción por símbolo o par. Se abre solo mientras Mercado, Detalle, Mi portafolio o Cambio están visibles, y se cierra al salir o al pasar a segundo plano.
- Si el canal se cae, la app reconecta con espera creciente (1, 2 y 4 s, con tope de 8 s) y muestra el banner. Mientras tanto, los precios quedan en su último valor.
- Un 401 en el handshake o un cierre 4401 del servidor abren el modal global de sesión expirada.
- Una orden o un cambio invalidan la caché de cuentas (`AccountChanges`) y recargan el portafolio.
- Pila y modales siguen los patrones de la fase 1: progreso bloqueante, `modal_error` con Reintentar, modales específicos por código y "¿Salir sin…?" con datos.

---

## 3. Contrato

`api/openapi.yaml` se extiende con los endpoints REST; el canal se documenta en `api/asyncapi.yaml`. Errores, autenticación, montos, fechas e `Idempotency-Key` siguen las reglas de la fase 1.

| Método y ruta | Respuesta 2xx | Errores |
|---|---|---|
| `GET /market/stocks` | `items[]`: `symbol`, `name`, `currency`, `last`, `change`, `changePct`, `ts` | 401 |
| `GET /market/stocks/{symbol}` | lo anterior más `open`, `high`, `low`, `previousClose`, `volume`, `description` | 401, 404 |
| `GET /market/stocks/{symbol}/history?range=1D\|1W\|1M\|1Y` | `points[]`: `ts`, `price` | 401, 404, 422 |
| `GET /portfolio` | `holdings[]`: `symbol`, `quantity`, `averagePrice`; totales | 401 |
| `POST /orders` + `Idempotency-Key` | 201: `orderId`, `side`, `symbol`, `quantity`, `executedPrice`, `total`, `accountId`, `accountAvailableBalance`, `movementId` | 401, 422 (`VALIDATION_ERROR`, `INSUFFICIENT_FUNDS`, `INSUFFICIENT_HOLDINGS`), 5xx |
| `GET /fx/rates` | `pair` (USD/PEN), `buy`, `sell`, `quoteId`, `expiresAt` | 401 |
| `POST /fx/exchanges` + `Idempotency-Key` | 201: `exchangeId`, `rate`, `debited`, `credited`, saldos de origen y destino, `movementIds` | 401, 422 (`VALIDATION_ERROR`, `INSUFFICIENT_FUNDS`, `QUOTE_EXPIRED`, `CURRENCY_MISMATCH`), 5xx |
| `POST /auth/biometric/enroll` | 201: `deviceToken` | 401 |
| `POST /auth/biometric/login` | 200: igual que `/auth/login` | 401 `INVALID_DEVICE` |
| `DELETE /auth/biometric` | 204 | 401 |
| `WS /market/stream` (Bearer en el handshake) | El cliente envía `subscribe` y `unsubscribe` con `symbols` y `pairs`. El servidor envía `quote` (`symbol`, `last`, `change`, `changePct`, `ts`) y `fx` (`pair`, `buy`, `sell`, `quoteId`, `expiresAt`, `ts`), y cierra con 4401 si vence la sesión. | handshake 401 o 503 |

**Reglas de negocio mínimas, documentadas en el contrato**
- Compra: total = cantidad × último precio. Valida fondos en la cuenta USD, descuenta, suma la tenencia (recalcula el precio promedio) y crea un movimiento.
- Venta: valida la tenencia, acredita, resta la tenencia y crea un movimiento.
- Cambio: monto en la moneda de origen por la tasa de la cotización (compra si el banco compra dólares, venta si los vende). Debita y acredita, con un movimiento en cada cuenta. Responde `QUOTE_EXPIRED` si la cotización venció.
- Idempotencia igual que en las transferencias. Sin comisiones ni impuestos.
- El estado vive en memoria, como en la fase 1. `POST /_demo/reset` también devuelve tenencias y precios a su valor inicial.
- Excepción: el registro de dispositivos biométricos del backend embebido persiste entre arranques, solo en `demoTools`, porque E5 exige ingresar con huella tras un arranque en frío.

---

## 4. Backend embebido y escenarios

- El backend es el mismo de `demoTools`. El canal usa el upgrade a WebSocket de `mockwebserver3` y reproduce los ticks de los fixtures.
- Fixtures nuevos, generados con semilla por `api/tools/` y versionados: `market/stocks.json`, `market/ticks/<SYMBOL>.json`, `market/history/<SYMBOL>.json`, `portfolio.json` y `fx/rates.json` con sus ticks.
- Logcat (`CarteraBackend`) registra cada conexión, suscripción y cierre del canal, y los ticks enviados.
- Los ids de endpoint nuevos para latencia y fallos son `market_stocks`, `market_stock`, `market_history`, `market_stream` (handshake), `portfolio`, `orders`, `fx_rates`, `fx_exchanges`, `biometric_enroll`, `biometric_login` y `biometric_delete`. Las reglas de latencia y fallos de la fase 1 aplican igual.

| Parámetro nuevo | Valores | Por defecto | Cabecera | Extra de adb |
|---|---|---|---|---|
| Intervalo de ticks | ms; 0 = precios congelados | 2000 | `X-Demo-Tick-Ms` (handshake) | `--ei tick_ms 0` |
| Corte del canal | s desde la conexión; 0 = nunca | 0 | `X-Demo-Stream-Drop-S` (handshake) | `--ei stream_drop_s 15` |
| Vigencia de la cotización | s | 30 | `X-Demo-Quote-Ttl-S` (`GET /fx/rates` y canal) | `--ei quote_ttl_s 5` |

La pantalla Escenario y los argumentos de `launchApp` suman las mismas tres claves.

---

## 5. Casos sembrados (E9)

Igual que en la fase 1: apagados por defecto, release no los puede encender y nombres neutros según D4. El mapeo vive solo en este documento.

| # | Caso | Interruptor | Dónde se ve | Qué mide hb-performance | Camino correcto (interruptor apagado) |
|---|---|---|---|---|---|
| 4 | Cada tick reemplaza la lista entera con objetos nuevos, y la lista de Mercado se recompone completa. | `QUOTES_FULL_REFRESH` / `quotes_full_refresh` | Mercado con `tick_ms 250` | `frame_p90_ms` y `hilo_max_ms` en una ventana fija | Actualización por símbolo, con claves y tipos estables. |
| 5 | El gráfico reconstruye todo su trazado en cada frame y con cada tick. | `CHART_LIVE_PATH` / `chart_live_path` | Detalle de QORI en 1D (≈390 puntos) con ticks | `frame_p90_ms` y `hilo_max_ms` | Trazado recordado; solo se actualiza el último punto. |

---

## 6. Testabilidad

- Misma convención de testTags que en la fase 1. El diálogo de biometría es del sistema y no tiene testTags; en el emulador se resuelve con `adb -e emu finger touch 1`.

| Pantalla | Contenedor | Elementos principales | Marca |
|---|---|---|---|
| Oferta de biometría | `biometria_oferta` | `biometria_activar`, `biometria_omitir` | `biometria_oferta` |
| Login | `login_pantalla` | `login_huella` (nuevo) | `login` |
| Mercado | `mercado_lista` | `mercado_item_<symbol>`, `mercado_portafolio`, `mercado_reconectando` | `mercado` |
| Detalle de acción | `accion_detalle` | `accion_precio`, `accion_grafico`, `accion_rango_<1D\|1S\|1M\|1A>`, `accion_operar` | `accion_detalle` |
| Sheet Comprar/Vender | `sheet_accion` | `sheet_accion_comprar`, `sheet_accion_vender` | — |
| Orden · formulario | `orden_pantalla` | `orden_cantidad`, `orden_cuenta`, `orden_total`, `orden_continuar` | `orden_formulario` |
| Orden · confirmación | `orden_confirmacion` | `orden_confirmar`, `orden_cancelar` | `orden_confirmacion` |
| Orden · resultado | `orden_resultado` | `orden_resultado_precio`, `orden_resultado_saldo`, `orden_volver_mercado` | `orden_resultado` |
| Cambio | `cambio_pantalla` | `cambio_compra`, `cambio_venta`, `cambio_monto`, `cambio_direccion`, `cambio_origen`, `cambio_destino`, `cambio_como_calculamos`, `cambio_continuar` | `cambio` |
| Sheet de cuentas | `sheet_cuentas` | `sheet_cuentas_item_<id>` | — |
| Cambio · confirmación | `cambio_confirmacion` | `cambio_confirmar`, `cambio_cancelar`, `modal_cotizacion_vencida` | `cambio_confirmacion` |
| Cambio · resultado | `cambio_resultado` | `cambio_resultado_debitado`, `cambio_resultado_acreditado`, `cambio_volver` | `cambio_resultado` |
| Mi portafolio | `portafolio_lista` | `portafolio_item_<symbol>` | `portafolio` |
| Menú lateral | `menu_lateral` | `home_menu` (botón), `menu_portafolio`, `menu_cambio`, `menu_seguridad`, `menu_ayuda`, `menu_cerrar_sesion` | — |
| Seguridad | `seguridad_pantalla` | `seguridad_huella` | `seguridad` |
| Coach marks | `coach_capa` | `coach_paso_<n>`, `coach_siguiente`, `coach_omitir` | — |

**Identificadores que también existen en el código** (se suman a la tabla anterior; los nombres salen de los `*TestTags` y de los helpers de `core/designsystem/component/`)

| Pantalla | Identificadores |
|---|---|
| Más | `mas_seguridad` (entrada a Seguridad) |
| Ayuda | `ayuda_proximamente` |
| Mercado, Detalle, Orden, Cambio y Mi portafolio | `accion_reconectando`, `orden_reconectando`, `cambio_reconectando`, `portafolio_reconectando`: el aviso "Reconectando…" de cada pantalla con precios, además de `mercado_reconectando` |
| Detalle de acción | `accion_tenencia` (cabecera de la tenencia), `sheet_accion_vender_no_disponible` (Vender cuando no hay tenencia) |
| Orden · formulario | `orden_accion`, `orden_lado`, `orden_precio`; los errores de campo son `<campo>_error` (`orden_cantidad_error`, `orden_cuenta_error`...) |
| Orden · resultado | `orden_resultado_operacion`, `orden_resultado_total`, `orden_resultado_tenencia`, `orden_resultado_error` |
| Orden · rechazos (modal, con `_aceptar`) | `modal_fondos_insuficientes`, `modal_tenencia_insuficiente`, `modal_orden_rechazada` |
| Cambio | `cambio_estimado`, `sheet_como_calculamos` (con `sheet_como_calculamos_entendido` para cerrarlo) |
| Cambio · confirmación | `cambio_confirmacion_debito`, `cambio_confirmacion_credito`, `cambio_confirmacion_tasa`, `cambio_vencimiento` |
| Cambio · cotización vencida | `modal_cotizacion_vencida` con `modal_cotizacion_vencida_aceptar` y `modal_cotizacion_vencida_salir` |
| Cambio · rechazos (modal, con `_aceptar`) | `modal_fondos_insuficientes`, `modal_moneda_distinta`, `modal_cambio_rechazado` |
| Cambio · resultado | `cambio_resultado_operacion`, `cambio_resultado_tasa`, `cambio_resultado_saldo_origen`, `cambio_resultado_saldo_destino`, `cambio_resultado_error` |
| Transferir | `transferir_resultado_error`; modales `modal_destino_inexistente` y `modal_transferencia_rechazada` (con `_aceptar`) |
| Modales comunes | `modal_sesion_expirada_aceptar`, `modal_error_reintentar`, `modal_error_cancelar`, `modal_confirmacion_aceptar`, `modal_confirmacion_cancelar` |

- Los estados genéricos de una pantalla siguen la forma `<pantalla>_cargando`, `<pantalla>_error`, `<pantalla>_reintentar`, `<pantalla>_vacio` y `<pantalla>_proximamente` (`CommonTestTags`).
- Un diálogo o un sheet vive en otra ventana: su contenedor y sus botones llevan el testTag como resource-id por su cuenta.

**Datos de referencia** (definitivos para los flows de testigo; si alguno cambia, cambia primero aquí)

| Acción | Empresa | Precio inicial (US$) |
|---|---|---|
| ANDM | Andina Minerals | 42.10 |
| CUSC | Cusco Telecom | 27.30 |
| INKA | Inka Energy | 63.40 |
| LIMA | Lima Logistics | 9.85 |
| MOCH | Moche Tech | 88.60 |
| NAZC | Nazca Retail | 12.45 |
| PCFC | Pacífico Foods | 18.75 |
| QORI | Qori Gold | 115.20 |

- **Portafolio de Ana:** ANDM 10 a 40.00; MOCH 5 a 80.00; QORI 2 a 120.00.
- **Tipo de cambio inicial USD/PEN:** compra 3.742, venta 3.768.
- **Orden de referencia** (`tick_ms 0`): comprar 2 ANDM cuesta US$ 84.20. ACC-003 pasa de US$ 1,050.20 a US$ 966.00, y ANDM queda en 12 a 40.35.
- **Venta de referencia** (`tick_ms 0`, después de la compra): vender 1 QORI acredita US$ 115.20. ACC-003 queda en US$ 1,081.20 y QORI en 1.
- **Cambio de referencia** (`tick_ms 0`, desde datos limpios): US$ 100.00 de ACC-003 a ACC-002 a 3.742 acredita S/ 374.20. ACC-003 queda en US$ 950.20 y ACC-002 en S/ 2,674.20.
- **Casos negativos:**
  - Como en la fase 1, el formulario frena los fondos insuficientes y la venta de más acciones de las que se tienen; los 422 del servidor se cubren con tests.
  - `QUOTE_EXPIRED` sí se alcanza: con `quote_ttl_s 5`, esperar más de 5 s en la confirmación del cambio abre `modal_cotizacion_vencida` con la cotización nueva.

---

## 7. Fuera de alcance

- **Fase 3:** módulo legacy Fragment + XML + RxJava, sus pantallas sin ids y el contrato listo para un servidor externo.
- **No entran en la fase 2:** órdenes límite o stop, horario de mercado, comisiones, impuestos, dividendos, noticias, alertas de precio, búsqueda y watchlist editable, notificaciones push, ingreso con rostro o PIN, cierre por inactividad, diseño visual, flows de testigo e instrumentación de hb-performance.

---

## 8. Bloques de implementación

Igual que en la fase 1: un chat de hb-implement por bloque. Cada bloque termina con el DoD en verde, tests de la lógica nueva, goldens de cada `*Content` nuevo en claro y oscuro, y su lista de verificación en el dispositivo. Al final, una sesión con `/goal` verifica toda la fase en el emulador, en debug y en demo, y cierra con push y CI en verde.

| Bloque | Contenido | Tamaño |
|---|---|---|
| 0 | Mantenimiento (Q4). | S |
| f | Contrato (OpenAPI y AsyncAPI), fixtures y generador; backend: REST de mercado, portafolio, órdenes, cambio y biometría, canal WebSocket con ticks y los escenarios nuevos. | L (si se alarga: f1 REST, f2 canal) |
| g | Cliente del canal (una conexión, suscripciones, reconexión y ciclo de vida), Mercado, Mi portafolio y Detalle de acción con el gráfico. | L |
| h | Órdenes: sheets, formulario, confirmación y resultado; invalidación de cuentas y portafolio. | M |
| i | Cambio: calculadora en vivo, cotización con vencimiento, confirmación y resultado. | M |
| j | Biometría (oferta, registro, ingreso con huella y Seguridad), coach marks y menú lateral. | L |
| k | Casos sembrados 4 y 5 (E9). | S |
