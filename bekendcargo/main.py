from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from typing import List, Optional, Literal, Dict
import random
import sqlite3
import logging
import os
from datetime import datetime

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class DeliveryDB:
    def __init__(self, db_path: str = "deliveries.db"):
        self.db_path = db_path
        self.init_db()

    def init_db(self):
        conn = None
        try:
            conn = sqlite3.connect(f"file:{self.db_path}?mode=rw", uri=True)
            cursor = conn.cursor()
            cursor.execute("PRAGMA integrity_check")
            result = cursor.fetchone()[0]

            if result != "ok":
                logger.warning(f"База повреждена: {result}. Пересоздаём...")
                conn.close()
                if os.path.exists(self.db_path):
                    os.remove(self.db_path)
                conn = sqlite3.connect(self.db_path)
            else:
                logger.info("База данных цела")

        except Exception as e:
            logger.warning(f"Ошибка проверки БД: {e}. Пересоздаём...")
            if conn:
                conn.close()
            if os.path.exists(self.db_path):
                os.remove(self.db_path)
            conn = sqlite3.connect(self.db_path)

        cursor = conn.cursor()

        cursor.execute('''
        CREATE TABLE IF NOT EXISTS deliveries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            company TEXT NOT NULL,
            delivery_type TEXT NOT NULL,
            weight REAL NOT NULL CHECK (weight > 0),
            size TEXT NOT NULL,
            town_from TEXT NOT NULL,
            town_to TEXT NOT NULL,
            price REAL NOT NULL CHECK (price >= 0),
            delivery_time INTEGER NOT NULL CHECK (delivery_time > 0),
            is_completed INTEGER NOT NULL DEFAULT 0,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
        ''')

        indexes = [
            "CREATE INDEX IF NOT EXISTS idx_delivery_type ON deliveries(delivery_type)",
            "CREATE INDEX IF NOT EXISTS idx_town_from ON deliveries(town_from)",
            "CREATE INDEX IF NOT EXISTS idx_town_to ON deliveries(town_to)",
            "CREATE INDEX IF NOT EXISTS idx_company ON deliveries(company)",
            "CREATE INDEX IF NOT EXISTS idx_created_at ON deliveries(created_at)",
        ]

        for idx_sql in indexes:
            try:
                cursor.execute(idx_sql)
            except sqlite3.Error as e:
                logger.warning(f"Пропуск индекса: {e}")

        conn.commit()
        conn.close()
        logger.info(f"База данных готова: {self.db_path}")

    def save_delivery(self, company: str, delivery_type: str, weight: float, size: str,
                      town_from: str, town_to: str, price: float, days: int) -> int:
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        try:
            cursor.execute('''
                INSERT INTO deliveries
                (company, delivery_type, weight, size, town_from, town_to, price, delivery_time, is_completed)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
            ''', (company, delivery_type, weight, size, f"г. {town_from}", f"г. {town_to}", price, days))

            delivery_id = cursor.lastrowid
            conn.commit()

            logger.info(f"Сохранено ID {delivery_id}: {company} | {price}₽ | {town_from} → {town_to}")
            return delivery_id

        except sqlite3.Error as e:
            logger.error(f"Ошибка сохранения в БД: {e}")
            raise
        finally:
            conn.close()

    def get_all_deliveries(self) -> List[Dict]:
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT
                    id, company, delivery_type, weight, size,
                    town_from, town_to, price, delivery_time, is_completed,
                    created_at
                FROM deliveries
                ORDER BY created_at DESC
            """)

            rows = cursor.fetchall()
            columns = [description[0] for description in cursor.description]

            return [dict(zip(columns, row)) for row in rows]

        except sqlite3.Error as e:
            logger.error(f"Ошибка получения данных: {e}")
            return []
        finally:
            conn.close()

    def get_deliveries_count(self) -> int:
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        try:
            cursor.execute("SELECT COUNT(*) FROM deliveries")
            count = cursor.fetchone()[0]
            return count
        except sqlite3.Error as e:
            logger.error(f"Ошибка подсчёта записей: {e}")
            return 0
        finally:
            conn.close()

    def clear_deliveries(self):
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        try:
            cursor.execute("DELETE FROM deliveries")
            conn.commit()
            logger.info("База данных очищена")
            return True
        except sqlite3.Error as e:
            logger.error(f"Ошибка очистки БД: {e}")
            return False
        finally:
            conn.close()

db = DeliveryDB("deliveries.db")

app = FastAPI(
    title="Delivery Aggregator API",
    description="Агрегатор доставки по 50 крупнейшим городам России с сохранением в БД",
    version="2.0",
    docs_url="/api/docs",
    redoc_url="/api/redoc"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["*"],
)

BoxSize = Literal["XS", "S", "M", "L", "XL", "XXL", "XXXL", "XXXXL"]
BOX_DIMENSIONS = {
    "XS": {"name": "XS (документы)", "dims": (20, 15, 5), "max_weight": 0.5},
    "S": {"name": "S (маленькая)", "dims": (30, 20, 15), "max_weight": 2},
    "M": {"name": "M (средняя)", "dims": (40, 30, 25), "max_weight": 5},
    "L": {"name": "L (стандартная)", "dims": (53, 38, 28), "max_weight": 10},
    "XL": {"name": "XL (большая)", "dims": (60, 40, 40), "max_weight": 15},
    "XXL": {"name": "XXL (очень большая)", "dims": (80, 60, 50), "max_weight": 25},
    "XXXL": {"name": "XXXL (грузовое)", "dims": (120, 80, 80), "max_weight": 40},
    "XXXXL": {"name": "XXXXL (паллета)", "dims": (120, 80, 150), "max_weight": 500},
}

CITIES = {
    "москва": 1, "санкт-петербург": 2, "калининград": 2, "нижний новгород": 3, "казань": 3,
    "самара": 3, "волгоград": 3, "ростов-на-дону": 4, "краснодар": 4, "воронеж": 4,
    "сочи": 4, "ставрополь": 4, "екатеринбург": 5, "челябинск": 5, "тюмень": 5,
    "пермь": 5, "уфа": 5, "новосибирск": 6, "омск": 6, "томск": 6, "барнаул": 6,
    "кемерово": 6, "красноярск": 7, "иркутск": 7, "хабаровск": 8, "владивосток": 8,
    "якутск": 9, "благовещенск": 9, "петропавловск-камчатский": 10, "магадан": 10,
    "южно-сахалинск": 10, "саратов": 3, "тольятти": 3, "ижевск": 5, "ульяновск": 3,
    "оренбург": 5, "новокузнецк": 6, "рязань": 2, "пенза": 3, "липецк": 2,
    "тула": 2, "астрахань": 4, "киров": 3, "чебоксары": 3, "калуга": 2,
    "курск": 2, "тверь": 2, "севастополь": 4, "брянск": 2, "иваново": 2,
    "магнитогорск": 5, "белгород": 2
}

class DeliveryOffer(BaseModel):
    company: str
    tariff_name: str
    cargo_type: str
    size: str
    transit_time: str
    price: int

class SearchResponse(BaseModel):
    from_city: str
    to_city: str
    weight_kg: float
    box_size: str
    offers: List[DeliveryOffer]

class TariffItem(BaseModel):
    company: str
    cargo_type: str
    tariff_type: str
    price: float
    days: int
    is_price_restored: bool = False
    is_time_restored: bool = False
    source_url: Optional[str] = None

class TariffsRequest(BaseModel):
    city: str
    weight: float
    strategy: str = "none"

class TariffsResponse(BaseModel):
    city: str
    weight: float
    avg_price: float
    avg_days: float
    tariffs: List[TariffItem]

def calculate_volume_weight(l: int, w: int, h: int) -> float:
    return round((l * w * h) / 5000, 2)

def get_zone_diff(city1: str, city2: str) -> int:
    c1 = city1.lower().replace("ё", "е")
    c2 = city2.lower().replace("ё", "е")
    z1 = CITIES.get(c1)
    z2 = CITIES.get(c2)

    if z1 is None or z2 is None:
        raise HTTPException(status_code=404, detail="Город не найден в списке")

    return abs(z1 - z2) + 1

def calculate_price(company: str, tariff: str, weight: float, vol_weight: float, zone_diff: int, box: str) -> int:
    charge_weight = max(weight, vol_weight)
    extra = 0

    if box == "XXL":
        extra = 1800
    elif box == "XXXL":
        extra = 4000
    elif box == "XXXXL":
        extra = 9000

    if company == "СДЭК":
        if "эконом" in tariff.lower():
            base, per_kg = 550, 130 + zone_diff * 38
        else:
            base, per_kg = 950, 380 + zone_diff * 55
    elif company == "Boxberry":
        base, per_kg = 680, 160 + zone_diff * 42
    elif company == "Почта России":
        if tariff == "EMS":
            base, per_kg = 1400, 500 + zone_diff * 70
        else:
            base, per_kg = 450, 110 + zone_diff * 25
    else:
        base, per_kg = 700, 200 + zone_diff * 40

    price = base + (charge_weight - 1) * per_kg + extra
    price = int(price * random.uniform(0.92, 1.08))
    return max(290, round(price / 10) * 10)

def get_offers(weight: float, box_size: BoxSize, zone_diff: int) -> List[DeliveryOffer]:
    dims = BOX_DIMENSIONS[box_size]["dims"]
    vol_weight = calculate_volume_weight(*dims)

    cargo_type = "Документы" if weight <= 0.5 else "Посылка" if weight <= 30 else "Груз"
    offers = []

    offers.append(DeliveryOffer(
        company="СДЭК",
        tariff_name="Экспресс лайт",
        cargo_type=cargo_type,
        size=box_size,
        transit_time=f"{max(1, zone_diff // 2)}-{zone_diff + 1} дн.",
        price=calculate_price("СДЭК", "экспресс", weight, vol_weight, zone_diff, box_size)
    ))

    offers.append(DeliveryOffer(
        company="СДЭК",
        tariff_name="Посылочка Эконом",
        cargo_type=cargo_type,
        size=box_size,
        transit_time=f"{zone_diff + 1}-{zone_diff + 5} дн.",
        price=calculate_price("СДЭК", "эконом", weight, vol_weight, zone_diff, box_size)
    ))

    offers.append(DeliveryOffer(
        company="Boxberry",
        tariff_name="Стандарт",
        cargo_type=cargo_type,
        size=box_size,
        transit_time=f"{zone_diff}-{zone_diff + 4} дн.",
        price=calculate_price("Boxberry", "", weight, vol_weight, zone_diff, box_size)
    ))

    offers.append(DeliveryOffer(
        company="Почта России",
        tariff_name="Обычная посылка",
        cargo_type=cargo_type,
        size=box_size,
        transit_time=f"{zone_diff + 3}-{zone_diff + 9} дн.",
        price=calculate_price("Почта России", "обычная", weight, vol_weight, zone_diff, box_size)
    ))

    offers.append(DeliveryOffer(
        company="Почта России",
        tariff_name="EMS",
        cargo_type=cargo_type,
        size=box_size,
        transit_time=f"{max(1, zone_diff // 3)}-{zone_diff // 2 + 2} дн.",
        price=calculate_price("Почта России", "EMS", weight, vol_weight, zone_diff, box_size)
    ))

    offers.sort(key=lambda x: x.price)
    return offers

def generate_tariffs_for_city(city: str, weight: float) -> List[TariffItem]:
    tariffs = []
    companies = ["СДЭК", "Boxberry", "Почта России", "Деловые Линии", "ПЭК", "КИТ"]
    cargo_types = ["Экспресс", "Сборный груз", "Терминал-Дверь", "Дверь-Дверь"]

    city_lower = city.lower()
    base_zone = CITIES.get(city_lower, 5)

    for i, company in enumerate(companies):
        cargo_type = cargo_types[i % len(cargo_types)]

        base_price = 500 + (weight * 50) + (base_zone * 100)
        price = base_price * random.uniform(0.8, 1.2)

        base_days = 2 + base_zone + int(weight / 10)
        days = max(1, base_days + random.randint(-1, 3))

        is_price_restored = random.random() < 0.3
        is_time_restored = random.random() < 0.2

        tariffs.append(TariffItem(
            company=company,
            cargo_type=cargo_type,
            tariff_type=f"Тариф {i + 1}",
            price=round(price, 2),
            days=days,
            is_price_restored=is_price_restored,
            is_time_restored=is_time_restored,
            source_url=f"https://{company.lower().replace(' ', '')}.ru/tariff"
        ))

    return tariffs

def calculate_and_save_delivery(input_string: str) -> str:
    try:
        parts = input_string.strip().split()
        if len(parts) != 4:
            return "Формат: 'Москва Санкт-Петербург 2.5 M'"

        from_city, to_city, weight_str, box_size = parts
        weight = float(weight_str)

        c1 = from_city.lower().replace("ё", "е")
        c2 = to_city.lower().replace("ё", "е")
        if c1 not in CITIES or c2 not in CITIES:
            return f"Города не найдены: {from_city} → {to_city}"

        if weight <= 0 or weight > 500:
            return f"Вес {weight}кг некорректен"

        if box_size not in BOX_DIMENSIONS:
            return f"Размер {box_size} неверный"

        if weight > BOX_DIMENSIONS[box_size]["max_weight"]:
            return f"Вес {weight}кг > лимит {box_size} ({BOX_DIMENSIONS[box_size]['max_weight']}кг)"

        zone_diff = get_zone_diff(from_city, to_city)
        offers = get_offers(weight, box_size, zone_diff)
        best_offer = offers[0]

        delivery_type = "экспресс лайт" if "экспресс" in best_offer.tariff_name.lower() else "посылочка (Эконом)"
        if "ems" in best_offer.tariff_name.lower():
            delivery_type = "EMS отправление"

        delivery_id = db.save_delivery(
            company=best_offer.company,
            delivery_type=delivery_type,
            weight=weight,
            size=box_size,
            town_from=from_city,
            town_to=to_city,
            price=best_offer.price,
            days=int(best_offer.transit_time.split("-")[0])
        )

        saved_records = db.get_all_deliveries()
        total_count = len(saved_records)
        last_record = saved_records[0] if saved_records else None
        saved_ok = delivery_id and last_record and last_record['id'] == delivery_id

        days = int(best_offer.transit_time.split("-")[0])
        result = f"""
ЛУЧШИЙ ТАРИФ:
{from_city.title()} → {to_city.title()}
{box_size} | {weight}кг
{best_offer.company} | {delivery_type}
{best_offer.price:,}₽
{days}-{int(best_offer.transit_time.split("-")[1][:-3])} дней
ID в БД: {delivery_id}

Сохранено: {'ДА' if saved_ok else 'НЕТ'}
Всего записей: {total_count}
        """
        return result.strip()

    except Exception as e:
        logger.error(f"Ошибка в calculate_and_save_delivery: {e}")
        return f"Ошибка: {str(e)}"

@app.get("/api/health")
async def health_check():
    deliveries_count = db.get_deliveries_count()
    return {
        "status": "ok",
        "timestamp": datetime.now().isoformat(),
        "database": "connected",
        "deliveries_count": deliveries_count,
        "cities_count": len(CITIES),
        "version": "2.0"
    }

@app.post("/api/tariffs", response_model=TariffsResponse)
async def get_tariffs(request: TariffsRequest):
    logger.info(f"Получен запрос на тарифы: город={request.city}, вес={request.weight}")

    tariffs = generate_tariffs_for_city(request.city, request.weight)

    if request.strategy == "cheapest":
        tariffs.sort(key=lambda x: x.price)
    elif request.strategy == "fastest":
        tariffs.sort(key=lambda x: x.days)
    elif request.strategy == "balanced":
        max_price = max(t.price for t in tariffs)
        max_days = max(t.days for t in tariffs)
        tariffs.sort(key=lambda t: (t.price / max_price * 0.7 + t.days / max_days * 0.3))

    avg_price = sum(t.price for t in tariffs) / len(tariffs)
    avg_days = sum(t.days for t in tariffs) / len(tariffs)

    if tariffs:
        best_tariff = tariffs[0]
        delivery_type = best_tariff.cargo_type
        if "экспресс" in best_tariff.cargo_type.lower():
            delivery_type = "Экспресс"
        elif "сборный" in best_tariff.cargo_type.lower():
            delivery_type = "Сборный груз"

        try:
            delivery_id = db.save_delivery(
                company=best_tariff.company,
                delivery_type=delivery_type,
                weight=request.weight,
                size="M",
                town_from="Москва",
                town_to=request.city,
                price=best_tariff.price,
                days=best_tariff.days
            )
            logger.info(f"Сохранено в БД с ID: {delivery_id}")
        except Exception as e:
            logger.error(f"Ошибка сохранения в БД: {e}")

    return TariffsResponse(
        city=request.city,
        weight=request.weight,
        avg_price=round(avg_price, 2),
        avg_days=round(avg_days, 1),
        tariffs=tariffs
    )

@app.get("/api/calculate")
async def calculate_delivery(
        from_city: str = Query(..., description="Город отправления"),
        to_city: str = Query(..., description="Город назначения"),
        weight: float = Query(..., gt=0, le=500, description="Вес в кг"),
        box_size: BoxSize = Query("M", description="Размер коробки")
):
    c1 = from_city.lower().replace("ё", "е")
    c2 = to_city.lower().replace("ё", "е")

    if c1 not in CITIES or c2 not in CITIES:
        raise HTTPException(status_code=404, detail="Город не найден в списке")
    if c1 == c2:
        raise HTTPException(status_code=400, detail="Города совпадают")

    if weight > BOX_DIMENSIONS[box_size]["max_weight"]:
        raise HTTPException(
            status_code=400,
            detail=f"Вес {weight}кг превышает лимит для коробки {box_size} ({BOX_DIMENSIONS[box_size]['max_weight']}кг)"
        )

    zone_diff = get_zone_diff(from_city, to_city)
    offers = get_offers(weight, box_size, zone_diff)

    for offer in offers[:3]:
        delivery_type = offer.tariff_name.lower().replace(" ", "_")
        if "экспресс лайт" in delivery_type:
            delivery_type = "экспресс лайт"
        elif "эконом" in delivery_type:
            delivery_type = "посылочка (Эконом)"
        elif "ems" in delivery_type:
            delivery_type = "EMS отправление"

        try:
            db.save_delivery(
                company=offer.company,
                delivery_type=delivery_type,
                weight=weight,
                size=box_size,
                town_from=from_city,
                town_to=to_city,
                price=offer.price,
                days=int(offer.transit_time.split("-")[0]) if "-" in offer.transit_time else zone_diff
            )
        except Exception as e:
            logger.error(f"Ошибка сохранения предложения {offer.company}: {e}")

    return SearchResponse(
        from_city=from_city.title(),
        to_city=to_city.title(),
        weight_kg=weight,
        box_size=f"{box_size} — {BOX_DIMENSIONS[box_size]['name']}",
        offers=offers
    )

@app.get("/api/test-calc")
async def test_calculation():
    test_inputs = [
        "Москва Санкт-Петербург 2.5 M",
        "Екатеринбург Новосибирск 10 L",
        "Казань Москва 0.3 XS"
    ]

    results = []
    for inp in test_inputs:
        result = calculate_and_save_delivery(inp)
        results.append({"input": inp, "output": result})

    return {"tests": results}

@app.get("/api/deliveries")
async def get_all_deliveries():
    deliveries = db.get_all_deliveries()
    return {
        "status": "success",
        "count": len(deliveries),
        "deliveries": deliveries
    }

@app.delete("/api/deliveries/clear")
async def clear_deliveries():
    success = db.clear_deliveries()

    if success:
        return {
            "status": "success",
            "message": "База данных очищена",
            "count": 0
        }
    else:
        raise HTTPException(status_code=500, detail="Ошибка очистки базы данных")

@app.get("/api/statistics")
async def get_statistics():
    deliveries = db.get_all_deliveries()

    if not deliveries:
        return {"message": "Нет данных"}

    total_price = sum(d['price'] for d in deliveries)
    avg_price = total_price / len(deliveries)

    companies = {}
    for d in deliveries:
        company = d['company']
        if company not in companies:
            companies[company] = {'count': 0, 'total_price': 0}
        companies[company]['count'] += 1
        companies[company]['total_price'] += d['price']

    cities = {}
    for d in deliveries:
        city = d['town_to']
        if city not in cities:
            cities[city] = {'count': 0}
        cities[city]['count'] += 1

    return {
        "total_deliveries": len(deliveries),
        "average_price": round(avg_price, 2),
        "total_value": round(total_price, 2),
        "by_company": {
            company: {
                "count": data['count'],
                "avg_price": round(data['total_price'] / data['count'], 2)
            }
            for company, data in companies.items()
        },
        "by_city": {
            city: data
            for city, data in cities.items()
        }
    }

if __name__ == "__main__":
    import uvicorn

    print("Запуск Delivery Aggregator API Server")
    print(f"База данных: deliveries.db")
    print(f"API доступен по адресу: http://localhost:8000")
    print(f"Документация: http://localhost:8000/api/docs")
    print(f"Для Android приложения используйте: http://10.0.2.2:8000")

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )