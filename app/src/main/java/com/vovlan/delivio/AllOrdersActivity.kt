package com.vovlan.delivio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton

class AllOrdersActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton

    // нижняя панель
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabFeedback: LinearLayout

    // контейнер для списка заказов
    private lateinit var ordersContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_orders)

        backButton = findViewById(R.id.backButton)
        tabHistory = findViewById(R.id.tabHistory)
        tabHome = findViewById(R.id.tabHome)
        tabFeedback = findViewById(R.id.tabFeedback)
        ordersContainer = findViewById(R.id.ordersContainer)

        // Кнопка "назад" вверху
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        tabHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        tabFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        showOrders()
    }

    private fun showOrders() {
        ordersContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (DataRepository.orders.isEmpty()) {
            val textView = TextView(this)
            textView.text = "Пока нет заказов"
            ordersContainer.addView(textView)
            return
        }

        val list = DataRepository.orders

        for ((index, order) in list.withIndex()) {
            val itemView = inflater.inflate(R.layout.item_order_all, ordersContainer, false)

            val companyText: TextView = itemView.findViewById(R.id.orderCompanyText)
            val cargoTypeText: TextView = itemView.findViewById(R.id.orderCargoTypeText)
            val timeText: TextView = itemView.findViewById(R.id.orderTimeText)
            val priceText: TextView = itemView.findViewById(R.id.orderPriceText)
            val statusText: TextView = itemView.findViewById(R.id.orderStatusText)
            val tariffTypeText: TextView = itemView.findViewById(R.id.orderTariffTypeText)
            val askButton: Button = itemView.findViewById(R.id.askButton)

            // Нижняя часть острова (кнопки)
            val actionsContainer: LinearLayout =
                itemView.findViewById(R.id.orderActionsContainerAll)
            val completeOrderButton: Button =
                itemView.findViewById(R.id.completeOrderButtonAll)
            val openSiteButton: Button =
                itemView.findViewById(R.id.openSiteButtonAll)
            val toHistoryButton: Button =
                itemView.findViewById(R.id.toHistoryButtonAll)

            val tariff = order.tariff

            companyText.text = tariff.company
            cargoTypeText.text = "Тип груза: ${tariff.cargoType}"
            timeText.text = "Время в пути: ${tariff.days} дн."
            priceText.text = "Стоимость: ${tariff.price} ₽"
            statusText.text = if (order.isFinished) "Статус: завершён" else "Статус: активен"
            tariffTypeText.text = tariff.tariffType

            // Флаг "раскрыта ли карточка"
            var isExpanded = false

            // Клик по острову -> раскрыть/свернуть (как на главном, только без кнопки "Посмотреть все")
            itemView.setOnClickListener {
                isExpanded = !isExpanded
                actionsContainer.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE
            }

            // Кнопка "Задать вопрос"
            askButton.setOnClickListener {
                val message = "Вопрос по заказу:\n" +
                        "${tariff.company}, ${tariff.tariffType}\n" +
                        "Тип груза: ${tariff.cargoType}\n" +
                        "Маршрут: Москва → ${order.city}, вес ${order.weightKg} кг\n" +
                        "Цена: ${tariff.price} ₽, срок: ${tariff.days} дн.\n" +
                        "Статус: ${if (order.isFinished) "завершён" else "активен"}"

                val intent = Intent(this, FeedbackActivity::class.java)
                intent.putExtra("prefill_message", message)
                startActivity(intent)
            }

            // Кнопка "Завершить заказ" в карточке "Все заказы"
            completeOrderButton.setOnClickListener {
                if (!order.isFinished) {
                    order.isFinished = true
                    statusText.text = "Статус: завершён"
                    Toast.makeText(this, "Заказ перенесён в историю", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Этот заказ уже завершён", Toast.LENGTH_SHORT).show()
                }
            }

            // Кнопка "Перейти на сайт"
            openSiteButton.setOnClickListener {
                val companyName = order.tariff.company
                val query = Uri.encode(companyName)
                val url = "https://www.google.com/search?q=$query"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }

            // Кнопка "Перейти в историю"
            toHistoryButton.setOnClickListener {
                startActivity(Intent(this, HistoryActivity::class.java))
            }

            ordersContainer.addView(itemView)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }
}