package com.vovlan.delivio

import android.content.Intent
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
    private lateinit var selectTariffButton: Button
    private lateinit var askButton: Button

    private var city: String = ""
    private var weightKg: Double = 0.0
    private lateinit var tariff: Tariff

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tariff_details)

        // Инициализация вьюх (подгони ID под свою разметку activity_tariff_details.xml)
        companyText = findViewById(R.id.companyText)
        cargoTypeText = findViewById(R.id.cargoTypeText)
        tariffTypeText = findViewById(R.id.tariffTypeText)
        timeText = findViewById(R.id.timeText)
        priceText = findViewById(R.id.priceText)
        selectTariffButton = findViewById(R.id.selectTariffButton)
        askButton = findViewById(R.id.askButton)

        // Читаем данные из Intent
        city = intent.getStringExtra("city") ?: ""
        weightKg = intent.getDoubleExtra("weightKg", 0.0)

        // Tariff передаётся как Serializable
        val serializable = intent.getSerializableExtra("tariff")
        if (serializable == null || serializable !is Tariff) {
            Toast.makeText(this, "Не удалось загрузить тариф", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        tariff = serializable

        // Заполняем данные
        companyText.text = tariff.company
        cargoTypeText.text = "Тип груза: ${tariff.cargoType}"
        tariffTypeText.text = tariff.tariffType
        timeText.text = "Время в пути: ${tariff.days} дн."
        priceText.text = "Стоимость: ${tariff.price} ₽"

        // Кнопка "Выбрать тариф" — создаём заказ и возвращаемся на главную
        selectTariffButton.setOnClickListener {
            if (city.isBlank() || weightKg <= 0.0) {
                Toast.makeText(this, "Некорректные данные заказа", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Новый заказ всегда не завершён
            val order = Order(
                city = city,
                weightKg = weightKg,
                tariff = tariff,
                isFinished = false
            )

            // Добавляем в память
            DataRepository.orders.add(0, order)

            Toast.makeText(this, "Тариф добавлен в заказы", Toast.LENGTH_SHORT).show()

            // Переходим на главный экран
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Кнопка "Задать вопрос"
        askButton.setOnClickListener {
            val message = "Вопрос по тарифу:\n" +
                    "${tariff.company}, ${tariff.tariffType}\n" +
                    "Тип груза: ${tariff.cargoType}\n" +
                    "Маршрут: Москва → $city, вес $weightKg кг\n" +
                    "Цена: ${tariff.price} ₽, срок: ${tariff.days} дн."

            val intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("prefill_message", message)
            startActivity(intent)
        }
    }
}