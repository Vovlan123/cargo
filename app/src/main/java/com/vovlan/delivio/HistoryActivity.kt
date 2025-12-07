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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    // нижняя панель
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabFeedback: LinearLayout

    // элементы экрана
    private lateinit var filterButton: ImageButton
    private lateinit var ordersContainer: LinearLayout

    private var currentFilter: FilterStrategy = FilterStrategy.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        tabHistory = findViewById(R.id.tabHistory)
        tabHome = findViewById(R.id.tabHome)
        tabFeedback = findViewById(R.id.tabFeedback)

        filterButton = findViewById(R.id.filterButton)
        ordersContainer = findViewById(R.id.ordersContainer)

        // навигация
        tabHistory.setOnClickListener {
            // уже на History
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

        // фильтр
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        updateFilterButtonText()
    }

    override fun onResume() {
        super.onResume()
        // Загружаем заказы из БД, чтобы статус завершённости был актуальным
        loadOrdersFromDb()
        showOrders()
    }

    private fun loadOrdersFromDb() {
        val dbHelper = OrdersDbHelper(this)
        val ordersFromDb = dbHelper.getAllOrders()
        DataRepository.orders.clear()
        DataRepository.orders.addAll(ordersFromDb)
    }

    private fun showFilterDialog() {
        val options = arrayOf(
            "Без фильтра",
            "Самая дешёвая",
            "Самая быстрая",
            "Сбалансированная",
            "Тип тарифа 1",
            "Тип тарифа 2",
            "Тип тарифа 3"
        )

        AlertDialog.Builder(this)
            .setTitle("Фильтр истории заказов")
            .setItems(options) { _, which ->
                currentFilter = when (which) {
                    0 -> FilterStrategy.NONE
                    1 -> FilterStrategy.CHEAPEST
                    2 -> FilterStrategy.FASTEST
                    3 -> FilterStrategy.BALANCED
                    4 -> FilterStrategy.TYPE1
                    5 -> FilterStrategy.TYPE2
                    6 -> FilterStrategy.TYPE3
                    else -> FilterStrategy.NONE
                }
                updateFilterButtonText()
                showOrders()
            }
            .show()
    }

    // Меняем только описание для доступности
    private fun updateFilterButtonText() {
        val description = when (currentFilter) {
            FilterStrategy.NONE -> "Фильтр"
            FilterStrategy.CHEAPEST -> "Дешёвые"
            FilterStrategy.FASTEST -> "Быстрые"
            FilterStrategy.BALANCED -> "Сбаланс."
            FilterStrategy.TYPE1 -> "Тип 1"
            FilterStrategy.TYPE2 -> "Тип 2"
            FilterStrategy.TYPE3 -> "Тип 3"
        }
        filterButton.contentDescription = description
    }

    private fun getOrdersForCurrentFilter(): List<Order> {
        val finished = DataRepository.orders
            .filter { it.isFinished }

        return when (currentFilter) {
            FilterStrategy.NONE -> finished

            FilterStrategy.CHEAPEST ->
                finished.sortedBy { it.tariff.price }

            FilterStrategy.FASTEST ->
                finished.sortedBy { it.tariff.days }

            FilterStrategy.BALANCED -> {
                if (finished.isEmpty()) return finished
                val maxPrice = finished.maxOf { it.tariff.price }
                val maxDays = finished.maxOf { it.tariff.days }
                finished.sortedBy {
                    (it.tariff.price / maxPrice) +
                            (it.tariff.days.toDouble() / maxDays)
                }
            }

            FilterStrategy.TYPE1 ->
                finished.filter { it.tariff.tariffType == "Тип тарифа 1" }

            FilterStrategy.TYPE2 ->
                finished.filter { it.tariff.tariffType == "Тип тарифа 2" }

            FilterStrategy.TYPE3 ->
                finished.filter { it.tariff.tariffType == "Тип тарифа 3" }
        }
    }

    private fun showOrders() {
        ordersContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val anyFinished = DataRepository.orders.any { it.isFinished }
        if (!anyFinished) {
            val tv = TextView(this).apply {
                text = "Пока нет завершённых заказов"
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

        val list = getOrdersForCurrentFilter()

        if (list.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Нет завершённых заказов под выбранный фильтр"
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

        for (order in list) {
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

            inOrdersButton.setOnClickListener {
                startActivity(Intent(this, AllOrdersActivity::class.java))
            }

            ordersContainer.addView(itemView)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }
}