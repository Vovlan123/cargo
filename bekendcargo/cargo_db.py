import sqlite3
from datetime import datetime
from typing import List, Dict, Any

class DeliveryDB:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.init_db()
        print("✅ База данных готова:", db_path)

    def init_db(self):
        """Создать таблицу если её нет"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS deliveries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                company TEXT NOT NULL,
                delivery_type TEXT NOT NULL,
                weight REAL NOT NULL,
                size TEXT NOT NULL,
                town_from TEXT NOT NULL,
                town_to TEXT NOT NULL,
                price INTEGER NOT NULL,
                days INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        conn.commit()
        conn.close()

    def save_delivery(self, company: str, delivery_type: str, weight: float,
                     size: str, town_from: str, town_to: str, price: int, days: int) -> int:
        """Сохранить доставку, вернуть ID"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO deliveries (company, delivery_type, weight, size, town_from, town_to, price, days)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', (company, delivery_type, weight, size, town_from, town_to, price, days))
        delivery_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return delivery_id

    def get_all_deliveries(self) -> List[Dict[str, Any]]:
        """Получить все доставки"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM deliveries ORDER BY id DESC')
        rows = cursor.fetchall()
        columns = [description[0] for description in cursor.description]
        conn.close()
        return [dict(zip(columns, row)) for row in rows]
