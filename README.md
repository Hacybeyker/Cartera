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
│   ├── asyncapi.yaml      # Contrato del canal en vivo /market/stream (AsyncAPI 3.0)
│   ├── fixtures/          # Datos del backend embebido
│   └── tools/             # Generadores de movimientos y de mercado (semilla fija)
├── app/src/main/java/com/hacybeyker/cartera/
│   ├── core/              # Compartido: design system y componentes comunes, dominio y datos de sesión,
│   │                      # configuración, primer uso, cuentas y movimientos, mercado y portafolio,
│   │                      # cliente del canal en vivo, paginación, cliente HTTP y marcas de contenido visible
│   ├── navigation/        # CarteraApp (raíz), AppNavDisplay (Navigation 3) y la pila de entrada
│   └── feature/<name>/    # splash, login, terms, onboarding, biometric, home, movements, transfer,
│                          # market, order, exchange, help y legacy
│                          # (domain / data / ui / di)
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
datos de `api/fixtures/` según el contrato de `api/openapi.yaml`, y el canal en vivo
`ws://127.0.0.1:8765/api/v1/market/stream` según `api/asyncapi.yaml`. Release apunta a
`https://cartera.invalid/api/v1/`, un marcador que no resuelve.

---

## 👤 Usuario de prueba

| Usuario | Contraseña | Nombre |
|---|---|---|
| `ana.torres` | `Cartera2026` | Ana Torres |

Sin OTP. Son datos de demo, públicos a propósito: solo existen en `api/fixtures/` y en el backend
embebido. La sesión vive en memoria, así que cada arranque en frío pide login (con clave o, si se
activó, con huella).

| Cuenta | Alias | Moneda | Saldo disponible | Movimientos |
|---|---|---|---|---|
| ACC-001 | Cuenta Sueldo | PEN | S/ 8,450.75 | 1.200 |
| ACC-002 | Cuenta Gastos | PEN | S/ 2,300.00 | 30 |
| ACC-003 | Cuenta Dólares | USD | US$ 1,050.20 | 12 |

Destinos de transferencia: `999-0000001-0-01` (Luis Ramírez, PEN), `999-0000002-0-02` (Comercial
Andina S.A.C., PEN) y `999-0000003-0-10` (María Quispe, USD). Cualquier otro número da
`DESTINATION_NOT_FOUND`; enviar soles a la cuenta en dólares da `CURRENCY_MISMATCH`.

### Mercado y cambio de demo

Ocho acciones ficticias en dólares. Sus precios y el tipo de cambio avanzan por ticks pregenerados con
semilla (`api/fixtures/market/` y `api/fixtures/fx/`): el primero es el precio inicial y, al agotarse,
vuelven a empezar. Con `tick_ms 0` quedan congelados en estos valores:

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

| Portafolio de Ana | Cantidad | Precio promedio (US$) |
|---|---|---|
| ANDM | 10 | 40.00 |
| MOCH | 5 | 80.00 |
| QORI | 2 | 120.00 |

- Las órdenes son a mercado, por cantidad entera, y se pagan y cobran en Cuenta Dólares (ACC-003).
  Comprar 2 ANDM cuesta US$ 84.20: ACC-003 pasa a US$ 966.00 y ANDM queda en 12 a 40.35. Después,
  vender 1 QORI acredita US$ 115.20: ACC-003 queda en US$ 1,081.20 y QORI en 1.
- Tipo de cambio USD/PEN inicial: compra 3.742, venta 3.768. Cada consulta emite una cotización que
  vence a los 30 s, o a lo que diga `quote_ttl_s`. Desde datos limpios, cambiar US$ 100.00 de ACC-003
  a ACC-002 acredita S/ 374.20: ACC-003 queda en US$ 950.20 y ACC-002 en S/ 2,674.20.
- Ingreso con huella: `POST /auth/biometric/enroll` entrega un `deviceToken` que luego se cambia por
  una sesión en `POST /auth/biometric/login`; `DELETE /auth/biometric` lo revoca.
- Si cambia `api/tools/generate_market.py`, los fixtures se regeneran con
  `python3 -I api/tools/generate_market.py`.

### Canal en vivo

`WS /market/stream` (contrato en `api/asyncapi.yaml`) abre con el mismo Bearer de la API: sin token
o vencido responde 401, y la latencia, los fallos y `backend_down` del endpoint `market_stream` le
aplican como a cualquier otro. El cliente envía `{"type":"subscribe","symbols":["ANDM"],"pairs":["USD/PEN"]}`
(o `unsubscribe`) y recibe enseguida el valor actual de lo que nombró; después, en cada tick, un
`quote` por símbolo suscrito y un `fx` por par suscrito.

- Un solo reloj de mercado para todas las conexiones, al `tick_ms` del último handshake: dos
  conexiones no aceleran los precios, y lo que muestra el canal es el precio con el que se ejecutan
  las órdenes. Con `tick_ms 0` los precios no se mueven.
- Al vencer la sesión del token (`session_ttl`), el servidor cierra con 4401.
- Con `stream_drop_s 15`, a los 15 s de cada conexión el servidor corta el socket sin cierre, como una
  caída de red.
- `RESET_DATA` no cierra el canal: devuelve los precios al inicio y envía a cada conexión el valor
  actual de lo suscrito.

**Logs del canal** (etiqueta `CarteraBackend`), una línea por evento y con el id de la conexión:

```text
GET /api/v1/market/stream 101 WS#1 3ms latency=0ms X-Request-Id=req-000014
WS#1 abierto user=USR-001 tick_ms=2000 stream_drop_s=0 quote_ttl_s=30 abiertas=1
WS reloj cada 2000 ms
WS#1 subscribe → ANDM 42.10 QORI 115.20 USD/PEN 3.742/3.768
WS#1 tick 12 → ANDM 42.15 QORI 115.30 USD/PEN 3.744/3.770
WS#1 unsubscribe → QORI
WS#1 mensaje ignorado: type debe ser subscribe o unsubscribe
WS#1 corte a los 15 s
WS#1 cerrado code=1006 por=servidor abiertas=0 motivo=corte de demo
WS reloj detenido
```

Cada conexión escribe exactamente una línea `abierto` y una `cerrado`, con `code=1000` (u otro) y
`por=cliente` si cerró la app, `code=4401 por=servidor` si venció la sesión, `code=1006 por=servidor`
en un corte de demo y `code=1006 por=red` si se cayó el socket. Para contar aperturas y cierres:

```bash
adb logcat -d -s CarteraBackend | grep -cE 'WS#[0-9]+ abierto '
adb logcat -d -s CarteraBackend | grep -cE 'WS#[0-9]+ cerrado '
adb logcat -d -s CarteraBackend | grep -E 'WS#[0-9]+ cerrado code=4401'
```

---

## 🚪 Recorrido de entrada

```
Splash ──► Login [Ingresar con huella*] ──► Términos* ──► Onboarding* ──► Oferta de huella* ──► Home [coach marks*]
```

- **Splash** pide `GET /app/config`. Si falla o el backend está caído, muestra el error y Reintentar
  en la misma pantalla.
- **Login**: usuario y contraseña incorrectos se avisan bajo el formulario; un error de red o del
  servidor, en un modal con Reintentar y Cancelar. Mientras ingresa, un progreso bloquea la pantalla.
  Si el dispositivo está registrado para ingresar con huella, muestra además "Ingresar con huella"
  (ver [Ingreso con huella](#ingreso-con-huella)).
- **Términos** (\*) aparece si la versión aceptada en el dispositivo difiere de `termsVersion`.
  Aceptar se activa al marcar la casilla; Rechazar (o Atrás) descarta la sesión y vuelve a Login.
- **Onboarding** (\*) aparece una vez por instalación: tres páginas con Siguiente, Omitir en todas y
  Empezar en la última.
- **Oferta de huella** (\*) aparece después del onboarding (o del login, si el onboarding ya se vio)
  solo si el dispositivo tiene una huella registrada y todavía no está registrado para ingresar con
  ella. Se resuelve una vez por instalación, con Activar u Omitir.
- **Coach marks** (\*) aparecen la primera vez que se entra a Home, después de la oferta de huella si
  la hubo (ver [Coach marks](#coach-marks)).
- Al terminar, la pila queda en `[Home]` y Atrás sale de la app.

Los términos aceptados, el onboarding visto, la oferta de huella resuelta y los coach marks vistos se
guardan en SharedPreferences: `pm clear` (o `clearState` en Maestro) los vuelve a mostrar. El token vive solo en memoria. Si una llamada
autenticada responde `SESSION_EXPIRED` o `UNAUTHORIZED` (por ejemplo, con `session_ttl` corto o tras
la muerte del proceso), aparece el modal "Tu sesión expiró"; Ingresar deja la pila en `[Login]` y,
tras el nuevo login, no se repiten Términos ni Onboarding.

### Ingreso con huella

- **Activar** (en la oferta o en Más → Seguridad) abre el diálogo de huella del sistema. Con una huella
  válida, la app registra el dispositivo (`POST /auth/biometric/enroll`) y guarda solo el `deviceToken`
  cifrado con una clave AES del Android Keystore que exige la huella en cada uso y se invalida si se
  registra una huella nueva. Cancelar el diálogo no llama al servidor ni guarda nada.
- **Ingresar con huella** abre el diálogo, descifra el `deviceToken` y lo cambia por una sesión
  (`POST /auth/biometric/login`), igual que el login con clave y con el mismo `session_ttl`; después
  siguen Términos u Onboarding si corresponden. La sesión sigue solo en memoria.
- Si el servidor ya no reconoce el dispositivo (`INVALID_DEVICE`, por ejemplo tras `RESET_DATA`) o se
  registró una huella nueva, la app borra lo guardado, oculta el botón y lo explica bajo el formulario.
- **Más → Seguridad**: el interruptor "Ingresar con huella" activa el registro o lo desactiva
  (`DELETE /auth/biometric` y se borran el token y la clave). Sin huella en el dispositivo queda
  deshabilitado. Cerrar sesión no borra el `deviceToken`.
- El `deviceToken` cifrado queda fuera de los backups. `pm clear` (o `clearState`) lo borra y vuelve a
  mostrar la oferta.
- El backend embebido recuerda los dispositivos registrados aunque se cierre la app (es lo único de su
  estado que sobrevive al proceso); RESET_DATA, desactivar en Seguridad o `pm clear` los olvidan.

**En el emulador.** El diálogo de huella es del sistema y no tiene testTags: se resuelve por adb.

```bash
# 1. Un PIN de bloqueo (la huella lo exige)
adb shell locksettings set-pin 1111
# 2. Abrir el registro de huella, ingresar el PIN y, cada vez que pida el dedo, tocar y soltar
#    (unas 15 veces, hasta "Fingerprint added")
adb shell am start -a android.settings.FINGERPRINT_ENROLL
adb -e emu finger touch 1 && adb -e emu finger remove 1
# 3. Con el diálogo de la app abierto, resolverlo con la misma huella (otro id, p. ej. 2, falla)
adb -e emu finger touch 1
```

---

## 🏠 Home y movimientos

```
Home  [barra inferior: Inicio · Mercado · Cambio · Más; coach marks la primera vez]
├─ Inicio  [botón de menú ──► Menú lateral; tabs: Cuentas · Movimientos]
│   ├─ Cuentas ──► Movimientos de cuenta ──► Detalle ──► Desglose
│   │     └─ Transferir ──► Confirmación ──► Resultado ──► Home (Inicio · Cuentas)
│   └─ Movimientos ──► Detalle ──► Desglose
├─ Mercado ──► Detalle de acción ──► [Operar: Comprar · Vender] ──► Orden ──► Confirmación ──► Resultado ──► Home (Mercado)
│     └─ Mi portafolio ──► Detalle de acción
├─ Cambio ──► Confirmación ──► Resultado ──► Home (Cambio)
└─ Más ──► Seguridad · Servicios clásicos (próximamente) · Cerrar sesión · Versión
Menú lateral ──► Mi portafolio · Cambio (tab) · Seguridad · Ayuda (próximamente) · Cerrar sesión
```

- Cada tab de Home conserva su estado (scroll y tab superior) mientras se usa otro.
- **Inicio** pide `GET /me` y `GET /accounts` en paralelo. **Movimientos** carga al abrir el tab.
- Las listas de movimientos se paginan por cursor, de a 20. Si falla la carga de más, aparece una fila
  con Reintentar al final y lo ya cargado se mantiene; el scroll no reintenta solo.
- **Movimientos de cuenta** pide la cuenta y su primera página en paralelo. Detalle y Desglose se
  apilan sobre Home (sin barra inferior) y vuelven con `barra_volver` o Atrás. Un id que no existe
  (`NOT_FOUND`) se muestra como error sin Reintentar.
- **Cerrar sesión** pide confirmación y deja la pila en `[Login]`; el próximo login no repite
  Términos ni Onboarding.

### Menú lateral

- Lo abre solo el botón `home_menu` de la barra de Inicio (no está en los otros tabs). Deslizar no lo
  abre, solo lo cierra; también se cierra con Atrás o tocando fuera.
- **Mi portafolio** y **Seguridad** abren su pantalla cuando el menú terminó de cerrarse; Atrás vuelve
  a Inicio con el menú cerrado. **Cambio** cierra el menú y elige el tab Cambio. **Ayuda** muestra
  "Próximamente" en el menú y abre una pantalla reservada, como Servicios clásicos.
- **Cerrar sesión** cierra el menú y pide la misma confirmación que Más, con el mismo resultado: la
  pila queda en `[Login]`.

### Coach marks

- La primera vez que se entra a Home (una vez por instalación), una capa oscurece la pantalla y
  resalta, uno por paso, Transferir, el tab Mercado y el botón del menú, con su texto, **Siguiente**
  (**Entendido** en el último paso) y **Omitir**. La capa no deja tocar lo de abajo.
- Omitir o Entendido la resuelven para siempre: no reaparece al volver a Home, tras cerrar sesión ni
  tras un arranque en frío. Si se cierra la app a mitad de camino, vuelve a empezar en el paso 1.
- Mientras Inicio carga, el paso 1 se ve sin resaltado (Transferir todavía no está en pantalla).
- Las marcas de contenido visible de Inicio salen igual con la capa encima.
- **Para los flows de testigo:** tras un `pm clear` (o `clearState`), la capa `coach_capa` aparece al
  entrar a Home; ciérrala con `coach_omitir` antes de seguir (o recorre los pasos con
  `coach_siguiente`, que en el último dice "Entendido").

### Mercado

- Carga al abrir el tab: `GET /market/stocks` y `GET /portfolio` en paralelo. Arriba, **Mi
  portafolio** con el valor de mercado y la variación del día; debajo, las ocho acciones con su precio
  y su variación contra el cierre anterior.
- Mientras el tab se ve, los precios llegan por el canal en vivo y cambian solos, fila por fila; la
  cabecera se recalcula en la app con las mismas reglas que `GET /portfolio`.
- Si el canal se cae, aparece el aviso **"Reconectando…"** y los precios quedan en su último valor
  hasta que vuelve.
- Tocar una acción abre su **Detalle** y tocar la cabecera abre **Mi portafolio**.
- El portafolio se guarda en memoria hasta que cambian las tenencias; al volver a Mercado tras ese
  cambio se vuelve a pedir solo `GET /portfolio`.

**El canal en la app.** Hay una sola conexión para toda la app, que se abre con la primera pantalla
que sigue precios (Mercado, Detalle de acción, Mi portafolio, el formulario de una orden o Cambio) y se cierra 1 s después de que no queda ninguna: al cambiar de tab,
al abrir otra pantalla sin precios, al pasar a segundo plano o al cerrar sesión. Si se cae, reconecta
esperando 1, 2, 4 y 8 s (y 8 s desde ahí), vuelve a suscribir todo y reinicia la espera tras conectar.
Un 401 en el handshake o un cierre 4401 abren "Tu sesión expiró" y no reconectan. Con
`stream_drop_s 15` se ve el aviso unos instantes cada 15 s y, en `CarteraBackend`, un `WS#n cerrado
code=1006 por=servidor` seguido de un `WS#n+1 abierto` alrededor de 1 s después.

### Detalle de acción

- Pide en paralelo `GET /market/stocks/{symbol}`, su historial en 1D y `GET /portfolio`. Muestra el
  precio en vivo con su variación del día, un gráfico de línea con los rangos **1D · 1S · 1M · 1A**
  (en el contrato, `1D`, `1W`, `1M` y `1Y`), el mínimo y el máximo de la serie, los datos del día
  (apertura, máximo, mínimo, cierre anterior, volumen y descripción) y, si hay tenencia, la cantidad,
  el precio promedio, el valor de mercado y la ganancia o pérdida.
- Cambiar de rango pide solo `GET …/history?range=`; si falla, el error y su Reintentar quedan en el
  lugar del gráfico y el resto del detalle sigue a la vista.
- El último punto de la serie es el precio vigente. En 1D, cada precio del canal mueve el precio, la
  tenencia y el último punto del gráfico; en 1S, 1M y 1A el gráfico queda como se cargó y solo cambian
  el precio y la tenencia.
- El gráfico se dibuja con el Canvas de Compose, sin librerías: el trazado de los puntos fijos se arma
  una vez por serie y el precio vivo solo mueve el último tramo.
- Una acción que no existe (`NOT_FOUND`) se muestra como error sin Reintentar. Si el canal se cae,
  aparece "Reconectando…" como en Mercado.

### Mi portafolio

- Cabecera con el valor de mercado, la ganancia o pérdida total y la variación del día; debajo, cada
  tenencia con cantidad, precio promedio, precio actual, valor de mercado y ganancia o pérdida, que se
  recalculan en vivo, fila por fila. Tocar una tenencia abre su Detalle.
- Usa el portafolio en memoria de Mercado y vuelve a pedir `GET /portfolio` al mostrarse solo si
  cambiaron las tenencias. Sin tenencias, muestra el estado vacío y no abre el canal.

### Comprar y vender

- **Operar**, fijo al pie del Detalle de acción, abre un bottom sheet con **Comprar** y **Vender**.
  Vender queda deshabilitado, con un texto que lo explica, si no hay acciones de ese símbolo.
- **Formulario:** cantidad entera, cuenta (un bottom sheet con las cuentas en dólares, la única
  preseleccionada) y el **total estimado** = cantidad × precio en vivo del canal. Las cuentas y la
  tenencia salen de las cachés, sin llamadas nuevas. Continuar valida en el cliente: cantidad mayor
  que cero, el total no mayor que el saldo disponible (compra) y no más acciones de las que se tienen
  (venta). Mientras no llega el precio, Continuar queda deshabilitado. Atrás con datos pregunta
  "¿Salir sin comprar?" o "¿Salir sin vender?".
- **Confirmación:** acción, operación, cantidad, precio de referencia, total estimado y cuenta, con
  "El precio final puede variar". Confirmar envía `POST /orders` con progreso bloqueante y la
  `Idempotency-Key` creada al tocar Continuar; Reintentar (en el modal de error de red o del servidor)
  reenvía la misma. Saldo y acciones insuficientes tienen su propio modal, que vuelve al formulario,
  y un `VALIDATION_ERROR` se muestra bajo cada fila del resumen. Cancelar pide confirmación y vuelve
  al Detalle.
- **Resultado:** n.º de operación, precio ejecutado, total, nuevo saldo de la cuenta y nueva
  tenencia. **Volver a Mercado** (o Atrás) vuelve a la entrada de Home que ya existe, con el tab
  Mercado elegido. Si la orden terminó sin respuesta, el resultado dice "No pudimos confirmar la
  orden".
- Una orden confirmada, o que terminó sin respuesta o con un error del servidor, invalida las cuentas
  y el portafolio: Mercado, Mi portafolio y el Detalle vuelven a pedir solo `GET /portfolio` al
  mostrarse, e Inicio solo `GET /accounts`. Los movimientos de una orden son de la categoría
  **Inversiones** (`INVESTMENT`).

| Caso (`tick_ms 0`) | Datos | Resultado |
|---|---|---|
| Compra de referencia | 2 ANDM desde ACC-003 | total US$ 84.20; precio US$ 42.10, saldo US$ 966.00 y tenencia 12 a US$ 40.35 |
| Venta de referencia (después de la compra) | 1 QORI en ACC-003 | acredita US$ 115.20; saldo US$ 1,081.20 y tenencia 1 |
| Saldo insuficiente | un total mayor que el saldo (el cliente lo frena; el servidor responde `INSUFFICIENT_FUNDS`) | "El total supera el saldo disponible." o modal "Saldo insuficiente" |
| Acciones insuficientes | vender más de las que se tienen (el cliente lo frena; el servidor responde `INSUFFICIENT_HOLDINGS`) | "La cantidad supera las acciones que tienes." o modal "Acciones insuficientes" |

### Cambio de divisas

- Carga al abrir el tab: las cuentas (de la memoria de Inicio) y `GET /fx/rates` en paralelo. Arriba,
  la **compra** y la **venta** de USD/PEN, que cambian solas por el canal en vivo mientras el tab se
  ve; si el canal se cae, aparece "Reconectando…" y quedan en su último valor.
- **Calculadora:** el sentido (de dólares a soles o de soles a dólares; invertirlo intercambia las
  cuentas), el monto en la moneda de origen, la cuenta de origen y la de destino (cada una en el
  bottom sheet de cuentas, solo con las de su moneda; con una sola posible, ya queda elegida) y lo que
  se recibe, en vivo. Si el banco compra los dólares se aplica la **compra** (monto × compra); si los
  vende, la **venta** (monto ÷ venta); a 2 decimales y mitad hacia arriba, igual que el servidor.
  **¿Cómo calculamos el tipo de cambio?** abre un bottom sheet con la explicación y un ejemplo con la
  cotización de ahora.
- **Continuar** valida en el cliente (monto mayor que cero, con hasta dos decimales, no mayor que el
  saldo del origen y que alcance un centavo de la otra moneda; origen y destino de las monedas del
  sentido) y, si todo está bien, pide una cotización nueva con `GET /fx/rates`: la confirmación
  empieza con la vigencia completa (`quote_ttl_s`) y una `Idempotency-Key` nueva.
- **Confirmación:** origen, destino, monto a debitar, monto a acreditar, tipo de cambio y una cuenta
  regresiva hasta el vencimiento de la cotización. No sigue al canal: la tasa es la de la cotización.
  Confirmar envía `POST /fx/exchanges` con progreso bloqueante; Reintentar (en el modal de error de red
  o del servidor) reenvía la misma clave. Saldo insuficiente y monedas que no corresponden tienen su
  modal, que vuelve a Cambio, y un `VALIDATION_ERROR` se muestra bajo cada fila. Cancelar pide
  confirmación y vuelve a Cambio.
- **Cotización vencida:** al llegar el vencimiento la pantalla pide otra con `GET /fx/rates`, y si el
  servidor responde `QUOTE_EXPIRED` usa la que trae la respuesta. En los dos casos se abre "La
  cotización venció" con la tasa y el monto nuevos: **Usar la nueva** actualiza el resumen y reinicia
  la cuenta regresiva (el próximo Confirmar va con otra `Idempotency-Key`, porque es otro cuerpo);
  **Salir** vuelve a Cambio.
- **Resultado:** n.º de operación, monto debitado, monto acreditado, tipo de cambio y los nuevos
  saldos de las dos cuentas. **Volver a Cambio** (o Atrás) vuelve a la entrada de Home que ya existe,
  con el tab Cambio elegido y lo escrito en la calculadora. Si el cambio terminó sin respuesta, el
  resultado dice "No pudimos confirmar el cambio".
- Un cambio confirmado, o que terminó sin respuesta o con un error del servidor, invalida las cuentas:
  Cambio e Inicio vuelven a pedir solo `GET /accounts` al mostrarse.

| Caso (`tick_ms 0`, datos limpios) | Datos | Resultado |
|---|---|---|
| Cambio de referencia | US$ 100.00 de ACC-003 a ACC-002 a 3.742 | acredita S/ 374.20; ACC-003 queda en US$ 950.20 y ACC-002 en S/ 2,674.20 |
| Soles a dólares | S/ 376.80 de ACC-002 a ACC-003 a 3.768 | acredita US$ 100.00 |
| Cotización vencida | `quote_ttl_s 5` y esperar más de 5 s en la confirmación | modal "La cotización venció" con la cotización nueva |

### Transferir

- **Formulario:** cuenta de origen propia, número de destino (`999-0000001-0-01`), monto y
  descripción opcional. Continuar valida en el cliente: monto mayor que cero, con hasta dos decimales
  y no mayor que el saldo disponible. Atrás con datos pregunta "¿Salir sin transferir?".
- **Confirmación:** resumen, Confirmar (con progreso bloqueante) y Cancelar (con confirmación). Atrás
  vuelve al formulario con los datos. Un error de red o del servidor abre un modal con Reintentar,
  que reenvía la misma `Idempotency-Key`: un reintento nunca cobra dos veces.
- **Rechazos:** saldo insuficiente, destino inexistente y monedas distintas tienen su propio modal,
  que vuelve al formulario. Un `VALIDATION_ERROR` se muestra bajo cada campo del resumen.
- **Resultado:** n.º de operación y nuevo saldo. Volver al inicio (o Atrás) deja la pila en `[Home]`
  y Inicio pide las cuentas una sola vez, sin `/me`. Las cuentas se guardan en memoria: el formulario
  no las vuelve a pedir y solo una transferencia las invalida. El movimiento nuevo aparece en
  Movimientos la próxima vez que se muestren.

| Caso | Datos | Resultado |
|---|---|---|
| Referencia | ACC-002 → `999-0000001-0-01` por S/ 150.00 | ACC-002 queda en S/ 2,150.00 |
| Saldo insuficiente | un monto mayor que el saldo (el cliente lo frena; el servidor responde `INSUFFICIENT_FUNDS`) | modal "Saldo insuficiente" |
| Monedas distintas | ACC-001 o ACC-002 → `999-0000003-0-10` | modal "Monedas distintas" |
| Destino inexistente | cualquier otro número con formato válido | modal "Cuenta de destino inexistente" |

---

## 🏷️ testTags y marcas

La raíz expone los testTags como `resource-id` (`testTagsAsResourceId`), y cada modal hace lo mismo
en su ventana (también los bottom sheets). La convención es snake_case en español, `<pantalla>_<elemento>`, con los estados
`<pantalla>_cargando`, `<pantalla>_error`, `<pantalla>_reintentar` y `<pantalla>_vacio`.

| Pantalla | testTags |
|---|---|
| Comunes | `barra_volver`, `progreso_bloqueante`, `modal_sesion_expirada` (`_aceptar`), `modal_error` (`_reintentar`, `_cancelar`), `modal_confirmacion` (`_aceptar`, `_cancelar`) |
| Splash | `splash_pantalla`, `splash_cargando`, `splash_error`, `splash_reintentar` |
| Login | `login_pantalla`, `login_usuario`, `login_contrasena`, `login_mostrar_contrasena`, `login_entrar`, `login_huella`, `login_error` |
| Términos | sin testTags: "Términos y condiciones", "He leído y acepto…", "Aceptar", "Rechazar" |
| Onboarding | sin testTags: "Siguiente", "Omitir", "Empezar" |
| Oferta de huella | `biometria_oferta`, `biometria_activar`, `biometria_omitir` (el diálogo del sistema no tiene testTags) |
| Home | `home_pantalla`, `home_tab_inicio`, `home_tab_mercado`, `home_tab_cambio`, `home_tab_mas`, `home_menu` (botón de la barra de Inicio) |
| Menú lateral | `menu_lateral`, `menu_portafolio`, `menu_cambio`, `menu_seguridad`, `menu_ayuda`, `menu_cerrar_sesion` |
| Coach marks | `coach_capa`, `coach_paso_<n>` (1 a 3), `coach_siguiente` (también en el último paso, donde dice "Entendido"), `coach_omitir` |
| Inicio | `inicio_pantalla`, `inicio_saludo`, `inicio_tab_cuentas`, `inicio_tab_movimientos` |
| Inicio · Cuentas | `cuentas_lista`, `cuentas_item_<id>`, `cuentas_transferir` y estados `cuentas_*` |
| Inicio · Movimientos | `movimientos_lista`, `movimientos_item_<id>`, `movimientos_mas_cargando`, `movimientos_mas_reintentar` y estados `movimientos_*` |
| Movimientos de cuenta | `cuenta_pantalla`, `cuenta_saldo`, `cuenta_movimientos_lista`, `cuenta_movimientos_item_<id>`, `cuenta_movimientos_mas_reintentar` y estados `cuenta_*` |
| Detalle | `movimiento_detalle`, `movimiento_monto`, `movimiento_ver_desglose` y estados `movimiento_*` |
| Desglose | `movimiento_desglose`, `desglose_ilustracion`, `desglose_linea_<n>` (desde 1) y estados `desglose_*` |
| Mercado | `mercado_portafolio`, `mercado_lista`, `mercado_item_<symbol>`, `mercado_reconectando` y estados `mercado_*` |
| Cambio | `cambio_pantalla`, `cambio_compra`, `cambio_venta`, `cambio_direccion`, `cambio_monto`, `cambio_origen`, `cambio_destino`, `cambio_estimado`, `cambio_como_calculamos`, `cambio_continuar`, `cambio_reconectando`, `<campo>_error` (`cambio_monto`, `cambio_origen`, `cambio_destino`) y estados `cambio_*` |
| Sheet "Cómo calculamos" | `sheet_como_calculamos`, `sheet_como_calculamos_entendido` |
| Cambio · confirmación | `cambio_confirmacion`, `cambio_confirmacion_debito`, `cambio_confirmacion_credito`, `cambio_confirmacion_tasa`, `cambio_vencimiento`, `cambio_confirmar`, `cambio_cancelar`, `<campo>_error` (también `cambio_cotizacion`), `modal_cotizacion_vencida` (`_aceptar`, `_salir`), `modal_fondos_insuficientes`, `modal_moneda_distinta` (cada uno con `_aceptar`) |
| Cambio · resultado | `cambio_resultado`, `cambio_resultado_operacion`, `cambio_resultado_debitado`, `cambio_resultado_acreditado`, `cambio_resultado_tasa`, `cambio_resultado_saldo_origen`, `cambio_resultado_saldo_destino`, `cambio_resultado_error`, `cambio_volver` |
| Detalle de acción | `accion_detalle`, `accion_precio`, `accion_grafico`, `accion_rango_<1D\|1S\|1M\|1A>`, `accion_tenencia`, `accion_reconectando`, `accion_operar`, estados `accion_*` y, del gráfico, `accion_grafico_cargando`, `accion_grafico_error` y `accion_grafico_reintentar` |
| Sheet Comprar/Vender | `sheet_accion`, `sheet_accion_comprar`, `sheet_accion_vender`, `sheet_accion_vender_no_disponible` |
| Sheet de cuentas | `sheet_cuentas`, `sheet_cuentas_item_<id>` |
| Orden · formulario | `orden_pantalla`, `orden_precio`, `orden_cantidad`, `orden_cuenta`, `orden_total`, `orden_continuar`, `orden_reconectando`, `<campo>_error` y estados `orden_*` |
| Orden · confirmación | `orden_confirmacion`, `orden_confirmar`, `orden_cancelar`, `<campo>_error` (`orden_accion`, `orden_lado`, `orden_cantidad`, `orden_cuenta`), `modal_fondos_insuficientes`, `modal_tenencia_insuficiente` (cada uno con `_aceptar`) |
| Orden · resultado | `orden_resultado`, `orden_resultado_operacion`, `orden_resultado_precio`, `orden_resultado_total`, `orden_resultado_saldo`, `orden_resultado_tenencia`, `orden_resultado_error`, `orden_volver_mercado` |
| Mi portafolio | `portafolio_lista`, `portafolio_item_<symbol>`, `portafolio_reconectando` y estados `portafolio_*` (con `portafolio_vacio`) |
| Más | `mas_pantalla`, `mas_nombre`, `mas_seguridad`, `mas_servicios_clasicos`, `mas_cerrar_sesion`, `mas_version` y estados `mas_*` |
| Seguridad | `seguridad_pantalla`, `seguridad_huella` (interruptor) |
| Servicios clásicos | `legacy_proximamente` |
| Ayuda | `ayuda_proximamente` |
| Transferir · formulario | `transferir_pantalla`, `transferir_origen`, `transferir_origen_item_<id>`, `transferir_destino`, `transferir_monto`, `transferir_descripcion`, `transferir_continuar`, `<campo>_error` y estados `transferir_*` |
| Transferir · confirmación | `transferir_confirmacion`, `transferir_confirmar`, `transferir_cancelar`, `<campo>_error`, `modal_fondos_insuficientes`, `modal_destino_inexistente`, `modal_moneda_distinta` (cada uno con `_aceptar`) |
| Transferir · resultado | `transferir_resultado`, `transferir_resultado_operacion`, `transferir_resultado_saldo`, `transferir_resultado_error`, `transferir_volver_inicio` |
| Escenario (debug y demo) | `escenario_pantalla`, `escenario_<clave>` (las claves de adb), `escenario_failure_mode_<valor>`, `escenario_guardar`, `escenario_valores_defecto`, `escenario_restablecer_datos`, `escenario_mensaje` |

Términos y Onboarding no llevan testTags a propósito: imitan pantallas de terceros y se seleccionan
por texto.

**Marcas de contenido visible.** Cada pantalla avisa con el nombre de su marca tras el primer frame
con contenido real: una vez por cada carga o refresco, nunca por el estado de carga ni por cargar más
páginas. Rotar o volver a una pantalla sin datos nuevos no repite la marca. Marcas: `login`,
`terminos`, `onboarding`, `biometria_oferta`, `seguridad`, `inicio_cuentas`, `inicio_movimientos`, `cuenta_movimientos`,
`movimiento_detalle`, `movimiento_desglose`, `mercado`, `accion_detalle`, `portafolio`, `cambio`, `mas`,
`transferir_formulario`, `transferir_confirmacion`, `transferir_resultado`, `orden_formulario`,
`orden_confirmacion`, `orden_resultado`, `cambio_confirmacion` y `cambio_resultado`. `login` se avisa
cuando la pantalla ya sabe si muestra "Ingresar con huella". Tras una transferencia,
`inicio_cuentas` se dispara otra vez cuando llegan las cuentas refrescadas. `accion_detalle` se repite
con cada rango cargado. Los precios en vivo no repiten `mercado`, `portafolio` ni `accion_detalle`: solo
la carga y el refresco del portafolio. Las cotizaciones en vivo tampoco repiten `cambio`: solo la carga
y el refresco de las cuentas. Por defecto nadie escucha. Para
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
| Intervalo de ticks | `--ei tick_ms 0` | ms entre ticks del canal en vivo; 0 = precios congelados | 2000 |
| Corte del canal | `--ei stream_drop_s 15` | s desde la conexión hasta que el servidor corta el canal; 0 = nunca | 0 |
| Vigencia de la cotización | `--ei quote_ttl_s 5` | s; rige desde la próxima cotización | 30 |
| Página completa de movimientos | `--ez movements_full_page true` | los movimientos de una cuenta llegan en una sola página completa | `false` |
| Refresco anticipado de cuentas | `--ez accounts_eager_refresh true` | al volver al inicio tras una transferencia, las cuentas se refrescan de forma anticipada | `false` |
| Transición enriquecida del desglose | `--ez breakdown_rich_transition true` | el desglose entra con una transición enriquecida y su ilustración a tamaño completo | `false` |
| Valores por defecto | `--ez defaults true` | restablece el escenario antes de aplicar el resto | — |

Ids de endpoint: `app_config`, `login`, `me`, `accounts`, `account`, `movements`, `movement`,
`breakdown`, `transfers`, `market_stocks`, `market_stock`, `market_history`, `market_stream` (el
handshake del canal en vivo), `portfolio`, `orders`, `fx_rates`, `fx_exchanges`, `biometric_enroll`,
`biometric_login` y `biometric_delete`.

- `tick_ms` y `stream_drop_s` viajan solo en el handshake del canal en vivo; `quote_ttl_s`, en
  `GET /fx/rates` y en ese handshake. `session_ttl` rige también en el ingreso con huella.

- Los tres últimos son interruptores del cliente: no viajan al backend y tienen el mismo nombre en
  BuildConfig (`MOVEMENTS_FULL_PAGE`, `ACCOUNTS_EAGER_REFRESH`, `BREAKDOWN_RICH_TRANSITION`), donde
  valen `false`. Rigen desde la próxima vez que se usan, sin reiniciar la app. Release no los puede
  encender.
- Lo que no se envía no cambia. Un valor por endpoint sin `default=` usa el valor de fábrica como global.
- Un valor inválido no se aplica: el escenario efectivo y lo rechazado se imprimen en logcat con la
  etiqueta `CarteraDemo`.

**Por adb.** El receptor está protegido con `android.permission.DUMP`, así que solo adb puede usarlo.
`--include-stopped-packages` hace que el broadcast llegue también después de un `pm clear`.

```bash
adb shell am broadcast --include-stopped-packages -p com.hacybeyker.cartera \
  -a com.hacybeyker.cartera.demo.SET_SCENARIO \
  --es latency "default=400,movements=2000" --ei session_ttl 60

# Precios congelados y cotizaciones de 5 s
adb shell am broadcast --include-stopped-packages -p com.hacybeyker.cartera \
  -a com.hacybeyker.cartera.demo.SET_SCENARIO --ei tick_ms 0 --ei quote_ttl_s 5

# Volver a los fixtures (saldos, movimientos, tenencias, precios, cotizaciones, huella e idempotencia;
# los dispositivos registrados para la huella son lo único que sobrevive a cerrar la app)
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
      tick_ms: 0
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
