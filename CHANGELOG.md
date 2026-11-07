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
