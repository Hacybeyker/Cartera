#!/usr/bin/env python3
"""Genera los fixtures de mercado, portafolio y tipo de cambio con una semilla fija.

Escribe api/fixtures/market/stocks.json, market/ticks/<SYMBOL>.json, market/history/<SYMBOL>.json,
portfolio.json y fx/rates.json. La salida es determinista: correr el script dos veces deja los mismos
archivos. Los JSON generados se versionan; solo hace falta volver a correrlo si cambian las reglas de abajo.

Reglas que fijan los tests (FixturesTest):
- El primer tick de cada acción es su precio inicial y el de fx es compra 3.742 / venta 3.768: con
  tick_ms 0 los precios quedan congelados en esos valores.
- El último punto de cada historial es el precio inicial: el gráfico empalma con el precio vivo.
- Precios con 2 decimales y tasas con 3, en un paseo aleatorio acotado alrededor del precio inicial.

Uso: python3 -I api/tools/generate_market.py
"""

import json
import random
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

SEED = 20261108
CENT = Decimal("0.01")
MILLI = Decimal("0.001")
FIXTURES = Path(__file__).resolve().parent.parent / "fixtures"
TICKS = 600

# symbol, name, initial price, description
STOCKS = [
    ("ANDM", "Andina Minerals", "42.10", "Minera de cobre y zinc con operaciones en la sierra central."),
    ("CUSC", "Cusco Telecom", "27.30", "Telefonía móvil e internet de fibra en el sur del país."),
    ("INKA", "Inka Energy", "63.40", "Generación hidroeléctrica y eólica conectada a la red nacional."),
    ("LIMA", "Lima Logistics", "9.85", "Almacenes y transporte de carga desde el puerto del Callao."),
    ("MOCH", "Moche Tech", "88.60", "Software de pagos y servicios en la nube para empresas."),
    ("NAZC", "Nazca Retail", "12.45", "Cadena de supermercados y tiendas de conveniencia."),
    ("PCFC", "Pacífico Foods", "18.75", "Conservas de pescado y productos del mar para exportación."),
    ("QORI", "Qori Gold", "115.20", "Exploración y producción de oro en el norte del país."),
]

# range, points, seconds between points, volatility per step, max distance from the initial price
HISTORY_RANGES = [
    ("1D", 390, 60, 0.0015, 0.04),
    ("1W", 168, 3600, 0.004, 0.08),
    ("1M", 120, 21600, 0.01, 0.15),
    ("1Y", 365, 86400, 0.02, 0.35),
]
TICK_VOLATILITY = 0.0012
TICK_BOUND = 0.03

PORTFOLIO = {
    "userId": "USR-001",
    "accountId": "ACC-003",
    "holdings": [
        {"symbol": "ANDM", "quantity": 10, "averagePrice": "40.00"},
        {"symbol": "MOCH", "quantity": 5, "averagePrice": "80.00"},
        {"symbol": "QORI", "quantity": 2, "averagePrice": "120.00"},
    ],
}

FX_BUY = Decimal("3.742")
FX_SELL = Decimal("3.768")
FX_VOLATILITY = 0.0004
FX_BOUND = 0.01


def walk(rng, start, steps, volatility, bound):
    """Paseo aleatorio que rebota en +-bound alrededor de start. Devuelve steps valores sin redondear."""
    low, high = start * (1 - bound), start * (1 + bound)
    values, current = [start], start
    for _ in range(steps - 1):
        current *= 1 + rng.gauss(0, volatility)
        if current > high:
            current = 2 * high - current
        elif current < low:
            current = 2 * low - current
        values.append(current)
    return values


def price(value, quantum=CENT):
    return str(Decimal(repr(value)).quantize(quantum, rounding=ROUND_HALF_UP))


def history(rng, initial):
    ranges = {}
    for name, points, step, volatility, bound in HISTORY_RANGES:
        backwards = walk(rng, float(initial), points, volatility, bound)
        prices = [price(value) for value in reversed(backwards)]
        prices[-1] = str(initial)
        ranges[name] = {"stepSeconds": step, "prices": prices}
    return ranges


def ticks(rng, initial):
    prices = [price(value) for value in walk(rng, float(initial), TICKS, TICK_VOLATILITY, TICK_BOUND)]
    prices[0] = str(initial)
    return prices


def stock(rng, symbol, name, initial, description, day):
    day_prices = [Decimal(value) for value in day]
    open_price = day_prices[0]
    previous_close = (open_price * Decimal(repr(1 + rng.uniform(-0.015, 0.015)))).quantize(CENT, ROUND_HALF_UP)
    return {
        "symbol": symbol,
        "name": name,
        "currency": "USD",
        "previousClose": str(previous_close),
        "open": str(open_price),
        "high": str(max(day_prices)),
        "low": str(min(day_prices)),
        "volume": rng.randint(150, 4800) * 1000,
        "description": description,
    }


def fx_ticks(rng):
    spread = FX_SELL - FX_BUY
    mid = (FX_BUY + FX_SELL) / 2
    ticks_out = []
    for value in walk(rng, float(mid), TICKS, FX_VOLATILITY, FX_BOUND):
        buy = (Decimal(repr(value)) - spread / 2).quantize(MILLI, ROUND_HALF_UP)
        ticks_out.append({"buy": str(buy), "sell": str(buy + spread)})
    ticks_out[0] = {"buy": str(FX_BUY), "sell": str(FX_SELL)}
    return ticks_out


def write(path, data, compact=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    if compact:
        text = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    else:
        text = json.dumps(data, ensure_ascii=False, indent=2)
    path.write_text(text + "\n", encoding="utf-8")


def main():
    rng = random.Random(SEED)
    stocks = []
    for symbol, name, initial, description in STOCKS:
        initial = Decimal(initial)
        ranges = history(rng, initial)
        stocks.append(stock(rng, symbol, name, initial, description, ranges["1D"]["prices"]))
        write(FIXTURES / "market" / "history" / f"{symbol}.json", {"symbol": symbol, "ranges": ranges}, compact=True)
        write(FIXTURES / "market" / "ticks" / f"{symbol}.json", {"symbol": symbol, "prices": ticks(rng, initial)},
              compact=True)
    write(FIXTURES / "market" / "stocks.json", stocks)
    write(FIXTURES / "portfolio.json", PORTFOLIO)
    write(FIXTURES / "fx" / "rates.json", {"pair": "USD/PEN", "ticks": fx_ticks(rng)}, compact=True)


if __name__ == "__main__":
    main()
