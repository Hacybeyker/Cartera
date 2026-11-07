#!/usr/bin/env python3
"""Genera api/fixtures/movements/ACC-00X.json con una semilla fija.

La salida es determinista: correr el script dos veces deja los mismos archivos. Los JSON
generados se versionan; solo hace falta volver a correrlo si cambian las reglas de abajo.

Uso: python3 -I api/tools/generate_movements.py
"""

import json
import random
from datetime import datetime, timedelta, timezone
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

SEED = 20261107
LIMA = timezone(timedelta(hours=-5))
ANCHOR = datetime(2026, 11, 6, 18, 45, tzinfo=LIMA)
IGV = Decimal("0.18")
CENT = Decimal("0.01")
OUTPUT = Path(__file__).resolve().parent.parent / "fixtures" / "movements"

MERCHANTS = {
    "GROCERIES": [("Supermercado La Canasta", "Lima"), ("Minimarket Don Lucho", "Lima"), ("Mercado Fresco", "Miraflores")],
    "RESTAURANTS": [("Cevichería El Muelle", "Barranco"), ("Pollería La Brasa", "Surco"), ("Café Central", "Lima")],
    "TRANSPORT": [("Taxi Seguro", "Lima"), ("Grifo Andino", "San Isidro"), ("Línea Metropolitana", "Lima")],
    "SHOPPING": [("Tienda Novedades", "Jesús María"), ("Librería El Saber", "Lima"), ("Electro Hogar", "San Borja")],
    "SERVICES": [("Luz del Pacífico", "Lima"), ("Agua Clara", "Lima"), ("Fibra Hogar Internet", "Lima")],
    "HEALTH": [("Farmacia Bienestar", "Pueblo Libre"), ("Clínica San Gabriel", "Lince")],
    "ENTERTAINMENT": [("Cine Estelar", "Surco"), ("Streaming Plus", "Lima")],
}
DEBIT_CATEGORIES = ["GROCERIES", "RESTAURANTS", "TRANSPORT", "SHOPPING", "SERVICES", "HEALTH", "ENTERTAINMENT"]
DEBIT_RANGES = {
    "GROCERIES": (35, 420),
    "RESTAURANTS": (18, 160),
    "TRANSPORT": (8, 120),
    "SHOPPING": (25, 900),
    "SERVICES": (45, 260),
    "HEALTH": (15, 350),
    "ENTERTAINMENT": (12, 95),
}
CHANNELS = {"GROCERIES": "POS", "RESTAURANTS": "POS", "TRANSPORT": "POS", "SHOPPING": "ONLINE",
            "SERVICES": "APP", "HEALTH": "POS", "ENTERTAINMENT": "ONLINE"}
PRODUCTS = [
    "Arroz extra 5 kg", "Aceite vegetal 1 L", "Azúcar rubia 1 kg", "Leche evaporada x6", "Huevos x15",
    "Pan de molde", "Fideos spaghetti", "Atún en lata x3", "Pollo entero", "Carne molida 1 kg",
    "Papa amarilla 2 kg", "Cebolla roja 1 kg", "Tomate 1 kg", "Limón 1 kg", "Palta fuerte 1 kg",
    "Plátano de seda", "Manzana delicia 1 kg", "Queso fresco", "Yogur natural 1 L", "Mantequilla",
    "Café molido", "Té filtrante x25", "Galletas de soda", "Mermelada de fresa", "Avena 900 g",
    "Detergente 2 kg", "Lavavajilla", "Papel higiénico x12", "Jabón de tocador x3", "Champú 750 ml",
    "Pasta dental", "Agua mineral 2.5 L", "Gaseosa 3 L", "Jugo de naranja 1 L", "Chocolate de taza",
    "Lentejas 1 kg", "Garbanzos 500 g", "Sal yodada", "Orégano seco", "Ají amarillo en pasta",
]


def money(value, currency):
    return {"value": str(value.quantize(CENT)), "currency": currency}


def cents(rng, low, high):
    return Decimal(rng.randint(low * 100, high * 100)) / 100


def with_igv(total):
    subtotal = (total / (1 + IGV)).quantize(CENT, rounding=ROUND_HALF_UP)
    return subtotal, total - subtotal


def debit(rng, account, index, date, currency, category=None):
    category = category or rng.choice(DEBIT_CATEGORIES)
    name, city = rng.choice(MERCHANTS[category])
    total = cents(rng, *DEBIT_RANGES[category])
    subtotal, taxes = with_igv(total)
    return record(account, index, date, currency, {
        "description": f"Compra {name}",
        "category": category,
        "amount": -total,
        "type": "DEBIT",
        "channel": CHANNELS[category],
        "counterparty": {"name": name, "reference": f"VISA ****{account['card']}"},
        "lines": [("Consumo", subtotal)],
        "taxes": taxes,
        "merchant": {"name": name, "city": city},
    })


def credit(account, index, date, currency, description, category, amount, payer, reference, exchange_rate=None):
    return record(account, index, date, currency, {
        "description": description,
        "category": category,
        "amount": amount,
        "type": "CREDIT",
        "channel": "BRANCH" if category == "SALARY" else "APP",
        "counterparty": {"name": payer, "reference": reference},
        "lines": [("Abono", amount)],
        "taxes": Decimal("0"),
        "merchant": None,
        "exchangeRate": exchange_rate,
    })


def record(account, index, date, currency, data):
    breakdown = {
        "lines": [{"concept": concept, "amount": money(value, currency)} for concept, value in data["lines"]],
        "fees": money(Decimal("0"), currency),
        "taxes": money(data["taxes"], currency),
    }
    if data.get("exchangeRate"):
        breakdown["exchangeRate"] = data["exchangeRate"]
    breakdown["merchant"] = data["merchant"]
    breakdown["category"] = data["category"]
    return {
        "id": f"MOV-{account['number']}-{index:04d}",
        "accountId": account["id"],
        "date": date.isoformat(),
        "description": data["description"],
        "category": data["category"],
        "amount": money(data["amount"], currency),
        "type": data["type"],
        "operationNumber": f"{account['number'][-1]}{index:07d}",
        "channel": data["channel"],
        "status": "COMPLETED",
        "counterparty": data["counterparty"],
        "breakdown": breakdown,
    }


def big_grocery(rng, account, date):
    lines = [(product, cents(rng, 3, 48)) for product in PRODUCTS]
    subtotal = sum((value for _, value in lines), Decimal("0"))
    taxes = (subtotal * IGV).quantize(CENT, rounding=ROUND_HALF_UP)
    name, city = MERCHANTS["GROCERIES"][0]
    return record(account, 1, date, "PEN", {
        "description": f"Compra {name}",
        "category": "GROCERIES",
        "amount": -(subtotal + taxes),
        "type": "DEBIT",
        "channel": "POS",
        "counterparty": {"name": name, "reference": f"VISA ****{account['card']}"},
        "lines": lines,
        "taxes": taxes,
        "merchant": {"name": name, "city": city},
    })


def salary_account(rng):
    account = {"id": "ACC-001", "number": "001", "card": "4821"}
    movements, date = [], ANCHOR
    for index in range(1, 1201):
        if index % 40 == 0:
            movements.append(credit(account, index, date, "PEN", "Abono de remuneraciones", "SALARY",
                                    Decimal("6850.00"), "Empresa Demo S.A.C.", "Planilla mensual"))
        elif index % 17 == 0:
            amount = cents(rng, 50, 600)
            movements.append(credit(account, index, date, "PEN", "Transferencia recibida", "TRANSFER",
                                    amount, "Luis Ramírez", "999-0000001-0-01"))
        else:
            movements.append(debit(rng, account, index, date, "PEN"))
        date -= timedelta(minutes=rng.randint(90, 720))
    return movements


def expenses_account(rng):
    account = {"id": "ACC-002", "number": "002", "card": "9034"}
    date = datetime(2026, 11, 6, 14, 32, tzinfo=LIMA)
    movements = [big_grocery(rng, account, date)]
    for index in range(2, 31):
        date -= timedelta(minutes=rng.randint(240, 1800))
        movements.append(debit(rng, account, index, date, "PEN"))
    return movements


def dollar_account(rng):
    account = {"id": "ACC-003", "number": "003", "card": "1177"}
    movements, date = [], datetime(2026, 11, 5, 10, 15, tzinfo=LIMA)
    for index in range(1, 13):
        if index % 4 == 1:
            movements.append(credit(account, index, date, "USD", "Cambio de soles a dólares", "TRANSFER",
                                    cents(rng, 100, 400), "Ana Torres", "ACC-001", exchange_rate="3.7520"))
        else:
            movements.append(debit(rng, account, index, date, "USD", category=rng.choice(["SHOPPING", "ENTERTAINMENT"])))
        date -= timedelta(days=rng.randint(2, 9), minutes=rng.randint(0, 600))
    return movements


def write(name, movements):
    lines = ",\n".join(json.dumps(movement, ensure_ascii=False, separators=(",", ":")) for movement in movements)
    (OUTPUT / name).write_text(f"[\n{lines}\n]\n", encoding="utf-8")


def main():
    rng = random.Random(SEED)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    write("ACC-001.json", salary_account(rng))
    write("ACC-002.json", expenses_account(rng))
    write("ACC-003.json", dollar_account(rng))


if __name__ == "__main__":
    main()
