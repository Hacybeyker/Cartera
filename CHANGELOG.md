# Changelog — Cartera

> Todos los cambios notables de este proyecto están documentados aquí.
> Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

---

## [Unreleased]

### Added
- Proyecto inicializado a partir de [ScaffoldingAndroidCompose](https://github.com/hacybeyker/ScaffoldingAndroidCompose)
- Documento de alcance de la fase 1 (`docs/ALCANCE-FASE-1.md`).
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

### Changed
- El desglose usa un Crossfade entre sus estados (carga, error y contenido).
- Inicio y Movimientos vuelven a pedir sus datos al mostrarse solo si una transferencia los cambió; Inicio refresca solo las cuentas.
- `ApiCaller` ya no trata cualquier `IllegalArgumentException` como respuesta inválida, solo los errores de lectura de la respuesta.
- Las marcas de contenido visible avisan una vez por cada carga o refresco, no una vez por pantalla.
- `hilt-lifecycle-viewmodel-compose` se declara en el catálogo en vez de llegar transitiva.

### Removed
- `hilt-navigation-compose`, que ningún import usaba.

### Fixed
- Tras un error del servidor al confirmar una transferencia, volver al inicio vuelve a pedir las cuentas: el cobro no está descartado.
- Un 401 sin cuerpo RFC 9457 también abre el modal de sesión expirada.
- Un monto o una fecha mal formados en una respuesta se muestran como error del servicio en vez de cerrar la app.

### Security
- Configuración de seguridad de red: sin tráfico en claro, salvo hacia `127.0.0.1` en debug y demo.

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
