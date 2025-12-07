package com.vovlan.delivio

data class TariffsRequestDto(
    val fromCity: String,   // город отправления
    val city: String,       // город назначения (как было)
    val weight: Double,     // вес в кг
    val strategy: String    // стратегия сортировки
)