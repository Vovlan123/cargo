package com.vovlan.delivio

import java.io.Serializable
// Модель тарифа (как приходит с бэка и хранится в заказе)
data class Tariff(
    val company: String,
    val cargoType: String,
    val tariffType: String,
    val price: Double,
    val days: Int,
    val isPriceRestored: Boolean,
    val isTimeRestored: Boolean,
    val sourceUrl: String
) : Serializable   // ВАЖНО: теперь Tariff реализует Serializable

// Модель заказа внутри приложения
data class Order(
    val city: String,
    val weightKg: Double,
    val tariff: Tariff,
    var isFinished: Boolean
)

// Стратегии сортировки/фильтрации (как у тебя уже использовалось)
enum class FilterStrategy {
    NONE,
    CHEAPEST,
    FASTEST,
    BALANCED,
    TYPE1,
    TYPE2,
    TYPE3
}

// Общий репозиторий заказов в памяти
object DataRepository {
    val orders: MutableList<Order> = mutableListOf()
}