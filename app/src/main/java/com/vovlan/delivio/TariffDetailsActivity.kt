package com.vovlan.delivio

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TariffDetailsActivity : AppCompatActivity() {

    private lateinit var companyText: TextView
    private lateinit var cargoTypeText: TextView
    private lateinit var tariffTypeText: TextView
    private lateinit var timeText: TextView
    private lateinit var priceText: TextView
    private lateinit var sourceText: TextView
    private lateinit var sourceTypeText: TextView

    private lateinit var openSiteButton: Button
    private lateinit var addToOrdersButton: Button

    private lateinit var tariff: Tariff
    private var city: String = ""
    private var weightKg: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tariff_details)

        // Город и вес из Intent
        city = intent.getStringExtra("city") ?: ""
        weightKg = intent.getDoubleExtra("weightKg", 0.0)

        // ТАРИФ: новый способ getSerializableExtra без депрекейта
        val tariffExtra: Tariff? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("tariff", Tariff::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("tariff") as? Tariff
        }

        if (tariffExtra == null) {
            Toast.makeText(this, "Нет данных тарифа", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        tariff = tariffExtra

        // Находим view
        companyText = findViewById(R.id.companyText)
        cargoTypeText = findViewById(R.id.cargoTypeText)
        tariffTypeText = findViewById(R.id.tariffTypeText)
        timeText = findViewById(R.id.timeText)
        priceText = findViewById(R.id.priceText)
        sourceText = findViewById(R.id.sourceText)
        sourceTypeText = findViewById(R.id.sourceTypeText)

        openSiteButton = findViewById(R.id.openSiteButton)
        addToOrdersButton = findViewById(R.id.addToOrdersButton)

        // Заполняем данными
        companyText.text = tariff.company
        cargoTypeText.text = "Тип груза: ${tariff.cargoType}"
        tariffTypeText.text = "Тип тарифа: ${tariff.tariffType}"
        timeText.text = "Время в пути: ${tariff.days} дн."
        priceText.text = "Стоимость: ${tariff.price} ₽"

        val isRestored = tariff.isPriceRestored || tariff.isTimeRestored
        if (isRestored) {
            sourceTypeText.text = "Прогноз"
            sourceText.text = "Стоимость/срок спрогнозированы моделью"
        } else {
            sourceTypeText.text = "От перевозчика"
            sourceText.text = "Это цена, указанная на сайте компании"
        }

        // Кнопка "Перейти на сайт"
        openSiteButton.setOnClickListener {
            val uri = Uri.parse(tariff.sourceUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // Кнопка "Добавить в заказы"
        addToOrdersButton.setOnClickListener {
            val order = Order(
                city = city,
                weightKg = weightKg,
                tariff = tariff
            )
            DataRepository.orders.add(0, order)
            Toast.makeText(this, "Тариф добавлен в заказы", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}