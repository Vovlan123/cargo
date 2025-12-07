package com.vovlan.delivio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OrderDetailsActivity : AppCompatActivity() {

    private var orderIndex: Int = -1

    private lateinit var companyText: TextView
    private lateinit var routeText: TextView
    private lateinit var tariffText: TextView
    private lateinit var timeText: TextView
    private lateinit var priceText: TextView
    private lateinit var sourceText: TextView
    private lateinit var statusText: TextView
    private lateinit var sourceTypeText: TextView

    private lateinit var openSiteButton: Button
    private lateinit var finishOrderButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        orderIndex = intent.getIntExtra("orderIndex", -1)
        if (orderIndex == -1 || orderIndex >= DataRepository.orders.size) {
            Toast.makeText(this, "Нет данных заказа", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        companyText = findViewById(R.id.orderCompanyText)
        routeText = findViewById(R.id.orderRouteText)
        tariffText = findViewById(R.id.orderTariffText)
        timeText = findViewById(R.id.orderTimeText)
        priceText = findViewById(R.id.orderPriceText)
        sourceText = findViewById(R.id.orderSourceText)
        statusText = findViewById(R.id.orderStatusText)
        sourceTypeText = findViewById(R.id.orderSourceTypeText)

        openSiteButton = findViewById(R.id.openSiteButton)
        finishOrderButton = findViewById(R.id.finishOrderButton)

        fillData()

        // Перейти на сайт компании
        openSiteButton.setOnClickListener {
            val order = DataRepository.orders[orderIndex]
            val uri = Uri.parse(order.tariff.sourceUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        // Отметить заказ завершённым
        finishOrderButton.setOnClickListener {
            val order = DataRepository.orders[orderIndex]
            order.isFinished = true
            fillData()
            Toast.makeText(this, "Заказ помечен как завершён", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fillData() {
        val order = DataRepository.orders[orderIndex]
        val tariff = order.tariff

        companyText.text = tariff.company
        routeText.text = "Москва → ${order.city}, вес ${order.weightKg} кг"
        tariffText.text = "Тип тарифа: ${tariff.tariffType}"
        timeText.text = "Время в пути: ${tariff.days} дн."
        priceText.text = "Стоимость: ${tariff.price} ₽"

        // Источник данных: прогноз или от перевозчика
        val isRestored = tariff.isPriceRestored || tariff.isTimeRestored
        if (isRestored) {
            sourceTypeText.text = "Прогноз"
            sourceText.text = "Стоимость/срок спрогнозированы моделью"
        } else {
            sourceTypeText.text = "От перевозчика"
            sourceText.text = "Это цена, указанная на сайте компании"
        }

        statusText.text =
            if (order.isFinished) "Статус: завершён" else "Статус: активен"
    }
}