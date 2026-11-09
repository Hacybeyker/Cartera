# Guía del contrato: cómo implementar el servidor de Cartera

Esta guía es para quien implemente el servidor de Cartera en NestJS, Spring o Node (o lo que sea) y quiera que la app
funcione contra él cambiando solo `BASE_URL`. Describe lo que el backend embebido de la app hace hoy, que es la
implementación de referencia: lo que el contrato (`api/openapi.yaml`) deja abierto, aquí queda fijado con el
comportamiento real.

| Archivo | Qué es |
|---|---|
| [`api/openapi.yaml`](../api/openapi.yaml) | El contrato REST (OpenAPI 3.1): rutas, esquemas, errores y ejemplos con nombre. |
| [`api/asyncapi.yaml`](../api/asyncapi.yaml) | El canal en vivo `WS /market/stream` (AsyncAPI 3.1). |
| [`api/CHANGELOG.md`](../api/CHANGELOG.md) | Versión del contrato (SemVer) y qué cambió en cada una. |
| [`api/fixtures/`](../api/fixtures/) | Los datos de la usuaria de prueba: la semilla del servidor (ver [Fixtures como semilla](#fixtures-como-semilla)). |
| `app/src/demoTools/java/com/hacybeyker/cartera/demo/backend/` | El backend embebido (Kotlin): `BackendRouter`, una mesa por grupo de endpoints, `Problems`, `BackendState` y `JwtSigner`. |

La versión del contrato vigente es la `1.0.0`. Un servidor que la implementa funciona con la app tal como está.

## Convenciones

- Base: `/api/v1`. Toda ruta de esta guía va detrás de ese prefijo (`POST /api/v1/auth/login`). La app lo toma de
  `BASE_URL`, que termina en `/`.
- JSON en camelCase, UTF-8, ids como texto. Las fechas son ISO 8601 con zona (`2026-11-07T10:00:00-05:00`; el backend
  embebido usa siempre `-05:00`, truncadas al segundo).
- Dinero: `Money` = `{ "value": "150.00", "currency": "PEN" }`. El valor es texto decimal con **dos** decimales (negativo
  en los débitos de los movimientos). Los precios de mercado, los porcentajes y el tipo de cambio son texto decimal sin
  moneda propia (`"42.10"`, `"1.25"` = 1,25 %, `"3.742"` con tres decimales); la moneda va en `currency`. Cálculos con
  decimales exactos (`BigDecimal`, `decimal.js`), nunca con `float`, y redondeo **mitad hacia arriba** a 2 decimales.
- Toda respuesta trae `X-Request-Id`: el que mandó el cliente o uno generado (`req-000001`, un contador). En los errores,
  `traceId` es el mismo valor.
- Los clientes ignoran los campos y los códigos que no conocen (por eso agregar uno es un cambio MINOR del contrato);
  el servidor, en cambio, **no debe enviar campos que el contrato no declara**: un test de la app lo comprueba contra el
  backend embebido.
- Las rutas desconocidas, y un método que no corresponde a una ruta, responden 404 `NOT_FOUND` con cuerpo RFC 9457,
  antes de mirar el token. No hay 405.
- El servidor no negocia contenido: siempre responde JSON (`application/json; charset=utf-8`), salvo los errores
  (`application/problem+json; charset=utf-8`) y la constancia (`text/html; charset=utf-8`). Un cuerpo de petición
  vacío cuenta como `{}`; uno que no es un objeto JSON da 422 `VALIDATION_ERROR` en el campo `body` (`INVALID_JSON`).

## Autenticación (JWT)

`Authorization: Bearer <JWT>` en todo menos en `GET /app/config`, `POST /auth/login`, `POST /auth/biometric/login` y
`POST /_demo/reset`.

**Emisión.** `POST /auth/login` con `{ "username", "password" }` (los dos obligatorios; si faltan o vienen vacíos, 422
`VALIDATION_ERROR` con un elemento `REQUIRED` por campo). Un par que no coincide da 401 `INVALID_CREDENTIALS` sin decir
cuál falló. Responde `{ "accessToken", "tokenType": "Bearer", "expiresIn": 900 }`. No hay refresh token: al vencer, la
persona vuelve a iniciar sesión (con clave o con huella).

**El token.**

| Dato | Valor |
|---|---|
| Algoritmo | HS256 (HMAC-SHA256). Sin `kid`. |
| Cabecera | `{"alg":"HS256","typ":"JWT"}` |
| Claims | `sub` (id de la persona, `USR-001`), `iat` y `exp` (segundos desde la época). Sin `iss`, `aud` ni `jti`. |
| Vida | `exp = iat + expiresIn`. Por defecto 900 s. |
| Codificación | Base64url sin relleno, las tres partes. |
| Clave | En la demo es pública a propósito (`cartera-demo-hs256-key-2026-not-a-secret`). |

> **Recomendación.** En un servidor real la clave sale de un secreto (variable de entorno o gestor de secretos), de al
> menos 256 bits, y se puede rotar. La app trata el token como opaco: no lo decodifica, solo lo reenvía.

**Verificación**, en este orden, en cada llamada autenticada:

1. Si falta la cabecera, el token no tiene tres partes o la firma no coincide (comparación en tiempo constante), o el
   payload no trae `sub` y `exp`: 401 `UNAUTHORIZED`. (El backend embebido quita el prefijo `Bearer ` si está y no
   lo exige; un servidor real debe exigirlo.)
2. Si `ahora >= exp`: 401 `SESSION_EXPIRED` (la app muestra "Tu sesión expiró" y deja la pila en Login).
3. Si `sub` ya no existe: 401 `UNAUTHORIZED`.

La app abre el mismo modal ante `SESSION_EXPIRED`, `UNAUTHORIZED` y un 401 sin cuerpo RFC 9457, así que nunca devuelvas
un 401 por otro motivo (un permiso insuficiente, por ejemplo, es 403 o 404). Todo lo que es de una persona se filtra por
`sub`: un recurso ajeno responde 404 `NOT_FOUND` (o 422 en el campo que lo nombra), como si no existiera.

**Ingreso con huella.** `POST /auth/biometric/enroll` (autenticado, sin cuerpo) registra el dispositivo y devuelve un
`deviceToken` opaco (201); el backend embebido genera `dvc_` + 32 bytes aleatorios en base64url. Cada registro crea uno
nuevo y los anteriores siguen vigentes. `POST /auth/biometric/login` con `{ "deviceToken" }` responde igual que el
login, con el mismo TTL; un token ausente da 422 `REQUIRED` y uno desconocido o revocado, 401 `INVALID_DEVICE`.
`DELETE /auth/biometric` revoca **todos** los dispositivos de la persona (204). El servidor recuerda los dispositivos
también tras reiniciarse; la app guarda el `deviceToken` cifrado con una clave del Android Keystore.

## Errores (RFC 9457)

Todo error es `application/problem+json` con estos campos:

| Campo | Tipo | Valor |
|---|---|---|
| `type` | URI | `https://cartera.invalid/problems/<code en kebab-case>` (`insufficient-funds`). Es un identificador, no se resuelve. |
| `title` | texto | Fijo por código, en español (tabla de abajo). |
| `status` | entero | El estado HTTP. |
| `detail` | texto | Mensaje en español para la persona; cambia según el caso. |
| `code` | enum | Lo único que la app mira para decidir qué mostrar. |
| `traceId` | texto | Igual a `X-Request-Id`. |
| `errors` | lista | Solo en `VALIDATION_ERROR`: `{ "field", "code", "message" }`, uno por campo con problema. |
| `quote` | `FxQuote` | Solo en `QUOTE_EXPIRED`: la cotización nueva. |

| `code` | Estado | `title` | Cuándo |
|---|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Credenciales inválidas | Login con usuario o contraseña que no coinciden. |
| `INVALID_DEVICE` | 401 | Dispositivo no registrado | `deviceToken` desconocido o revocado. |
| `SESSION_EXPIRED` | 401 | Sesión expirada | Token con firma válida y `exp` pasado. |
| `UNAUTHORIZED` | 401 | No autorizado | Falta el token o no es válido. |
| `NOT_FOUND` | 404 | Recurso no encontrado | Ruta desconocida; cuenta, movimiento o acción que no existe o no es de la persona. |
| `BILLER_NOT_FOUND` | 404 | Empresa no encontrada | `billerId` desconocido en `GET /billers/{billerId}/debts`. |
| `CUSTOMER_NOT_FOUND` | 404 | Cliente no encontrado | El código existe con el formato de la empresa, pero no es de ningún cliente. |
| `PAYMENT_NOT_FOUND` | 404 | Pago no encontrado | La constancia de un pago que no existe o es de otra persona. |
| `VALIDATION_ERROR` | 422 | Datos inválidos | Cuerpo, parámetros o cabecera mal formados o que nombran algo ajeno (con `errors[]`). |
| `INSUFFICIENT_FUNDS` | 422 | Saldo insuficiente | El monto, el total o el pago supera el saldo disponible. |
| `DESTINATION_NOT_FOUND` | 422 | Cuenta de destino no encontrada | Transferencia a un número que no existe. |
| `CURRENCY_MISMATCH` | 422 | Las monedas no coinciden | Origen, destino y monto de otra moneda; cambio fuera del par USD/PEN; pago con una cuenta en dólares. |
| `INSUFFICIENT_HOLDINGS` | 422 | Tenencia insuficiente | Una venta de más acciones de las que se tienen. |
| `QUOTE_EXPIRED` | 422 | Cotización vencida | La cotización del cambio ya venció; trae `quote`. |
| `INVALID_PIN` | 422 | Clave de operaciones inválida | La clave del pago no es la de la persona. |
| `DEBT_ALREADY_PAID` | 422 | Recibo ya pagado | Alguno de los recibos del pago ya está pagado (`detail` lista los ids). |
| `INTERNAL_ERROR` | 500 | Error interno | Cualquier fallo no previsto. No filtres trazas ni mensajes internos. |
| `SERVICE_UNAVAILABLE` | 503 | Servicio no disponible | Mantenimiento o dependencia caída; trae `Retry-After` (segundos). |

Los **códigos de campo** de `errors[].code`, que la app usa para pintar el error bajo cada campo, son:

| `code` | Cuándo |
|---|---|
| `REQUIRED` | Falta el campo, la cabecera `Idempotency-Key` (campo `Idempotency-Key`) o el handshake WebSocket (campo `Upgrade`). |
| `INVALID_FORMAT` | Formato incorrecto: monto, moneda, cantidad, código de cliente, clave de 4 dígitos, lista de ids. |
| `INVALID_VALUE` | Un valor fuera del conjunto permitido: `range`, `side`, `includeHistory`. |
| `MUST_BE_POSITIVE` | Monto o cantidad menor o igual que cero. |
| `TOO_LONG` | `description` de más de 140 caracteres. |
| `TOO_SMALL` | El monto acreditado de un cambio no llega a un centavo (`amount.value`). |
| `MIN_ITEMS` / `DUPLICATED` | `debtIds` vacío o con un id repetido. |
| `OUT_OF_RANGE` | `limit` fuera de 1 a 1000. |
| `INVALID` | `cursor` que no corresponde a la lista. |
| `INVALID_JSON` | El cuerpo no es un objeto JSON (campo `body`). |
| `NOT_OWNED` | Una cuenta que no es de la persona (`accountId`, `sourceAccountId`, `destinationAccountId`). |
| `NOT_FOUND` | `quoteId` desconocido o de otra persona. |
| `UNSUPPORTED_CURRENCY` | Una orden con una cuenta que no está en dólares. |
| `UNKNOWN_SYMBOL` / `UNKNOWN_BILLER` / `UNKNOWN_CUSTOMER` / `UNKNOWN_DEBT` | El símbolo, la empresa, el código de cliente o algún recibo no existen. |

Agregar un código de campo nuevo es un cambio MINOR del contrato (el tipo de `errors[].code` es texto libre).

## Paginación por cursor

Solo `GET /movements` pagina. Parámetros: `accountId` (opcional; sin él, los movimientos de todas las cuentas propias),
`cursor` y `limit`.

- Orden: fecha descendente y, ante igual fecha, id descendente.
- `limit`: de 1 a 1000, **20 por defecto**. Un valor no numérico o fuera de rango da 422 en el campo `limit`
  (`OUT_OF_RANGE`). Un `accountId` ajeno, 422 en `accountId` (`NOT_OWNED`).
- Respuesta: `{ "items": [...], "nextCursor": "..." }`; `nextCursor` es `null` en la última página.
- El cursor es opaco para el cliente y **apunta al último elemento entregado, no a una posición**: así un movimiento
  nuevo al inicio no desplaza las páginas. El backend embebido usa el id del último movimiento en base64url sin relleno
  (`MOV-001-0006` → `TU9WLTAwMS0wMDA2`). Un cursor vacío equivale a no enviarlo; uno que no decodifica o cuyo elemento ya no
  está en la lista da 422 en el campo `cursor` (`INVALID`).
- La app pide páginas de 20 y, si falla la carga de una página, conserva lo ya cargado y ofrece Reintentar.

## Idempotencia

`POST /transfers`, `POST /orders`, `POST /fx/exchanges` y `POST /bill-payments` exigen la cabecera `Idempotency-Key`
(texto libre; la app envía un UUID). Falta o está en blanco: 422 `VALIDATION_ERROR` en el campo `Idempotency-Key`
(`REQUIRED`).

- **Alcance:** la clave vale **por usuario y por operación**: la misma clave en una transferencia y en una orden son
  dos claves distintas, y la de otra persona no choca. El backend embebido indexa `<operación>:<usuario>:<clave>`.
- **Repetirla** devuelve la misma respuesta 201 (mismo cuerpo: mismo `operationNumber`, mismo `createdAt`), sin volver
  a ejecutar nada y **aunque cambie el cuerpo**: la respuesta guardada se entrega antes de validar nada. Por eso la app
  genera una clave nueva cuando la persona cambia lo que envía (otra cotización, por ejemplo) y reutiliza la misma solo
  al Reintentar tras un error de red o un 5xx.
- **Solo se guarda lo 2xx.** Un 4xx no guarda nada: la misma clave se puede volver a usar con un cuerpo corregido.
- La demo guarda las claves en memoria hasta reiniciar o llamar a `POST /_demo/reset`.

> **Recomendación.** En un servidor real, guarda la respuesta (o su identificador) con un TTL de al menos 24 horas y haz
> la inserción de la clave atómica con la ejecución, para que dos llamadas simultáneas con la misma clave no cobren dos
> veces.

## Reglas de negocio

Cada regla se valida **en el orden en que se lista**; la primera que falla gana, salvo la validación de forma de cada
cuerpo, que reúne todos los errores en un solo `VALIDATION_ERROR`.

### Cuentas y movimientos

- `GET /accounts` devuelve las cuentas de la persona con `availableBalance` y `accountingBalance`. Las operaciones de
  abajo solo cambian el **disponible**; el contable queda como en la semilla (el backend embebido no lo mueve).
- Cada operación crea un movimiento al inicio de la cuenta afectada (visible en `GET /movements`, su detalle y su
  desglose), con `channel: "APP"` y `status: "COMPLETED"`. Los ids y los números de operación llevan una secuencia
  propia por tipo, desde 1:

| Operación | Id de la operación | `operationNumber` | Id del movimiento | Categoría |
|---|---|---|---|---|
| Transferencia | `TRF-000001` | `90000001` | `MOV-002-T0001` | `TRANSFER` |
| Orden | `ORD-000001` | `70000001` | `MOV-003-O0001` | `INVESTMENT` |
| Cambio | `FX-000001` | `80000001` | `MOV-003-X0001` y `MOV-002-X0001` | `TRANSFER` |
| Pago de servicios | `PAY-000001` | `80000001` | `MOV-002-P0001` | `SERVICES` |

  (El número de operación de los cambios y el de los pagos comparten prefijo, `8`, pero tienen secuencias separadas:
  nada los usa como clave.) El id del movimiento lleva el número de la cuenta y una letra por tipo.
- `GET /movements/{movementId}` y `.../breakdown` buscan entre los movimientos de las cuentas **propias**; los de otra
  persona dan 404. El desglose cumple `|monto| = Σ lines + fees + taxes`, y `exchangeRate` (4 decimales) solo viaja en los
  de un cambio.

### Transferencias (`POST /transfers`)

1. `Idempotency-Key` presente y cuerpo válido (`VALIDATION_ERROR`, un elemento por campo): `sourceAccountId` y
   `destinationAccountNumber` no vacíos; `amount.value` con la forma `^\d{1,9}(\.\d{1,2})?$` y mayor que cero
   (`INVALID_FORMAT`, `MUST_BE_POSITIVE`); `amount.currency` con la forma `^[A-Z]{3}$`; `description` de hasta 140
   caracteres (opcional).
2. El origen es una cuenta propia (`VALIDATION_ERROR`, campo `sourceAccountId`, `NOT_OWNED`).
3. El destino existe entre las cuentas de terceros, por número exacto (`DESTINATION_NOT_FOUND`).
4. Origen, destino y moneda del monto coinciden (`CURRENCY_MISMATCH`).
5. `monto <= saldo disponible` (`INSUFFICIENT_FUNDS`).

Si pasa: se descuenta el saldo disponible y se agrega un movimiento `DEBIT` (monto negativo) con
`description` = la enviada o `Transferencia a <titular>`, `counterparty` = `{ name: titular, reference: número destino }`
y un desglose de una línea "Transferencia", sin comisiones ni impuestos. La respuesta 201 trae `transferId`,
`operationNumber`, `createdAt`, `sourceAccountId`, `sourceAvailableBalance` y `movementId`.

### Mercado y portafolio

- `GET /market/stocks` devuelve las ocho acciones, y `GET /market/stocks/{symbol}`, una con los datos del día (`open`,
  `high`, `low`, `previousClose`, `volume`, `description`). `change = last - previousClose` y `changePct = change * 100 /
  previousClose`, a 2 decimales. `high` y `low` incluyen los precios que ya pasaron por el servidor. `ts` es cuándo
  cambió el precio por última vez.
- `GET /market/stocks/{symbol}/history?range=1D|1W|1M|1Y` (obligatorio; otro valor, 422 en `range`): puntos
  equiespaciados, del más antiguo al más reciente: 1D tiene 390 (uno por minuto), 1W 168 (por hora), 1M 120 (cada 6
  horas) y 1Y 365 (por día). El último punto tiene el precio **inicial** de la acción y la hora de la consulta
  (`ts` hacia atrás de a un paso), así el gráfico empalma con el precio en vivo.
- **Precio vigente.** El servidor tiene un solo precio por acción y uno para el tipo de cambio. `GET /market/stocks`,
  `POST /orders`, `GET /portfolio` y `GET /fx/rates` leen siempre ese mismo precio, que es el que publica el canal en
  vivo. En la demo avanza por una secuencia pregenerada de ticks (la semilla); un servidor real lo alimenta de donde
  quiera, pero debe mantener esa coherencia entre REST y canal.
- `GET /portfolio`: por tenencia, `marketValue = cantidad × último` y `cost = cantidad × averagePrice` (redondeo a 2
  decimales), `gain = marketValue - cost`, `gainPct = gain * 100 / cost`, `dayChange = cantidad × (último -
  previousClose)`. `totals` suma cada uno; `dayChangePct` es sobre el valor al cierre anterior (`marketValue -
  dayChange`). Un porcentaje con base cero es `"0.00"`. Una tenencia que llega a 0 sale de la lista. `accountId` es la
  cuenta en dólares donde se liquidan las órdenes.

### Órdenes (`POST /orders`)

Solo órdenes **a mercado**, por cantidad entera, al precio vigente del servidor (el último tick, no el que vio la app).

1. `Idempotency-Key` presente y cuerpo válido, todos los errores juntos: `symbol` existente (`UNKNOWN_SYMBOL`), `side`
   `BUY` o `SELL` (`INVALID_VALUE`), `quantity` como **número JSON** entero mayor que cero (`INVALID_FORMAT`,
   `MUST_BE_POSITIVE`), `accountId` no vacío.
2. La cuenta es propia (`NOT_OWNED`) y está en dólares (`UNSUPPORTED_CURRENCY`), ambos como `VALIDATION_ERROR` en
   `accountId`.
3. Compra: `total = cantidad × precio` (a 2 decimales) `<= saldo disponible` (`INSUFFICIENT_FUNDS`). Venta: `cantidad <=
   tenencia` (`INSUFFICIENT_HOLDINGS`, `detail` "Tienes N acciones de X.").

Si pasa: la compra descuenta el total y suma la tenencia con el nuevo promedio `(cantidad anterior × promedio +
cantidad × precio) / cantidad nueva`, a 2 decimales; la venta acredita el total, resta la tenencia y conserva el
promedio. El movimiento es `INVESTMENT` (`DEBIT` la compra, `CREDIT` la venta), con descripción "Compra de 2 ANDM" o
"Venta de 1 QORI". La respuesta 201 trae el precio ejecutado, el total, el nuevo saldo y la tenencia resultante
(`holding.averagePrice` es `null` si quedó en 0). Sin comisiones ni impuestos.

### Cambio de divisas (`GET /fx/rates`, `POST /fx/exchanges`)

- `GET /fx/rates` emite en cada llamada una **cotización** nueva: `{ pair: "USD/PEN", buy, sell, quoteId, expiresAt,
  ts }` con la tasa vigente. El servidor la recuerda, con su tasa, hasta `expiresAt` (ahora + 30 s). `buy` es lo que
  paga el banco por cada dólar; `sell`, lo que cobra. Se puede usar más de una vez mientras esté vigente.
- `POST /fx/exchanges` valida, en orden:
  1. `Idempotency-Key` y cuerpo (`quoteId`, `sourceAccountId`, `destinationAccountId` no vacíos; `amount` como en las
     transferencias).
  2. Las dos cuentas son propias (`NOT_OWNED`) y la cotización existe y es de la persona (`NOT_FOUND` en `quoteId`).
  3. Origen y destino forman el par USD/PEN, en cualquier sentido, y el monto está en la moneda de origen
     (`CURRENCY_MISMATCH`).
  4. La cotización no venció: `ahora < expiresAt`. Si venció, 422 `QUOTE_EXPIRED` con `quote` = una cotización **nueva**
     (emitida en ese momento, con la misma vigencia que la vencida), para que la app la ofrezca sin otra llamada.
  5. `monto <= saldo disponible` (`INSUFFICIENT_FUNDS`).
  6. Lo acreditado es de al menos 0.01 (`TOO_SMALL` en `amount.value`).
- Si el banco compra dólares (USD → PEN) acredita `monto × buy`; si los vende (PEN → USD), `monto / sell`; ambos a 2
  decimales, mitad hacia arriba. Debita el origen, acredita el destino y agrega un movimiento `TRANSFER` en cada cuenta
  (descripción "Cambio de dólares a soles" o "Cambio de soles a dólares"; `exchangeRate` en el desglose). La respuesta
  201 trae la tasa usada (`rate`), lo debitado, lo acreditado, los dos saldos nuevos y `movementIds` (origen, destino).

### Pago de servicios

Veinte empresas por rubro. Cada una publica el formato de su código de cliente (`customerCodeFormat`, una expresión
regular que debe cubrir el texto completo) y, por código, los recibos pendientes en soles. La clave de operaciones es
una clave numérica de 4 dígitos de la persona, distinta de la contraseña.

- `GET /billers?query=`: las empresas en el orden de sus rubros. `query` filtra por nombre o rubro, sin distinguir
  mayúsculas ni tildes: cada palabra debe aparecer en el nombre o en el rubro. Sin `query` salen todas.
- `GET /billers/{billerId}/debts?customerCode=&includeHistory=`, en este orden: la empresa existe (404
  `BILLER_NOT_FOUND`); `customerCode` cumple el formato de la empresa e `includeHistory` es `true`, `false` o está
  ausente (422, un elemento por campo); el código existe en esa empresa (404 `CUSTOMER_NOT_FOUND`). Un recibo ya
  pagado no aparece; un cliente sin recibos pendientes da 200 con `debts: []`. Con `includeHistory=true` se añade
  `history` con el histórico de facturación de los últimos 24 meses (del más antiguo al más reciente, `amount` =
  suma de `lines`); si la empresa no lo lleva, `history` es `[]`. Sin el parámetro el campo no se envía.
- `POST /bill-payments` valida, en orden:
  1. `Idempotency-Key` y cuerpo (todos los errores juntos): `billerId` existente (`UNKNOWN_BILLER`), `customerCode` con
     el formato de la empresa, `debtIds` con al menos un id, sin repetir, `accountId` no vacío y `pin` de exactamente 4
     dígitos (`INVALID_FORMAT`).
  2. La cuenta es propia (`VALIDATION_ERROR`, `accountId`, `NOT_OWNED`) y está en soles (`CURRENCY_MISMATCH`).
  3. `pin` es la clave de operaciones de la persona (`INVALID_PIN`). La demo la guarda en `users.json`; no hay contador
     de intentos ni bloqueo.
  4. El código existe en la empresa (`VALIDATION_ERROR`, `customerCode`, `UNKNOWN_CUSTOMER`) y todos los recibos son de
     ese cliente (`debtIds`, `UNKNOWN_DEBT`), y **ninguno está pagado** (`DEBT_ALREADY_PAID`; no se cobra ninguno).
  5. `total <= saldo disponible` (`INSUFFICIENT_FUNDS`).

  Si pasa: se debita el total, los recibos quedan pagados y se agrega un movimiento `SERVICES` `DEBIT` ("Pago de
  \<empresa\>", contraparte = la empresa y el código de cliente) con un recibo por línea de desglose ("Recibo de octubre
  2026"). El 201 trae `paymentId`, `operationNumber`, `createdAt`, `paidDebts[]`, `total`, `accountId`,
  `accountAvailableBalance` y `movementId`, con todos los montos en soles.
- `GET /bill-payments/{paymentId}/voucher`: la constancia como HTML (`text/html`), una página simple y semántica, sin
  scripts, en español: número de operación, fecha y hora (`dd/MM/yyyy HH:mm`), empresa, cliente, código, cuenta de
  cargo, una tabla con los recibos pagados (período e importe), el total y el nuevo saldo disponible. Los montos van con
  formato de soles (`S/ 2,122.50`) y cada dato en su propio elemento (`dt`/`dd`, `td`), para buscarlos por texto. La
  constancia guarda el saldo y la cuenta **de ese momento**: no cambia aunque la cuenta se mueva después. Un pago ajeno o
  inexistente da 404 `PAYMENT_NOT_FOUND`.
- Los recibos pagados se marcan por id de recibo, no por persona; en una implementación con más clientes que la usuaria
  de prueba, un recibo pertenece a un solo cliente de una empresa.

### Otras reglas

- `GET /app/config` no pide token: `{ minSupportedVersion, termsVersion }`. La app vuelve a mostrar los términos cuando
  `termsVersion` cambia; `minSupportedVersion` se lee, pero hoy no bloquea nada.
- `GET /me`: `id`, `fullName`, `documentMasked` (el documento enmascarado) y `email`. No expongas nada más del usuario.

## Canal en vivo (WebSocket)

`GET /api/v1/market/stream` con upgrade a WebSocket y el mismo `Authorization: Bearer <JWT>`. Está descrito en
[`api/asyncapi.yaml`](../api/asyncapi.yaml); aquí el resumen para implementarlo.

- **Handshake.** Responde como cualquier endpoint antes del upgrade: 401 `UNAUTHORIZED` o `SESSION_EXPIRED` con RFC 9457
  si el token falta o venció, 422 `VALIDATION_ERROR` en el campo `Upgrade` si no hay cabeceras de upgrade, y 101 si todo
  está bien. Una vez abierto, no hay más autenticación.
- **Mensajes del cliente** (texto JSON con `type`): `{ "type": "subscribe", "symbols": ["ANDM"], "pairs": ["USD/PEN"] }` y
  `unsubscribe` con la misma forma. Una lista ausente equivale a vacía. Cada `subscribe` se responde enseguida con el valor
  actual de lo que nombra: un `quote` por símbolo y un `fx` por par, así el cliente tiene precio sin esperar el siguiente
  tick. `unsubscribe` no tiene respuesta. Un mensaje que no es un objeto JSON, con otro `type`, con listas mal formadas,
  binario, o con símbolos o pares desconocidos se **ignora**: ni respuesta ni cierre.
- **Mensajes del servidor.** `quote` = la misma forma de cada elemento de `GET /market/stocks` más `type: "quote"`; `fx` =
  la misma forma de `GET /fx/rates` más `type: "fx"`. Cada `fx` es una cotización nueva que el servidor recuerda: sirve en
  `POST /fx/exchanges` hasta su `expiresAt`.
- **Reloj de mercado.** Uno solo, compartido por todas las conexiones: corre mientras haya al menos una abierta y en cada
  tick avanza **todas** las acciones y el tipo de cambio (dos conexiones no aceleran los precios). Cada conexión recibe
  solo lo que tiene suscrito. REST y órdenes ven el mismo precio.
- **Cierres.** Al vencer el token del handshake (`exp`), el servidor cierra con el código **4401** ("La sesión expiró.");
  si el cliente no contesta el cierre en 5 s, corta el socket. Si el cliente cierra, el servidor contesta con 1000. La app
  trata un 4401 como un 401: abre el modal de sesión expirada y no reconecta con ese token. Cualquier otra caída
  (sin trama de cierre, un 5xx en el handshake, un timeout) hace que la app reconecte esperando 1, 2, 4 y 8 s (tope 8) y
  vuelva a suscribir todo.
- **`POST /_demo/reset`** no cierra las conexiones: vuelve los precios al primer tick y envía enseguida a cada conexión
  el valor actual de lo suscrito, con cotizaciones nuevas.

Prism no cubre WebSockets: contra un mock generado solo desde el contrato, la app no tiene canal (reconecta en bucle y
muestra "Reconectando…").

## Solo de demo

Nada de esto forma parte del contrato de un servidor real; el cliente de debug y demo lo usa para provocar escenarios.

| Qué | Para qué | En un servidor real |
|---|---|---|
| `X-Demo-Latency-Ms` | Retrasa la respuesta (entero ≥ 0), sin variación. | No lo implementes. |
| `X-Demo-Failure-Rate` y `X-Demo-Failure-Mode` | Un porcentaje (0 a 100) de llamadas falla, con semilla fija, como 500 `INTERNAL_ERROR` (`http_500`), sin respuesta (`timeout`) o cerrando el socket (`disconnect`). | No lo implementes. |
| `X-Demo-Backend-Down: true` | 503 `SERVICE_UNAVAILABLE` con `Retry-After: 30` en todos los endpoints. | No lo implementes (el 503 real sí es parte del contrato). |
| `X-Demo-Session-Ttl` | En `POST /auth/login` y `POST /auth/biometric/login`: segundos de vida del token. | El TTL lo decide el servidor. |
| `X-Demo-Quote-Ttl-S` | En `GET /fx/rates` y en el handshake del canal: vigencia de la cotización (30 s por defecto). | La vigencia la decide el servidor (30 s es razonable). |
| `X-Demo-Tick-Ms` y `X-Demo-Stream-Drop-S` | Solo en el handshake: milisegundos entre ticks (0 congela los precios) y segundos hasta que el servidor corta la conexión (0 = nunca). | No lo implementes. |
| `POST /_demo/reset` | Devuelve el estado a los fixtures. No pide token y ninguna cabecera `X-Demo-*` le aplica. | No lo implementes, o protégelo para que no exista fuera de entornos de prueba. |

Todo lo bajo `/_demo` es de demo. Las cabeceras de latencia y fallos aplican a todos los endpoints (también al handshake
del canal), salvo `POST /_demo/reset` y las rutas que no existen. Un valor inválido equivale a no enviar la cabecera. En
la demo, el estado (saldos, movimientos nuevos, tenencias, precios vigentes, cotizaciones, pagos, recibos pagados e
idempotencia) vive en memoria y vuelve a los fixtures al reiniciar el proceso o con ese endpoint; la excepción son los
dispositivos registrados para la huella, que sobreviven al reinicio.

## Fixtures como semilla

[`api/fixtures/`](../api/fixtures/) tiene los datos iniciales de la usuaria de prueba (`ana.torres` / `Cartera2026`,
clave de operaciones `1234`). Son JSON; en la app viajan como assets de debug y demo, y los tests los leen del disco. Úsalos
para sembrar la base de datos de tu servidor al arrancar (una migración, un script o el `seed` de tu ORM) y
reprodúcelos cuando quieras volver al estado inicial. Los generadores (`api/tools/generate_movements.py`,
`generate_market.py` y `generate_billers.py`) los producen con semilla fija: si cambian, se regeneran con
`python3 -I api/tools/<generador>.py`.

| Archivo | Forma | Notas |
|---|---|---|
| `users.json` | `[{ id, username, password, fullName, documentMasked, email, operationsPin }]` | Una usuaria (`USR-001`). `password` y `operationsPin` van en claro **solo en la demo**: guárdalos con un hash (argon2 o bcrypt). `GET /me` expone `id`, `fullName`, `documentMasked` y `email`. |
| `accounts.json` | `[{ id, ownerId, alias, type, currency, numberMasked, availableBalance, accountingBalance }]` | Tres cuentas: `ACC-001` (soles, ahorros), `ACC-002` (soles, corriente) y `ACC-003` (dólares). `ownerId` apunta a `users.id` y no sale en la API. Saldos como `Money`. |
| `third-party-accounts.json` | `[{ number, holderName, currency }]` | Los destinos de transferencia, por `number` exacto. |
| `app-config.json` | `{ minSupportedVersion, termsVersion }` | La respuesta de `GET /app/config`. |
| `movements/<ACC-ID>.json` | `[{ id, accountId, date, description, category, amount, type, operationNumber, channel, status, counterparty, breakdown }]` | Un archivo por cuenta (1.200, 30 y 12 movimientos). Es el registro completo: la lista toma los siete primeros campos, el detalle suma `operationNumber`, `channel`, `status` y `counterparty`, y `breakdown` es la respuesta del desglose. `amount` va con signo y `type` lo concuerda. Se ordenan al cargar. |
| `market/stocks.json` | `[{ symbol, name, currency, previousClose, open, high, low, volume, description }]` | Las ocho acciones, todas en USD. |
| `market/ticks/<SYMBOL>.json` | `{ symbol, prices: [...] }` | 600 precios en texto. El primero es el precio inicial; cada tick avanza un índice y, al agotarse, vuelve al primero. |
| `market/history/<SYMBOL>.json` | `{ symbol, ranges: { "1D": { stepSeconds, prices }, "1W": ..., "1M": ..., "1Y": ... } }` | Los puntos de cada rango, del más antiguo al más reciente; el último es el precio inicial. La API les pone `ts` hacia atrás desde la hora de la consulta. |
| `fx/rates.json` | `{ pair: "USD/PEN", ticks: [{ buy, sell }, ...] }` | 600 ticks del tipo de cambio; el primero es el inicial (compra 3.742, venta 3.768). |
| `portfolio.json` | `{ userId, accountId, holdings: [{ symbol, quantity, averagePrice }] }` | Las tenencias iniciales de Ana (ANDM 10, MOCH 5, QORI 2) y la cuenta en dólares donde se liquidan las órdenes. |
| `billers.json` | `[{ id, name, category, customerCodeFormat }]` | Veinte empresas; el orden del archivo es el de `GET /billers`. |
| `debts/<billerId>.json` | `{ billerId, customers: [{ code, name, debts: [{ id, period, dueDate, amount }], history? }] }` | Un archivo por empresa. Solo cuatro tienen cliente de prueba (`BIL-LUZ` 100200, `BIL-AGUA` 300400, `BIL-TEL` 500600 y `BIL-NET` 700800); las demás tienen `customers: []`. `history` (24 meses de `{ period, amount, paidAt, lines }`) lo trae `BIL-LUZ`. |

**Relaciones.** `users.id` ← `accounts.ownerId` ← `movements/<ACC-ID>.json` (el nombre del archivo es la cuenta); `portfolio.userId`
→ `users.id` y `portfolio.accountId` → una cuenta en USD; `market/ticks/<SYMBOL>.json` y `market/history/<SYMBOL>.json`
→ `stocks.symbol` (por nombre de archivo); `debts/<billerId>.json` → `billers.id`; `users.operationsPin` es lo que valida el
pago de servicios.

**Lo que el servidor guarda además de la semilla** (nada de esto está en los fixtures): saldos disponibles, movimientos
creados, tenencias cambiadas, cotizaciones emitidas, recibos pagados y pagos hechos (para la constancia), claves de
idempotencia, dispositivos registrados, el índice de tick de cada acción y del tipo de cambio, y los contadores de
secuencia (`TRF-`, `ORD-`, `FX-`, `PAY-`, `QTE-`).

## Probar sin servidor: Prism

Se puede correr la app contra un mock generado **solo** desde el contrato, sin escribir una línea del servidor. Prism
sirve los ejemplos de `openapi.yaml`, que son los mismos datos que el backend embebido (un test lo comprueba), así que
login, perfil, cuentas, movimientos con su detalle y su desglose, empresas, recibos, el pago y su constancia se ven
igual que en la demo.

```bash
cd ~/StudioProjects/Cartera && npx @stoplight/prism-cli@5.16.0 mock api/openapi.yaml
```

Prism escucha en `http://127.0.0.1:4010` y sirve las rutas **sin** el prefijo `/api/v1` (`GET /accounts`, no
`GET /api/v1/accounts`). Para que la app de debug las encuentre desde el emulador, apúntala a la máquina anfitriona con
`BASE_URL = http://10.0.2.2:4010/`:

```bash
cd ~/StudioProjects/Cartera && ./gradlew installDebug -Pcartera.baseUrl=http://10.0.2.2:4010/
cd ~/StudioProjects/Cartera && adb shell pm grant com.hacybeyker.cartera android.permission.ACCESS_LOCAL_NETWORK
```

`cartera.baseUrl` es una propiedad de Gradle que solo cambia `BuildConfig.BASE_URL` en **debug** (demo y release no la
leen). Debe ser una URL `http` o `https` que termine en `/`; si no, el build falla con un mensaje claro. Se pasa con `-P` o,
para no repetirla, en `~/.gradle/gradle.properties` (no en el `gradle.properties` del proyecto). El tráfico sin TLS hacia
`10.0.2.2` solo está permitido en debug (`app/src/debug/res/xml/network_security_config.xml`). El backend embebido
sigue arrancando; la app simplemente no lo usa. Vuelve a la demo quitando la propiedad.

**Qué se puede recorrer con Prism:** el login (cualquier usuario y contraseña con la forma correcta devuelven el token de
ejemplo, con `expiresIn: 900`), `GET /me`, `GET /accounts` (ACC-001, ACC-002 y ACC-003 con los saldos de los fixtures),
los movimientos y su detalle y desglose, `GET /billers`, los recibos de Luz del Norte (código 100200, dos recibos),
`POST /bill-payments` (201 del pago de referencia: operación 80000001 y saldo 2122.50) y la constancia
(`GET /bill-payments/{paymentId}/voucher`).

**Qué no:**

- Prism no tiene estado: devuelve siempre el primer ejemplo, valida la forma de los parámetros y del cuerpo pero no los
  usa para elegir la respuesta (`accountId`, `cursor`, `query` y `customerCode` no cambian lo que vuelve), y un saldo no
  cambia tras pagar. El código `999999` o la clave `0000` no dan un error; para ver uno, envía la cabecera `Prefer:
  code=422` (y, si hay varios ejemplos, `Prefer: example=claveInvalida`).
- Exige que haya una cabecera `Authorization` (cualquier valor) y valida la forma de los cuerpos y de los parámetros
  obligatorios; no mira la firma ni el vencimiento.
- No sirve WebSockets, así que el canal en vivo no existe y las pantallas con precios muestran "Reconectando…".
- Los ejemplos de listas están recortados donde lo dicen (la página de movimientos trae seis, sin siguiente página, para
  que la app no pida de nuevo la misma).

## Validar el contrato

- **Lint.** `cd ~/StudioProjects/Cartera && npx @stoplight/spectral-cli@6.17.1 lint api/openapi.yaml api/asyncapi.yaml
  --fail-severity=hint` (reglas
  `spectral:oas` y `spectral:asyncapi`, en [`.spectral.yaml`](../.spectral.yaml)); lo corre el job `contract` del CI y
  debe dar cero problemas. Spectral también valida cada ejemplo contra su esquema.
- **Tests de contrato (JVM).** `app/src/testDebug/java/com/hacybeyker/cartera/demo/contract/`: validan las respuestas
  del backend embebido contra los esquemas de `openapi.yaml` con swagger-request-validator (estado, tipo de contenido,
  forma, campos no declarados), prueban que cada respuesta documentada se produce y que cada ejemplo es válido y, salvo
  los que se dicen, es lo que responde el backend desde datos limpios. Se corren con `./gradlew test`.
- **Tu servidor.** El mismo contrato sirve para probarlo: apunta swagger-request-validator, Dredd, Schemathesis u otra
  herramienta a `api/openapi.yaml` y a tu servidor; los ejemplos y los fixtures son la entrada.

## Cómo encaja en cada framework

La app solo necesita que el comportamiento de arriba se cumpla; la estructura es tuya. Un mapa para empezar:

| Pieza | NestJS | Spring Boot | Node (Express o Fastify) |
|---|---|---|---|
| Prefijo `/api/v1` | `app.setGlobalPrefix('api/v1')` | `server.servlet.context-path` o `@RequestMapping("/api/v1")` | `app.use('/api/v1', router)` |
| JWT HS256 y 401 | Guard con `@nestjs/jwt` (`verify` con `algorithms: ['HS256']`); distingue `TokenExpiredError` (`SESSION_EXPIRED`) del resto (`UNAUTHORIZED`) | Spring Security con `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)` y un `AuthenticationEntryPoint` que escribe el problema | `jose` (`jwtVerify`) en un middleware |
| Errores RFC 9457 | `ExceptionFilter` global que escribe `application/problem+json` | `ProblemDetail` y `@RestControllerAdvice` (soporte nativo desde Spring 6), con `code`, `traceId` y `errors` como propiedades | Middleware de errores final |
| `X-Request-Id` y `traceId` | Interceptor o middleware que lo fija en la respuesta y en el contexto | `OncePerRequestFilter` | Middleware al inicio |
| Validación de cuerpos | `class-validator` con un `ValidationPipe` que arma `errors[]` (todos los campos juntos) | Bean Validation y un `ConstraintViolationException` handler | `zod` o `ajv` |
| Idempotencia | Interceptor sobre los cuatro `POST`, con una tabla `(operación, usuario, clave) → respuesta`, atómica con la operación | `HandlerInterceptor` o aspecto con una tabla única `(operation, user_id, key)` | Middleware con la misma tabla |
| Decimales | `decimal.js` o `big.js`; columnas `NUMERIC` | `BigDecimal` con `RoundingMode.HALF_UP` | `decimal.js` o `big.js` |
| WebSocket | `@WebSocketGateway` con `ws` (no Socket.IO: el cliente es WebSocket puro) | `TextWebSocketHandler` con un `HandshakeInterceptor` que autentica | `ws` con `server.on('upgrade')` |

Para cualquier stack: el estado que cambia (saldos, movimientos, tenencias, pagos) debe escribirse en una sola transacción
con la clave de idempotencia, o una repetición simultánea cobrará dos veces; y todo recurso se filtra por el `sub` del
token antes de responder.
