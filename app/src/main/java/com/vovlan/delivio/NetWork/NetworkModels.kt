package com.vovlan.delivio

import com.google.gson.annotations.SerializedName

data class TariffDto(
    @SerializedName("company") val company: String,
    @SerializedName("cargo_type") val cargoType: String,
    @SerializedName("tariff_type") val tariffType: String,
    @SerializedName("price") val price: Double,
    @SerializedName("days") val days: Int,
    @SerializedName("is_price_restored") val isPriceRestored: Boolean,
    @SerializedName("is_time_restored") val isTimeRestored: Boolean,
    @SerializedName("source_url") val sourceUrl: String
)

data class TariffsResponseDto(
    @SerializedName("city") val city: String,
    @SerializedName("weight_kg") val weightKg: Double,
    @SerializedName("strategy") val strategy: String,
    @SerializedName("avg_price") val avgPrice: Double,
    @SerializedName("avg_days") val avgDays: Double,
    @SerializedName("tariffs") val tariffs: List<TariffDto>
)

data class TariffsRequestDto(
    @SerializedName("city") val city: String,
    @SerializedName("weight") val weight: Double,
    @SerializedName("strategy") val strategy: String = "none"
)