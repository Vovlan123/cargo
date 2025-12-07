package com.vovlan.delivio

import java.io.Serializable

// Один тариф доставки. Serializable — чтобы передавать через Intent.
data class Tariff(
    val company: String,
    val cargoType: String,
    val tariffType: String,
    val price: Double,
    val days: Int,
    val isPriceRestored: Boolean,
    val isTimeRestored: Boolean,
    val sourceUrl: String
) : Serializable

// Заказ = выбранный тариф + город + вес
data class Order(
    val city: String,
    val weightKg: Double,
    val tariff: Tariff,
    var isFinished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// Простое хранилище заказов в памяти приложения
object DataRepository {
    val orders = mutableListOf<Order>()
}

// Стратегии сортировки / фильтрации
enum class FilterStrategy {
    NONE,        // без фильтра
    CHEAPEST,    // самая дешёвая
    FASTEST,     // самая быстрая
    BALANCED,    // баланс цена/срок
    TYPE1,       // тип тарифа 1
    TYPE2,       // тип тарифа 2
    TYPE3        // тип тарифа 3
}