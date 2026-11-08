#!/usr/bin/env python3
"""Genera los fixtures del pago de servicios con una semilla fija.

Escribe api/fixtures/billers.json y api/fixtures/debts/<billerId>.json, uno por empresa. La salida es
determinista: correr el script dos veces deja los mismos archivos. Los JSON generados se versionan; solo hace
falta volver a correrlo si cambian las reglas de abajo.

Reglas que fijan los tests (FixturesTest):
- Las cuatro empresas de referencia van primero en su rubro, con los códigos y recibos del alcance.
- Las 16 de relleno no tienen clientes: cualquier código da CUSTOMER_NOT_FOUND. Su archivo de recibos existe,
  con la lista de clientes vacía, para que el backend lea todas las empresas de la misma forma.
- Luz del Norte trae el histórico de 24 meses del cliente 100200 (solo se envía con includeHistory=true). Cada
  mes suma sus líneas, con IGV, y el archivo mide al menos 150 KB.
- Los montos son Money en soles, con dos decimales.

Uso: python3 -I api/tools/generate_billers.py
"""

import calendar
import json
import random
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

SEED = 20261109
CENT = Decimal("0.01")
IGV = Decimal("0.18")
FIXTURES = Path(__file__).resolve().parent.parent / "fixtures"
CURRENCY = "PEN"
HISTORY_MONTHS = 24
HISTORY_START = (2024, 10)
MONTHS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]

SIX = r"^\d{6}$"

# id, name, category, customerCodeFormat. Las de referencia van primero en su rubro.
BILLERS = [
    ("BIL-LUZ", "Luz del Norte", "Electricidad", SIX),
    ("BIL-ESI", "Electro Sierra", "Electricidad", r"^\d{8}$"),
    ("BIL-ECO", "Energía Costa", "Electricidad", r"^\d{7}$"),
    ("BIL-AGUA", "Agua Trujillo", "Agua", SIX),
    ("BIL-AVA", "Agua del Valle", "Agua", r"^\d{8}$"),
    ("BIL-APA", "Aguas Pacífico", "Agua", r"^\d{10}$"),
    ("BIL-GAN", "Gas Andino", "Gas", r"^\d{8}$"),
    ("BIL-GCO", "Gas Costa Norte", "Gas", SIX),
    ("BIL-TEL", "Telefonía Andina", "Telefonía", SIX),
    ("BIL-MSO", "Móvil Sol", "Telefonía", r"^\d{9}$"),
    ("BIL-TVA", "Telecom Valle", "Telefonía", r"^\d{9}$"),
    ("BIL-NET", "Internet Moche", "Internet", SIX),
    ("BIL-FNO", "Fibra Norte", "Internet", r"^\d{8}$"),
    ("BIL-RHO", "Red Hogar", "Internet", r"^\d{10}$"),
    ("BIL-CVS", "Cable Visión Sur", "Cable", r"^\d{8}$"),
    ("BIL-TVC", "TV Andina Cable", "Cable", SIX),
    ("BIL-SHO", "Seguros Horizonte", "Seguros", r"^\d{10}$"),
    ("BIL-PFA", "Protección Familiar", "Seguros", r"^\d{8}$"),
    ("BIL-IAN", "Instituto Andino", "Educación", r"^\d{7}$"),
    ("BIL-AHO", "Academia Horizonte", "Educación", r"^\d{6}$"),
]

# biller id -> (customer code, customer name, [(period, due date, amount)])
CUSTOMERS = {
    "BIL-LUZ": ("100200", "Ana Torres", [("2026-10", "2026-11-12", "85.40"), ("2026-11", "2026-12-12", "92.10")]),
    "BIL-AGUA": ("300400", "Familia Torres", [("2026-10", "2026-11-18", "46.80")]),
    "BIL-TEL": ("500600", "Ana Torres", [("2026-10", "2026-11-20", "59.90")]),
    "BIL-NET": ("700800", "Ana Torres", []),
}

HISTORY_BILLER = "BIL-LUZ"

FIXED_CHARGES = [
    ("Cargo fijo mensual", "Cargo por cliente", "3.10"),
    ("Mantenimiento y reposición de conexión", "Cargo regulado por conexión", "1.85"),
    ("Alumbrado público", "Tarifa municipal por sector", "4.20"),
    ("Aporte por electrificación rural", "Ley de electrificación rural", "0.62"),
    ("Recargo FISE", "Fondo de inclusión social energético", "0.38"),
]


def money(value):
    return {"value": str(value.quantize(CENT, ROUND_HALF_UP)), "currency": CURRENCY}


def cents(value):
    return value.quantize(CENT, ROUND_HALF_UP)


def write(path, data, compact=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    if compact:
        text = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    else:
        text = json.dumps(data, ensure_ascii=False, indent=2)
    path.write_text(text + "\n", encoding="utf-8")


def month_after(year, month):
    return (year + 1, 1) if month == 12 else (year, month + 1)


def consumption_lines(rng, year, month, index):
    """Una línea por día y franja horaria: la tarifa sube un poco con cada mes."""
    rate_off_peak = Decimal("0.4180") + Decimal("0.0007") * index
    rate_peak = Decimal("0.5790") + Decimal("0.0009") * index
    lines = []
    for day in range(1, calendar.monthrange(year, month)[1] + 1):
        for label, low, high, rate in (
            ("fuera de punta", 1.5, 4.5, rate_off_peak),
            ("hora punta", 0.3, 1.4, rate_peak),
        ):
            kwh = Decimal(str(round(rng.uniform(low, high), 2))).quantize(CENT)
            lines.append(
                {
                    "concept": f"Consumo {day:02d} {MONTHS[month - 1]} · {label}",
                    "amount": money(kwh * rate),
                    "detail": f"{kwh} kWh × S/ {rate}",
                }
            )
    return lines


def billing_month(rng, year, month, index):
    lines = consumption_lines(rng, year, month, index)
    for concept, detail, amount in FIXED_CHARGES:
        lines.append({"concept": concept, "amount": money(Decimal(amount)), "detail": detail})
    subtotal = sum(Decimal(line["amount"]["value"]) for line in lines)
    tax = cents(subtotal * IGV)
    lines.append({"concept": "IGV (18 %)", "amount": money(tax), "detail": f"18 % sobre S/ {cents(subtotal)}"})
    paid_year, paid_month = month_after(year, month)
    paid_day = rng.randint(6, 12)
    paid_hour = rng.randint(9, 18)
    return {
        "period": f"{year}-{month:02d}",
        "amount": money(subtotal + tax),
        "paidAt": f"{paid_year}-{paid_month:02d}-{paid_day:02d}T{paid_hour:02d}:{rng.randint(0, 59):02d}:00-05:00",
        "lines": lines,
    }


def history(rng):
    year, month = HISTORY_START
    months = []
    for index in range(HISTORY_MONTHS):
        months.append(billing_month(rng, year, month, index))
        year, month = month_after(year, month)
    return months


def customer(biller_id, rng):
    code, name, debts = CUSTOMERS[biller_id]
    suffix = biller_id.removeprefix("BIL-")
    entry = {
        "code": code,
        "name": name,
        "debts": [
            {
                "id": f"DEB-{suffix}-{period.replace('-', '')}",
                "period": period,
                "dueDate": due,
                "amount": money(Decimal(amount)),
            }
            for period, due, amount in debts
        ],
    }
    if biller_id == HISTORY_BILLER:
        entry["history"] = history(rng)
    return entry


def main():
    rng = random.Random(SEED)
    write(
        FIXTURES / "billers.json",
        [
            {"id": biller_id, "name": name, "category": category, "customerCodeFormat": code_format}
            for biller_id, name, category, code_format in BILLERS
        ],
    )
    for biller_id, _, _, _ in BILLERS:
        customers = [customer(biller_id, rng)] if biller_id in CUSTOMERS else []
        has_history = any("history" in entry for entry in customers)
        write(
            FIXTURES / "debts" / f"{biller_id}.json",
            {"billerId": biller_id, "customers": customers},
            compact=has_history,
        )


if __name__ == "__main__":
    main()
