from fastapi import FastAPI, Form, Request
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from typing import Literal
import random

app = FastAPI(title="Cаrgo — Хакатон ВШЭ 2025")
app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

BoxSize = Literal["XS", "S", "M", "L", "XL", "XXL", "XXXL", "XXXXL"]

BOX_INFO = {
    "XS":    {"name": "Документы",           "hint": "до 1 кг",    "max": 1},
    "S":     {"name": "Маленькая посылка",   "hint": "1–2 кг",       "max": 2},
    "M":     {"name": "Средняя коробка",     "hint": "3–5 кг",       "max": 5},
    "L":     {"name": "Стандартная коробка", "hint": "6–10 кг",      "max": 10},
    "XL":    {"name": "Большая коробка",     "hint": "11–15 кг",     "max": 15},
    "XXL":   {"name": "Очень большая",       "hint": "16–25 кг",     "max": 25},
    "XXXL":  {"name": "Грузовое место",      "hint": "26–40 кг",     "max": 40},
    "XXXXL": {"name": "Паллета",             "hint": "41–500 кг",    "max": 500},
}

CITIES = {
    "москва":1,"санкт-петербург":2,"калининград":2,"нижний новгород":3,"казань":3,"самара":3,"волгоград":3,
    "ростов-на-дону":4,"краснодар":4,"воронеж":4,"сочи":4,"ставрополь":4,"екатеринбург":5,"челябинск":5,
    "тюмень":5,"пермь":5,"уфа":5,"новосибирск":6,"омск":6,"томск":6,"барнаул":6,"кемерово":6,
    "красноярск":7,"иркутск":7,"хабаровск":8,"владивосток":8,"якутск":9,"благовещенск":9,
    "петропавловск-камчатский":10,"магадан":10,"южно-сахалинск":10,"саратов":3,"тольятти":3,"ижевск":5,
    "ульяновск":3,"оренбург":5,"новокузнецк":6,"рязань":2,"пенза":3,"липецк":2,"тула":2,"астрахань":4,
    "киров":3,"чебоксары":3,"калуга":2,"курск":2,"тверь":2,"севастополь":4,"брянск":2,"иваново":2,
    "магнитогорск":5,"белгород":2
}

def calculate_volume_weight(l, w, h): return round((l * w * h) / 5000, 2)

def get_zone_diff(c1, c2):
    z1 = CITIES.get(c1.lower().replace("ё", "е"))
    z2 = CITIES.get(c2.lower().replace("ё", "е"))
    if not z1 or not z2:
        raise ValueError("Город не найден")
    return abs(z1 - z2) + 1

def calculate_price(company, tariff, weight, vol_weight, zone_diff, box):
    charge = max(weight, vol_weight)
    extra = {"XXL": 1800, "XXXL": 4000, "XXXXL": 9000}.get(box, 0)
    if company == "СДЭК":
        base = 550 if "эконом" in tariff.lower() else 950
        per_kg = (130 + zone_diff * 38) if "эконом" in tariff.lower() else (380 + zone_diff * 55)
    elif company == "Boxberry":
        base, per_kg = 680, 160 + zone_diff * 42
    elif company == "Почта России":
        base, per_kg = (1400, 500 + zone_diff * 70) if tariff == "EMS" else (450, 110 + zone_diff * 25)
    else:
        base, per_kg = 700, 200 + zone_diff * 40
    price = base + (charge - 1) * per_kg + extra
    price = int(price * random.uniform(0.92, 1.08))
    return max(290, round(price / 10) * 10)

@app.get("/")
async def home(request: Request):
    return templates.TemplateResponse("index.html", {
        "request": request,
        "cities": sorted(CITIES.keys()),
        "boxes": BOX_INFO
    })

@app.post("/calc")
async def calc(request: Request, from_city: str = Form(...), to_city: str = Form(...), weight: int = Form(...), box_size: BoxSize = Form(...)):
    error = None
    offers = None

    if from_city.lower() == to_city.lower():
        error = "Города отправления и назначения не могут совпадать!"
    elif weight > BOX_INFO[box_size]["max"]:
        error = f"Вес {weight} кг превышает лимит коробки {box_size}!"
    else:
        try:
            zone_diff = get_zone_diff(from_city, to_city)
            vol_weight = calculate_volume_weight(80, 60, 50) if box_size in ["XXL", "XXXL", "XXXXL"] else calculate_volume_weight(60, 40, 40)
            offers = [
                ("СДЭК", "Экспресс лайт", calculate_price("СДЭК", "экспресс", weight, vol_weight, zone_diff, box_size), f"{max(1, zone_diff//2)}–{zone_diff+1} дн."),
                ("СДЭК", "Посылочка Эконом", calculate_price("СДЭК", "эконом", weight, vol_weight, zone_diff, box_size), f"{zone_diff+1}–{zone_diff+5} дн."),
                ("Boxberry", "Стандарт", calculate_price("Boxberry", "", weight, vol_weight, zone_diff, box_size), f"{zone_diff}–{zone_diff+4} дн."),
                ("Почта России", "Обычная посылка", calculate_price("Почта России", "обычная", weight, vol_weight, zone_diff, box_size), f"{zone_diff+3}–{zone_diff+9} дн."),
                ("Почта России", "EMS", calculate_price("Почта России", "EMS", weight, vol_weight, zone_diff, box_size), f"{max(1, zone_diff//3)}–{zone_diff//2+2} дн."),
            ]
            offers.sort(key=lambda x: x[2])
        except Exception as e:
            error = str(e)

    return templates.TemplateResponse("index.html", {
        "request": request,
        "cities": sorted(CITIES.keys()),
        "boxes": BOX_INFO,
        "from_city": from_city,
        "to_city": to_city,
        "weight": weight,
        "box_size": box_size,
        "error": error,
        "offers": offers
    })

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)