package com.vovlan.delivio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AllOrdersActivity : AppCompatActivity() {

    // нижняя панель
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabFeedback: LinearLayout

    // контейнер для заказов
    private lateinit var ordersContainer: LinearLayout

    // стрелка "назад"
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_orders)

        tabHistory = findViewById(R.id.tabHistory)
        tabHome = findViewById(R.id.tabHome)
        tabFeedback = findViewById(R.id.tabFeedback)

        ordersContainer = findViewById(R.id.ordersContainer)
        backButton = findViewById(R.id.backButton)

        // навигация по нижней панели
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

        // стрелка "назад"
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        // подгружаем заказы из БД (актуальный статус isFinished)
        loadOrdersFromDb()
        showActiveOrders()
    }

    private fun loadOrdersFromDb() {
        val dbHelper = OrdersDbHelper(this)
        val ordersFromDb = dbHelper.getAllOrders()
        DataRepository.orders.clear()
        DataRepository.orders.addAll(ordersFromDb)
    }

    private fun showActiveOrders() {
        ordersContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        // Показываем только НЕЗАВЕРШЁННЫЕ заказы
        val activeOrders = DataRepository.orders.filter { !it.isFinished }

        if (activeOrders.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Пока нет активных заказов"
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#676767"))
                gravity = android.view.Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (32 * resources.displayMetrics.density).toInt()
                }
            }
            ordersContainer.addView(tv)
            return
        }

        for (order in activeOrders) {
            val itemView = inflater.inflate(R.layout.item_order, ordersContainer, false)

            val companyText: TextView = itemView.findViewById(R.id.orderCompanyText)
            val routeText: TextView = itemView.findViewById(R.id.orderRouteText)
            val timeText: TextView = itemView.findViewById(R.id.orderTimeText)
            val priceText: TextView = itemView.findViewById(R.id.orderPriceText)
            val tariffTypeText: TextView = itemView.findViewById(R.id.orderTariffTypeText)
            val askButton: Button = itemView.findViewById(R.id.askButton)

            val actionsContainer: LinearLayout =
                itemView.findViewById(R.id.orderActionsContainerHistory)
            val openSiteButton: Button =
                itemView.findViewById(R.id.openSiteButtonHistory)
            val inOrdersButton: Button =
                itemView.findViewById(R.id.inOrdersButtonHistory)

            // НОВАЯ кнопка "Завершить заказ" (добавь её в layout item_order.xml)
            val completeButton: Button =
                itemView.findViewById(R.id.completeOrderButton)

            val tariff = order.tariff

            companyText.text = tariff.company
            routeText.text = "Москва → ${order.city}, вес ${order.weightKg} кг"
            timeText.text = "Время в пути: ${tariff.days} дн."
            priceText.text = "Стоимость: ${tariff.price} ₽"
            tariffTypeText.text = tariff.tariffType

            var isExpanded = false
            itemView.setOnClickListener {
                isExpanded = !isExpanded
                actionsContainer.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE
            }

            askButton.setOnClickListener {
                val message = "Вопрос по заказу:\n" +
                        "${tariff.company}, ${tariff.tariffType}\n" +
                        "Тип груза: ${tariff.cargoType}\n" +
                        "Маршрут: Москва → ${order.city}, вес ${order.weightKg} кг\n" +
                        "Цена: ${tariff.price} ₽, срок: ${tariff.days} дн."

                val intent = Intent(this, FeedbackActivity::class.java)
                intent.putExtra("prefill_message", message)
                startActivity(intent)
            }

            openSiteButton.setOnClickListener {
                val companyName = order.tariff.company
                val query = Uri.encode(companyName)
                val url = "https://www.google.com/search?q=$query"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }

            // Здесь можно сделать, например, переход на этот же экран, так что ничего
            inOrdersButton.setOnClickListener {
                // уже на экране "Все заказы"
            }

            // НОВОЕ: "Завершить заказ" из списка всех заказов
            completeButton.setOnClickListener {
                // 1. Помечаем в памяти
                order.isFinished = true

                // 2. Обновляем в БД
                try {
                    val dbHelper = OrdersDbHelper(this)
                    dbHelper.markOrderCompleted(order)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Ошибка при обновлении заказа в БД: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 3. Обновляем список (заказ исчезнет из "Все заказы")
                showActiveOrders()

                // 4. Можно показать тост
                Toast.makeText(
                    this,
                    "Заказ перенесён в историю",
                    Toast.LENGTH_SHORT
                ).show()
            }

            ordersContainer.addView(itemView)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }
}