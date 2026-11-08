# Changelog — Cartera

> Todos los cambios notables de este proyecto están documentados aquí.
> Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

---

## [Unreleased]

### Added
- Proyecto inicializado a partir de [ScaffoldingAndroidCompose](https://github.com/hacybeyker/ScaffoldingAndroidCompose)
- Documento de alcance de la fase 1 (`docs/ALCANCE-FASE-1.md`).
- Documento de alcance de la fase 2 (`docs/ALCANCE-FASE-2.md`).
- Licencia MIT.
- Contrato de la API en OpenAPI 3.1 (`api/openapi.yaml`), con errores RFC 9457 y la regla de transferencia.
- Fixtures de la usuaria de prueba, sus tres cuentas y sus movimientos, con un generador de semilla fija (`api/tools/`).
- Backend embebido para debug y demo: login con JWT, cuentas, movimientos paginados por cursor, detalle, desglose, transferencias con `Idempotency-Key` y `POST /_demo/reset`.
- Escenarios de red por cabecera en el backend embebido: latencia, tasa y tipo de fallo, TTL de la sesión y backend caído.
- Variante `demo`: hereda de release, es `profileable` y se firma con la clave debug.
- Dependencias de red: Retrofit 3, OkHttp 5 y `kotlinx-serialization-json`.
- Cliente HTTP de la API: servicios Retrofit, DTO del contrato, errores RFC 9457 tipados, token Bearer en memoria y `BASE_URL` y timeouts en BuildConfig.
- Escenarios de demo controlables por adb (`SET_SCENARIO`, `RESET_DATA`) y por los argumentos de `launchApp` de Maestro, guardados en el dispositivo.
- README con el usuario de prueba y cómo cambiar los escenarios.
- Splash que pide la configuración de la app, con Reintentar ante un error o el backend caído.
- Login con usuario y contraseña (mostrar u ocultar): credenciales incorrectas bajo el formulario y errores de red en un modal.
- Términos y condiciones, que reaparecen cuando cambia su versión, y onboarding de tres páginas que se ve una vez por instalación.
- Sesión en memoria y modal "Tu sesión expiró" ante cualquier llamada autenticada que responda `SESSION_EXPIRED` o `UNAUTHORIZED`.
- Estados y modales comunes (carga, error con Reintentar, progreso bloqueante, error y confirmación) con testTags expuestos como `resource-id`.
- Punto de extensión para las marcas de contenido visible de cada pantalla.
- Home con barra inferior Inicio · Mercado · Cambio · Más, donde cada tab conserva su estado.
- Inicio con saludo y tabs Cuentas · Movimientos: saldo por cuenta y movimientos de todas las cuentas, paginados por cursor.
- Movimientos de cada cuenta, detalle del movimiento y desglose con su ilustración de categoría.
- Paginación con fila de Reintentar cuando falla la carga de más, sin perder lo ya cargado.
- Más: nombre, Servicios clásicos (próximamente), Cerrar sesión con confirmación y Versión.
- Pantalla Escenario en debug y demo, oculta tras 7 toques en Versión, con Guardar, Valores por defecto y Restablecer datos.
- Transferencia: formulario con validación en el cliente, confirmación con progreso bloqueante y resultado con el n.º de operación y el nuevo saldo.
- Reintentar una transferencia reenvía la misma `Idempotency-Key`, así nunca se cobra dos veces.
- Modales propios para saldo insuficiente, destino inexistente y monedas distintas, y errores de validación por campo.
- Confirmaciones para salir del formulario con datos y para cancelar en la confirmación.
- Interruptores del cliente, apagados al inicio y con el mismo nombre en BuildConfig, en el escenario, adb, launchApp y la pantalla Escenario: página completa de movimientos (`movements_full_page`), refresco anticipado de cuentas (`accounts_eager_refresh`) y transición enriquecida del desglose (`breakdown_rich_transition`). Release no los puede encender.
- Ilustraciones de categoría en `drawable-nodpi`, para la transición enriquecida del desglose.
- Caché de cuentas en memoria: el formulario la reutiliza y una transferencia la invalida, así al volver al inicio se piden las cuentas una sola vez.
- Contrato de mercado, portafolio, órdenes, cambio de divisas e ingreso con huella en `api/openapi.yaml`, con sus reglas de negocio, los códigos `INSUFFICIENT_HOLDINGS`, `QUOTE_EXPIRED` e `INVALID_DEVICE` y las cabeceras `X-Demo-Tick-Ms`, `X-Demo-Stream-Drop-S` y `X-Demo-Quote-Ttl-S`.
- Fixtures de mercado con un generador de semilla fija (`api/tools/generate_market.py`): ocho acciones, sus ticks e historiales (1D, 1W, 1M y 1Y), el portafolio de Ana y el tipo de cambio USD/PEN con sus ticks.
- Backend embebido: acciones con precio vigente, detalle e historial; portafolio valorizado; órdenes de compra y venta a mercado en la cuenta en dólares con `Idempotency-Key`; cotizaciones que vencen y cambio entre cuentas propias; registro, ingreso y revocación de huella.
- Escenario: intervalo de ticks (`tick_ms`), corte del canal (`stream_drop_s`) y vigencia de la cotización (`quote_ttl_s`) por adb, launchApp y la pantalla Escenario.
- Canal en vivo `WS /market/stream` en el backend embebido, con su contrato en `api/asyncapi.yaml` (AsyncAPI 3.0): suscripción por símbolo y por par con el valor actual al suscribirse, `quote` y `fx` en cada tick de un reloj de mercado global, cierre 4401 al vencer la sesión, corte de demo con `stream_drop_s` y logs de apertura, suscripción, ticks, corte y cierre en `CarteraBackend`.
- Tab Mercado: cabecera de Mi portafolio con su valor y la variación del día, y las ocho acciones con precio y variación que se actualizan solos por el canal en vivo, fila por fila. Si el canal se cae, aviso "Reconectando…" y los precios quedan en su último valor.
- Cliente del canal en vivo: una sola conexión compartida con suscripciones por símbolo y por par, abierta solo mientras una pantalla con precios se ve, reconexión con espera de 1, 2, 4 y 8 s y modal de sesión expirada ante un 401 en el handshake o un cierre 4401.
- Datos de mercado y portafolio en la app (acciones, detalle, historial por rango y portafolio), con el portafolio recalculado en el cliente con los precios en vivo.
- Detalle de acción: precio en vivo con su variación, gráfico de línea dibujado con el Canvas de Compose y rangos 1D, 1S, 1M y 1A, mínimo y máximo de la serie, datos del día y la tenencia propia con su ganancia o pérdida en vivo. En 1D, el último punto del gráfico sigue al precio.
- Mi portafolio: totales y cada tenencia con cantidad, precio promedio, precio actual, valor de mercado y ganancia o pérdida en vivo; tocar una tenencia abre su detalle.
- Órdenes de compra y venta: Operar en el Detalle de acción abre un bottom sheet con Comprar y Vender (Vender solo con tenencia); formulario con cantidad, cuenta en dólares elegida en un bottom sheet y total estimado en vivo; confirmación con "El precio final puede variar" y progreso bloqueante; resultado con precio ejecutado, total, nuevo saldo y nueva tenencia, y Volver a Mercado.
- Validación de la orden en el cliente (cantidad entera, saldo en una compra y tenencia en una venta), `Idempotency-Key` reutilizada en cada Reintentar y modales propios para saldo y acciones insuficientes.
- Bottom sheet de cuentas reutilizable (`sheet_cuentas`), con alias, número enmascarado y saldo disponible.
- Categoría de movimientos Inversiones (`INVESTMENT`) para los movimientos de las órdenes.
- Tab Cambio: compra y venta de USD/PEN en vivo por el canal, calculadora con monto, sentido (de dólares a soles o al revés), cuentas de origen y destino en el bottom sheet de cuentas (solo las de su moneda) y lo que se recibe en vivo, con la misma regla y el mismo redondeo que el servidor; aviso "Reconectando…" si el canal se cae.
- Bottom sheet "Cómo calculamos el tipo de cambio", con la compra, la venta, cuál se aplica en cada sentido y un ejemplo con la cotización de ahora.
- Confirmación del cambio con la cotización pedida al continuar y su cuenta regresiva; al vencer (por el reloj o por un `QUOTE_EXPIRED` del servidor) ofrece la cotización nueva en un modal, y usarla reinicia la cuenta regresiva con otra `Idempotency-Key`.
- Resultado del cambio con monto debitado, monto acreditado, tipo de cambio y los nuevos saldos de las dos cuentas, y Volver a Cambio; "No pudimos confirmar el cambio" si terminó sin respuesta.
- Validación del cambio en el cliente (monto, saldo, un centavo como mínimo y cuentas de las monedas del sentido), `Idempotency-Key` reutilizada en cada Reintentar y modales propios para saldo insuficiente y monedas que no corresponden.
- Ingreso con huella (`androidx.biometric` 1.1.0): oferta "Ingresa con tu huella" con Activar y Omitir tras el onboarding, una vez por instalación y solo si el dispositivo tiene una huella registrada y no está registrado.
- Registro del dispositivo: el diálogo de huella del sistema autoriza una clave AES del Android Keystore que exige la huella en cada uso y se invalida con una huella nueva; se guarda solo el `deviceToken` cifrado. Cancelar el diálogo no llama al servidor ni guarda nada.
- "Ingresar con huella" en Login cuando el dispositivo está registrado: la misma sesión en memoria que el login con clave, con Términos y Onboarding si corresponden. Si el servidor ya no reconoce el dispositivo (`INVALID_DEVICE`) o se registró una huella nueva, se borra lo guardado, se oculta el botón y se explica bajo el formulario.
- El backend embebido guarda los dispositivos registrados para ingresar con huella en el dispositivo, así el ingreso con huella funciona tras un arranque en frío; RESET_DATA y `DELETE /auth/biometric` también los borran.
- Pantalla Seguridad, desde Más, con el interruptor "Ingresar con huella": activarlo registra el dispositivo y desactivarlo lo revoca (`DELETE /auth/biometric`) y borra el token y la clave; sin huella en el dispositivo queda deshabilitado con su explicación.
- Menú lateral en Home, abierto con el botón de la barra de Inicio: Mi portafolio, Cambio (elige el tab), Seguridad, Ayuda (próximamente) y Cerrar sesión con la misma confirmación que Más. Se cierra deslizando, tocando fuera o con Atrás.
- Coach marks la primera vez que se entra a Home: tres pasos (Transferir, el tab Mercado y el botón del menú) que resaltan cada elemento, con Siguiente, Entendido en el último y Omitir. Se ven una vez por instalación.

### Changed
- El desglose usa un Crossfade entre sus estados (carga, error y contenido).
- Inicio y Movimientos vuelven a pedir sus datos al mostrarse solo si una transferencia los cambió; Inicio refresca solo las cuentas.
- El tab Mercado deja de ser "Próximamente"; tocar una acción o Mi portafolio lleva al Detalle de acción y a Mi portafolio.
- El aviso "Reconectando…" y la fila etiqueta · valor pasan a componentes compartidos (`core/ui/market/`, `core/designsystem/component/`).
- `ApiCaller` ya no trata cualquier `IllegalArgumentException` como respuesta inválida, solo los errores de lectura de la respuesta.
- Las marcas de contenido visible avisan una vez por cada carga o refresco, no una vez por pantalla.
- `hilt-lifecycle-viewmodel-compose` se declara en el catálogo en vez de llegar transitiva.
- Gradle 9.9.0, AGP 9.5.1 y Navigation 3 1.2.1.
- Login avisa su marca `login` cuando ya sabe si muestra "Ingresar con huella".
- Tras el onboarding (o tras el login, si ya se vio) puede seguir la oferta de ingreso con huella en vez de Home.
- `MainActivity` pasa a ser una `FragmentActivity`, que `BiometricPrompt` exige.
- El tab Más usa el ícono de tres puntos, para no repetir el del botón del menú lateral.
- Al volver a Home en un tab pedido (tras una orden o un cambio), ese tab se ve desde el primer frame.
- `POST /_demo/reset` también devuelve tenencias, precios, cotizaciones y dispositivos registrados a su estado inicial; con el canal en vivo abierto, no lo cierra y le envía los valores iniciales de lo suscrito.
- La `Idempotency-Key` vale por usuario y por operación: la misma clave en una transferencia y en una orden no se cruza.
- Una orden confirmada, o que terminó sin respuesta o con un error del servidor, invalida las cuentas y el portafolio: Mercado, Mi portafolio y el Detalle de acción vuelven a pedir solo el portafolio al mostrarse, e Inicio solo las cuentas.
- El generador de `Idempotency-Key` pasa a `core/` para compartirlo entre transferencias y órdenes.
- El tab Cambio deja de ser "Próximamente".
- Un cambio confirmado, o que terminó sin respuesta o con un error del servidor, invalida las cuentas: Cambio e Inicio vuelven a pedirlas al mostrarse.

### Removed
- `hilt-navigation-compose`, que ningún import usaba.
- Los íconos `.webp` del launcher por densidad (`mipmap-*dpi`): con minSdk 26 se usa siempre el ícono adaptativo.

### Fixed
- Tras un error del servidor al confirmar una transferencia, volver al inicio vuelve a pedir las cuentas: el cobro no está descartado.
- Un 401 sin cuerpo RFC 9457 también abre el modal de sesión expirada.
- Un monto o una fecha mal formados en una respuesta se muestran como error del servicio en vez de cerrar la app.

### Security
- Configuración de seguridad de red: sin tráfico en claro, salvo hacia `127.0.0.1` en debug y demo.
- El `deviceToken` cifrado del ingreso con huella queda fuera de los backups y de la transferencia entre dispositivos; el token de sesión sigue solo en memoria y cerrar sesión no borra el `deviceToken`.

---

<!-- Ejemplo de entrada:

## [1.0.0] — 2026-11-07

### Added
- Feature X implementada por capas dentro de la feature (domain → data → ui)

### Changed
- Refactor de Z para mejorar legibilidad

### Removed
- Eliminado código obsoleto de A

### Fixed
- Bug Y corregido en la feature Z

-->

[Unreleased]: https://github.com/Hacybeyker/Cartera/compare/v1.0.0...HEAD
