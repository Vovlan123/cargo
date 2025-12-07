package com.vovlan.delivio

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class OrdersDbHelper(context: Context) :
    SQLiteOpenHelper(context, "zakazi.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS orders (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                company_name  TEXT    NOT NULL,
                delivery_type TEXT    NOT NULL CHECK (
                    delivery_type IN ('экспресс лайт', 'посылочка (Эконом)', 'EMS отправление')
                ),
                weight_kg     REAL    NOT NULL CHECK (weight_kg > 0),
                size          TEXT    NOT NULL,
                town_from     TEXT    NOT NULL,
                town_to       TEXT    NOT NULL,
                price_rub     INTEGER NOT NULL CHECK (price_rub >= 0),
                time_days     INTEGER NOT NULL CHECK (time_days > 0),
                created_at    INTEGER NOT NULL
            );
            """.trimIndent()
        )
        // Размер не проверяется триггером — маппим из кода.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // На будущее: миграции
    }

    fun insertOrder(
        companyName: String,
        deliveryType: String,
        weightKg: Double,
        size: String,
        townFrom: String,
        townTo: String,
        priceRub: Int,
        timeDays: Int
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("company_name", companyName)
            put("delivery_type", deliveryType)
            put("weight_kg", weightKg)
            put("size", size)
            put("town_from", "г. $townFrom")
            put("town_to", "г. $townTo")
            put("price_rub", priceRub)
            put("time_days", timeDays)
            put("created_at", System.currentTimeMillis())
        }
        return try {
            db.insertOrThrow("orders", null, values)
        } catch (e: Exception) {
            Log.e("OrdersDbHelper", "Ошибка вставки заказа", e)
            -1L
        }
    }
}