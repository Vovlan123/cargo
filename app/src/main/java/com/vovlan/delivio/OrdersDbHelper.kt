package com.vovlan.delivio

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OrdersDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION
) {

    companion object {
        private const val DB_NAME = "orders.db"
        private const val DB_VERSION = 1

        const val TABLE_NAME = "deliveries"

        const val COL_ID = "id"
        const val COL_COMPANY = "company"
        const val COL_DELIVERY_TYPE = "delivery_type"
        const val COL_WEIGHT = "weight"
        const val COL_SIZE = "size"
        const val COL_TOWN_FROM = "town_from"
        const val COL_TOWN_TO = "town_to"
        const val COL_PRICE = "price"
        const val COL_DELIVERY_TIME = "delivery_time"
        const val COL_IS_COMPLETED = "is_completed"
        const val COL_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_COMPANY TEXT NOT NULL,
                $COL_DELIVERY_TYPE TEXT NOT NULL,
                $COL_WEIGHT REAL NOT NULL,
                $COL_SIZE TEXT NOT NULL,
                $COL_TOWN_FROM TEXT NOT NULL,
                $COL_TOWN_TO TEXT NOT NULL,
                $COL_PRICE INTEGER NOT NULL,
                $COL_DELIVERY_TIME INTEGER NOT NULL,
                $COL_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
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
            put(COL_COMPANY, companyName)
            put(COL_DELIVERY_TYPE, deliveryType)
            put(COL_WEIGHT, weightKg)
            put(COL_SIZE, size)
            put(COL_TOWN_FROM, townFrom)
            put(COL_TOWN_TO, townTo)
            put(COL_PRICE, priceRub)
            put(COL_DELIVERY_TIME, timeDays)
            put(COL_IS_COMPLETED, 0)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllOrders(): List<Order> {
        val result = mutableListOf<Order>()
        val db = readableDatabase

        val columns = arrayOf(
            COL_COMPANY,
            COL_DELIVERY_TYPE,
            COL_WEIGHT,
            COL_TOWN_TO,
            COL_PRICE,
            COL_DELIVERY_TIME,
            COL_IS_COMPLETED
        )

        val cursor = db.query(
            TABLE_NAME,
            columns,
            null,
            null,
            null,
            null,
            "$COL_CREATED_AT DESC"
        )

        cursor.use {
            val idxCompany = it.getColumnIndexOrThrow(COL_COMPANY)
            val idxDeliveryType = it.getColumnIndexOrThrow(COL_DELIVERY_TYPE)
            val idxWeight = it.getColumnIndexOrThrow(COL_WEIGHT)
            val idxTownTo = it.getColumnIndexOrThrow(COL_TOWN_TO)
            val idxPrice = it.getColumnIndexOrThrow(COL_PRICE)
            val idxDeliveryTime = it.getColumnIndexOrThrow(COL_DELIVERY_TIME)
            val idxIsCompleted = it.getColumnIndexOrThrow(COL_IS_COMPLETED)

            while (it.moveToNext()) {
                val company = it.getString(idxCompany)
                val deliveryType = it.getString(idxDeliveryType)
                val weightKg = it.getDouble(idxWeight)
                val townTo = it.getString(idxTownTo)
                val price = it.getInt(idxPrice).toDouble()
                val days = it.getInt(idxDeliveryTime)
                val isCompleted = it.getInt(idxIsCompleted) == 1

                val tariff = Tariff(
                    company = company,
                    cargoType = "",
                    tariffType = deliveryType,
                    price = price,
                    days = days,
                    isPriceRestored = false,
                    isTimeRestored = false,
                    sourceUrl = ""   // не null, а пустая строка
                )

                val order = Order(
                    city = townTo,
                    weightKg = weightKg,
                    tariff = tariff,
                    isFinished = isCompleted
                )

                result.add(order)
            }
        }

        return result
    }
}