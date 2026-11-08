# Cartera · Alcance de la fase 3

> **Estado:** aprobado el 2026-11-08, con todas las propuestas (R1–R5) y decisiones (S1–S7) de la sección 1 · parte de la fase 2 cerrada (`df42172`, CI en verde).
> Fija qué se construye, no cómo se escribe el código.

**Fase 3 en una línea:** un módulo legacy de verdad (Fragments, XML, RxJava y MVP) con un flujo bancario clásico, el pago de servicios; pantallas sin ids para probar selectores; y el contrato listo para que un servidor externo (NestJS, Spring o Node) lo implemente. La prueba de que alcanza con cambiar `BASE_URL` es correr la app contra un mock externo generado solo desde el contrato. No se despliega nada.

Siguen vigentes todas las reglas de las fases 1 y 2: contrato primero, fixtures como único origen de datos, escenarios por `X-Demo-*`, marcas de contenido visible, `main` sin nada de demo y D4 para los casos sembrados.

---

## 1. Decisiones (aprobadas)

### 1.1 Lo que el stack actual no trae

| # | Hueco | Propuesta | Alternativa |
|---|---|---|---|
| R1 | **Un solo módulo:** la regla de la plantilla (P6 de la fase 1 lo dejó para ahora). | Módulo Gradle `:legacy` (Android library), del que depende `app`. Los gates (ktlint, detekt, Lint, Kover y Roborazzi) y el DoD cubren los dos módulos. | El legacy como paquete dentro de `app`: más simple, pero menos fiel a cómo conviven el código viejo y el nuevo en las apps reales. |
| R2 | **Stack legacy:** no hay Views, RxJava ni Fragments. | En `:legacy`: MVP (Presenter más interfaz de vista), RxJava 3 con RxAndroid, el adaptador RxJava 3 de Retrofit, Fragments, XML con ViewBinding, AppCompat, Material Components, RecyclerView y ConstraintLayout. DI con Hilt, que ya está. | Dagger 2 sin Hilt: más fiel a un legacy viejo, pero duplica la DI. |
| R3 | **Integración con Navigation 3.** | Una `LegacyActivity` aparte, lanzada desde Servicios clásicos (hoy "Próximamente"). Al volver, el resultado le avisa a Compose que hubo un pago, y Compose invalida `AccountChanges`. | `AndroidFragment` dentro de una entrada de Navigation 3 (requiere `fragment-compose`). |
| R4 | **Validar el contrato.** | Lint de `openapi.yaml` y `asyncapi.yaml` con Spectral (vía `npx`, local y en un job de CI). Tests de contrato en la JVM que validan las respuestas del backend embebido contra el esquema, con `swagger-request-validator` (solo en tests). | Solo el lint. |
| R5 | **Probar un servidor externo sin desplegar.** | Mock local con Prism (`npx`) generado desde `openapi.yaml` y sus ejemplos, más una propiedad de Gradle que cambia `BASE_URL` en debug. Prism no cubre WebSockets: el canal queda documentado. | Solo documentación. |

### 1.2 Decisiones de diseño

| # | Decisión | Por qué |
|---|---|---|
| S1 | La función legacy es el **pago de servicios**: empresas, código de cliente, recibos pendientes, cuenta y resumen, clave de operaciones y constancia. | Es el módulo clásico de la banca peruana, con listas, formularios y un comprobante. |
| S2 | **Elementos sin ids**, a propósito y documentados: el teclado de la clave (vista propia con nodos virtuales de accesibilidad que solo exponen el texto), la constancia en un WebView y los ítems de la lista de empresas (solo texto). El resto de `:legacy` usa `android:id`. | Ejercita los selectores por texto y por accesibilidad sin dejar todo el módulo sin ids. |
| S3 | **Teclado de la clave con dígitos en orden aleatorio**, como en los bancos. El orden sale de una semilla del escenario (`pin_pad_seed`), así que es determinista para testigo. Clave de demo: `1234`. | Realismo, sin flows frágiles. |
| S4 | `:legacy` **no ve nada de `app`**: define sus propias interfaces (sesión, `BASE_URL`, marcas de contenido visible) y `app` las implementa con Hilt. | Inversión de dependencias, como en un legacy real integrado. |
| S5 | **AGENTS.md declara que `:legacy` conserva su stack a propósito**: los agentes no lo migran a Compose ni a corrutinas. | Prueba el soporte legacy de hb-stack-mobile-kotlin y de hb-setup. |
| S6 | (Opcional) **Dos casos sembrados en el stack legacy** (§5). | hb-performance debe funcionar también con XML y RxJava; sin casos ahí, no se puede comprobar. |
| S7 | **Retro desde el inicio:** cada bloque anota sus fricciones con la plantilla y las skills. Al cerrar, una retro de la fase y la consolidada de las tres. | Evidencia para mejorar las herramientas. |

---

## 2. Pantallas y navegación

```
Más ──► Servicios clásicos ──► LegacyActivity
          Empresas ──► Código de cliente ──► Recibos pendientes ──► Cuenta y resumen ──► Clave (teclado) ──► Constancia (WebView)
          ◄── al terminar, vuelve a Compose (Más) e invalida las cuentas
```

| Pantalla (Fragment) | Llamadas | Contenido |
|---|---|---|
| Empresas | `GET /billers?query=` | Buscador y lista por rubro. Los ítems no tienen id (S2). |
| Código de cliente | — | Campo del código de suministro, validación de formato y Continuar. |
| Recibos pendientes | `GET /billers/{billerId}/debts?customerCode=` | Lista con casillas, total seleccionado y estado vacío "No tienes recibos pendientes". 404 `CUSTOMER_NOT_FOUND`: mensaje en el campo anterior. |
| Cuenta y resumen | `GET /accounts` | Cuenta de origen (RadioGroup), resumen de recibos y total. |
| Clave | `POST /bill-payments` + `Idempotency-Key` | Teclado de 4 dígitos sin ids (S2 y S3). 422 `INVALID_PIN`: mensaje y teclado reordenado. |
| Constancia | `GET /bill-payments/{paymentId}/voucher` (text/html) | WebView sin ids con el n.º de operación, los recibos pagados y el nuevo saldo; botón Finalizar. |

**Reglas**
- La pila es la de Fragments de la `LegacyActivity`. Atrás en la Constancia equivale a Finalizar.
- Un 401 en `:legacy` cierra la Activity y abre el modal global de sesión expirada en Compose.
- Errores de red y 5xx: diálogo de AppCompat con Reintentar y Cancelar, en el estilo XML.
- Al finalizar un pago, Más y las pantallas de cuentas muestran el saldo nuevo con un solo `GET /accounts`, como en la fase 1.

---

## 3. Contrato

### 3.1 Endpoints nuevos (en `api/openapi.yaml`)

| Método y ruta | Respuesta 2xx | Errores |
|---|---|---|
| `GET /billers?query=` | `items[]`: `id`, `name`, `category`, `customerCodeFormat` | 401 |
| `GET /billers/{billerId}/debts?customerCode=` | `customerName` y `debts[]`: `id`, `period`, `dueDate`, `amount` | 401, 404 `CUSTOMER_NOT_FOUND`, 422 |
| `POST /bill-payments` + `Idempotency-Key` | 201: `paymentId`, `operationNumber`, `paidDebts[]`, `total`, `accountId`, `accountAvailableBalance`, `movementId` | 401, 422 (`VALIDATION_ERROR`, `INVALID_PIN`, `INSUFFICIENT_FUNDS`, `DEBT_ALREADY_PAID`), 5xx |
| `GET /bill-payments/{paymentId}/voucher` | 200 `text/html` | 401, 404 |

**Regla de negocio mínima:** se valida la clave de operaciones (`users.json`) y el saldo de la cuenta. Si pasa, se debita el total, se marcan los recibos como pagados y se crea un movimiento. La idempotencia funciona igual que en las transferencias.

### 3.2 Contrato listo para un servidor externo

- `info.version` en SemVer (1.0.0 al cerrar la fase), con un `api/CHANGELOG.md`.
- `servers`: el embebido (`http://127.0.0.1:8765/api/v1`) y un remoto de ejemplo, sin desplegar.
- Ejemplos en cada respuesta, tomados de los fixtures: así Prism devuelve datos coherentes.
- `docs/CONTRATO.md`, una guía para implementar el servidor en NestJS, Spring o Node:
  - Autenticación JWT y su TTL.
  - Errores RFC 9457.
  - Paginación por cursor.
  - Idempotencia.
  - Reglas de negocio.
  - El canal WebSocket (AsyncAPI).
  - Qué es solo de demo (`X-Demo-*`, `/_demo/reset`).
  - Cómo usar `api/fixtures/` como semilla.
- Lint con Spectral y tests de contrato (R4).
- Prueba con un servidor externo (R5):
  - La app en debug apunta a Prism (`http://10.0.2.2:4010/api/v1/`).
  - Se recorren login, cuentas, movimientos y pago de servicios con los ejemplos del contrato.
  - El canal en vivo y la lógica con estado quedan fuera: Prism no la tiene.

---

## 4. Backend embebido y escenarios

- Fixtures nuevos: `billers.json` y `debts/<billerId>.json`, con los datos de §6.
- Ids de endpoint nuevos: `billers`, `biller_debts`, `bill_payments` y `bill_voucher`. Latencia y fallos aplican igual.

| Parámetro nuevo | Valores | Por defecto | Extra de adb |
|---|---|---|---|
| Semilla del teclado | entero; 0 = orden fijo 1–9, 0 | 0 | `--ei pin_pad_seed 7` |

La semilla es del cliente, no viaja al servidor. Está en la pantalla Escenario y en `launchApp`.

---

## 5. Casos sembrados en el stack legacy (S6)

Igual que antes: apagados por defecto, release no los puede encender, nombres neutros (D4) y el mapeo solo en este documento.

| # | Caso | Interruptor | Dónde se ve | Qué mide hb-performance | Camino correcto (interruptor apagado) |
|---|---|---|---|---|---|
| 6 | El buscador de empresas reconstruye el adaptador y llama a `notifyDataSetChanged` en cada tecla, sin `debounce`, con un ítem de layout anidado y pesado. | `BILLERS_FULL_REBIND` / `billers_full_rebind` | Empresas, escribiendo "a" | `frame_p90_ms` y `hilo_max_ms` mientras se escribe | `debounce`, `ListAdapter` con `DiffUtil` y un layout plano. |
| 7 | La respuesta de recibos se decodifica en el hilo principal: falta `subscribeOn` en la cadena Rx. | `DEBTS_MAIN_DECODE` / `debts_main_decode` | Recibos de Luz del Norte | Tramo hasta la marca `legacy_recibos`; `hilo_max_ms` | `subscribeOn(io)` y `observeOn(main)`. |

Para que el caso 7 se note, el fixture de Luz del Norte trae, en el camino encendido, el histórico de los 24 meses.

---

## 6. Testabilidad

- Ids de `:legacy` en snake_case, con el prefijo `legacy_<pantalla>_<elemento>` (por ejemplo `legacy_empresas_buscar` o `legacy_recibos_lista`). Maestro los ve como resource-id.
- **Sin ids (S2):** los ítems de Empresas (por texto), el teclado de la clave (cada dígito es un nodo virtual de accesibilidad con su texto) y la Constancia (texto del WebView).
- Marcas de contenido visible, por la interfaz propia de `:legacy` que `app` conecta a `VisibleContentMarker`: `legacy_empresas`, `legacy_recibos` y `legacy_constancia` (tras `onPageFinished`).
- Goldens de Roborazzi de cada Fragment, en claro y oscuro.

**Datos de referencia**

| Empresa | Id | Rubro | Código de prueba | Recibos pendientes |
|---|---|---|---|---|
| Luz del Norte | BIL-LUZ | Electricidad | 100200 | 2: S/ 85.40 (oct.) y S/ 92.10 (nov.) |
| Agua Trujillo | BIL-AGUA | Agua | 300400 | 1: S/ 46.80 |
| Telefonía Andina | BIL-TEL | Telefonía | 500600 | 1: S/ 59.90 |
| Internet Moche | BIL-NET | Internet | 700800 | 0: "No tienes recibos pendientes" |

- **Clave de operaciones de Ana:** `1234`.
- **Pago de referencia** (desde datos limpios): los dos recibos de Luz del Norte (S/ 177.50) desde Cuenta Gastos. El saldo pasa de S/ 2,300.00 a S/ 2,122.50, con operación 80000001.
- **Casos negativos:**
  - El código 999999 da `CUSTOMER_NOT_FOUND`.
  - La clave 0000 da `INVALID_PIN`.
  - Pagar dos veces el mismo recibo con otra clave de idempotencia da `DEBT_ALREADY_PAID`.

---

## 7. Fuera de alcance

- Desplegar cualquier servidor (Dokploy u otro).
- Implementar el servidor real.
- Migrar `:legacy` a Compose o a corrutinas.
- Pagos recurrentes, recargas de celular y agenda de servicios.
- Notificaciones, diseño visual, flows de testigo e instrumentación de hb-performance. Estos dos últimos llegan después de cerrar la demo.

---

## 8. Bloques de implementación

Igual que en la fase 2: un `/goal` con un subagente de hb-implement por bloque, el DoD en verde y un commit por bloque. Al final, verificación en el emulador (debug y demo) con regresión de las fases 1 y 2, push, CI en verde y retro.

| Bloque | Contenido | Tamaño |
|---|---|---|
| 0 | Módulo `:legacy` (R1 y R2): Gradle, dependencias, ViewBinding, Hilt, gates y DoD para los dos módulos; `LegacyActivity` vacía, lanzada desde Servicios clásicos (R3); interfaces de `:legacy` conectadas por `app` (S4); AGENTS.md (S5). | M |
| l | Contrato, fixtures y backend del pago de servicios (§3.1 y §4); voucher HTML; `pin_pad_seed` en el escenario. | M |
| m | Empresas, Código de cliente, Recibos pendientes y Cuenta y resumen: MVP, RxJava, XML y RecyclerView. | L |
| n | Teclado de la clave sin ids, pago, Constancia en WebView y vuelta a Compose con invalidación de cuentas. | M |
| o | Contrato listo (§3.2): SemVer y changelog, ejemplos, `docs/CONTRATO.md`, Spectral en CI, tests de contrato, propiedad de `BASE_URL` y prueba con Prism. | M |
| p | Casos sembrados 6 y 7 (S6). | S |
