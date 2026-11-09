# Changelog del contrato de la API

> Cambios de `openapi.yaml` y `asyncapi.yaml`. Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/)
> y versionado con [SemVer](https://semver.org/lang/es/). El changelog de la app está en [`../CHANGELOG.md`](../CHANGELOG.md).

## Cómo se versiona el contrato

La versión está en `info.version` de `openapi.yaml` y de `asyncapi.yaml`, y las dos suben juntas. Un servidor que
implementa la versión `X.Y.Z` es compatible con cualquier cliente escrito para `X.Y'.Z'` mientras `Y' <= Y`.

- **MAJOR.** Rompe a los clientes que ya funcionaban. Se quita o se renombra un endpoint, un parámetro, un campo o un
  código de error; cambia el tipo, el formato o la obligatoriedad de un campo de la respuesta; un parámetro o un campo
  de la petición pasa a ser obligatorio; o una regla de negocio cambia de modo que una petición que era válida deja de
  serlo. También cuenta cambiar la autenticación (algoritmo, claims o TTL por defecto) o la semántica de la
  idempotencia y de los cursores.
- **MINOR.** Agrega sin romper. Un endpoint, un parámetro opcional, un campo opcional de petición o de respuesta, un
  código de error, un valor de un enum de petición o un mensaje nuevo del canal en vivo. Los clientes deben ignorar los
  campos y los códigos que no conocen.
- **PATCH.** No cambia lo que un servidor debe hacer: textos, descripciones, ejemplos, correcciones de erratas y
  restricciones de formato que ya cumplía todo el que implementaba bien la versión anterior.

Mientras no haya un servidor desplegado, el backend embebido de la app (`app/src/demoTools/`) es la implementación de
referencia: un cambio del contrato cambia juntos el YAML, los fixtures de `fixtures/` y ese backend.

## [Unreleased]

## [1.0.0] - 2026-11-08

Primera versión estable: el contrato completo que la app usa contra el backend embebido y que cualquier servidor
externo (NestJS, Spring o Node) puede implementar. Lo que trae, por lo que se fue añadiendo:

### Added

- **Convenciones.** JSON en camelCase, ids como texto, fechas ISO 8601 con zona, montos como texto decimal con moneda
  ISO 4217 (`Money`), `X-Request-Id` en toda respuesta, errores RFC 9457 (`application/problem+json`, con `code`,
  `traceId` y `errors[]` por campo) y paginación por cursor.
- **Servidores.** El backend embebido (`http://127.0.0.1:8765/api/v1`) y un servidor de ejemplo sin desplegar
  (`https://api.cartera.example/api/v1`).
- **Autenticación y cuentas.** `GET /app/config`, `POST /auth/login` (JWT HS256 con `sub`, `iat` y `exp`, TTL por
  defecto de 900 s y sin refresh token), `GET /me`, `GET /accounts` y `GET /accounts/{accountId}`. Códigos
  `INVALID_CREDENTIALS`, `SESSION_EXPIRED`, `UNAUTHORIZED` y `NOT_FOUND`.
- **Movimientos.** `GET /movements` (de una cuenta o de todas, con `cursor` y `limit` de 1 a 1000, 20 por defecto),
  `GET /movements/{movementId}` y `GET /movements/{movementId}/breakdown`.
- **Transferencias.** `POST /transfers` con `Idempotency-Key` (por usuario y operación) y los códigos
  `VALIDATION_ERROR`, `DESTINATION_NOT_FOUND`, `CURRENCY_MISMATCH` e `INSUFFICIENT_FUNDS`.
- **Mercado, portafolio, órdenes y cambio.** `GET /market/stocks`, `GET /market/stocks/{symbol}` y
  `GET /market/stocks/{symbol}/history` (rangos `1D`, `1W`, `1M` y `1Y`); `GET /portfolio`; `POST /orders` a mercado
  (`INSUFFICIENT_HOLDINGS`); `GET /fx/rates` y `POST /fx/exchanges` con cotizaciones que vencen (`QUOTE_EXPIRED`, con la
  cotización nueva en `quote`).
- **Canal en vivo.** `asyncapi.yaml` describe `WS /market/stream`: handshake con el mismo Bearer, `subscribe` y
  `unsubscribe` por símbolo y por par, mensajes `quote` y `fx` del reloj de mercado y cierre 4401 al vencer la sesión.
- **Biometría.** `POST /auth/biometric/enroll`, `POST /auth/biometric/login` y `DELETE /auth/biometric`, con el código
  `INVALID_DEVICE`.
- **Pago de servicios.** `GET /billers` (con `query`), `GET /billers/{billerId}/debts` (con `customerCode` y, opcional,
  `includeHistory` para el histórico de facturación de los últimos 24 meses), `POST /bill-payments` con
  `Idempotency-Key` y clave de operaciones, y `GET /bill-payments/{paymentId}/voucher` (`text/html`). Códigos
  `BILLER_NOT_FOUND`, `CUSTOMER_NOT_FOUND`, `PAYMENT_NOT_FOUND`, `INVALID_PIN` y `DEBT_ALREADY_PAID`.
- **Solo demo.** Cabeceras `X-Demo-*` (latencia, fallos, TTL de la sesión y de las cotizaciones, ticks y corte del
  canal) y `POST /_demo/reset`. Un servidor real no las implementa.
- **Ejemplos** con nombre en cada respuesta de éxito y en las de error documentadas, tomados de los fixtures: un mock
  generado solo desde el contrato (Prism) devuelve los mismos datos que el backend embebido.
- **Metadatos.** Contacto y licencia, y una descripción en cada operación.
- **Validación.** Lint de los dos archivos con Spectral (`../.spectral.yaml`) y tests de la app que validan las
  respuestas del backend embebido y los ejemplos contra los esquemas.
