package com.vovlan.delivio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Кнопки
    private lateinit var showAllButton: Button
    private lateinit var newOrderButton: Button

    // Карточка последнего заказа (остров)
    private lateinit var lastOrderCard: LinearLayout
    private lateinit var lastOrderCompany: TextView
    private lateinit var lastOrderCargoType: TextView
    private lateinit var lastOrderTime: TextView
    private lateinit var lastOrderPrice: TextView
    private lateinit var lastOrderTariffType: TextView
    private lateinit var lastOrderAskButton: Button

    // НИЖНЯЯ ЧАСТЬ ОСТРОВА (кнопки при развороте)
    private lateinit var orderActionsContainer: LinearLayout
    private lateinit var completeOrderButton: Button
    private lateinit var openSiteButton: Button
    private lateinit var inOrdersButton: Button

    // Нижняя панель (три вкладки)
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabFeedback: LinearLayout

    // Флаг: развернут ли сейчас остров
    private var isOrderExpanded = false

    // Флаг: чтобы избежать двойного открытия экрана "Новый заказ"
    private var isNewOrderOpening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Кнопки
        showAllButton = findViewById(R.id.showAllButton)
        newOrderButton = findViewById(R.id.newOrderButton)

        // Остров последнего заказа
        lastOrderCard = findViewById(R.id.lastOrderCard)
        lastOrderCompany = findViewById(R.id.lastOrderCompany)
        lastOrderCargoType = findViewById(R.id.lastOrderCargoType)
        lastOrderTime = findViewById(R.id.lastOrderTime)
        lastOrderPrice = findViewById(R.id.lastOrderPrice)
        lastOrderTariffType = findViewById(R.id.lastOrderTariffType)
        lastOrderAskButton = findViewById(R.id.lastOrderAskButton)

        // Блок с дополнительными действиями
        orderActionsContainer = findViewById(R.id.orderActionsContainer)
        completeOrderButton = findViewById(R.id.completeOrderButton)
        openSiteButton = findViewById(R.id.openSiteButton)
        inOrdersButton = findViewById(R.id.inOrdersButton)

        // Нижняя панель
        tabHistory = findViewById(R.id.tabHistory)
        tabHome = findViewById(R.id.tabHome)
        tabFeedback = findViewById(R.id.tabFeedback)

        // Обработка аппаратной кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                noActivityTransition()
                finish()
            }
        })

        // Кнопка "Посмотреть все"
        showAllButton.setOnClickListener {
            startActivity(Intent(this, AllOrdersActivity::class.java))
        }

        // Кнопка "Новый заказ" с защитой от двойного открытия
        newOrderButton.setOnClickListener {
            if (isNewOrderOpening) {
                return@setOnClickListener
            }
            isNewOrderOpening = true
            startActivity(Intent(this, NewOrderActivity::class.java))
        }

        // Клик по карточке последнего заказа -> разворачиваем/сворачиваем
        lastOrderCard.setOnClickListener {
            if (DataRepository.orders.isEmpty()) {
                Toast.makeText(this, "Пока нет заказов", Toast.LENGTH_SHORT).show()
            } else {
                toggleOrderExpanded()
            }
        }

        // Кнопка "Задать вопрос"
        lastOrderAskButton.setOnClickListener {
            if (DataRepository.orders.isEmpty()) {
                Toast.makeText(this, "Пока нет заказов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val order = DataRepository.orders.first()
            val tariff = order.tariff
            val message = "Вопрос по заказу:\n" +
                    "${tariff.company}, ${tariff.tariffType}\n" +
                    "Тип груза: ${tariff.cargoType}\n" +
                    "Маршрут: Москва → ${order.city}, вес ${order.weightKg} кг\n" +
                    "Цена: ${tariff.price} ₽, срок: ${tariff.days} дн."

            val intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("prefill_message", message)
            startActivity(intent)
        }

        // Кнопка "Завершить заказ"
        completeOrderButton.setOnClickListener {
            if (DataRepository.orders.isEmpty()) {
                Toast.makeText(this, "Пока нет заказов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val order = DataRepository.orders.first()

            if (!order.isFinished) {
                order.isFinished = true
                Toast.makeText(this, "Заказ перенесён в историю", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Этот заказ уже завершён", Toast.LENGTH_SHORT).show()
            }

            // Сворачиваем карточку
            collapseOrderDetails()

            // Открываем экран истории
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Кнопка "Перейти на сайт"
        openSiteButton.setOnClickListener {
            if (DataRepository.orders.isEmpty()) {
                Toast.makeText(this, "Пока нет заказов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val order = DataRepository.orders.first()
            val companyName = order.tariff.company
            val query = Uri.encode(companyName)
            val url = "https://www.google.com/search?q=$query"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }

        // Кнопка "В заказах"
        inOrdersButton.setOnClickListener {
            startActivity(Intent(this, AllOrdersActivity::class.java))
        }

        // Нижняя панель навигации
        tabHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }

        tabHome.setOnClickListener {
            // уже на главном
        }

        tabFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
            noActivityTransition()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Разрешаем повторно открывать NewOrderActivity
        isNewOrderOpening = false

        // Загружаем заказы из локальной БД в память
        loadOrdersFromDb()

        // Обновляем UI
        updateLastOrder()
        collapseOrderDetails()
    }

    /**
     * Чтение заказов из SQLite в DataRepository.orders
     */
    private fun loadOrdersFromDb() {
        val dbHelper = OrdersDbHelper(this)
        val ordersFromDb = dbHelper.getAllOrders()

        DataRepository.orders.clear()
        DataRepository.orders.addAll(ordersFromDb)
    }

    private fun updateLastOrder() {
        if (DataRepository.orders.isEmpty()) {
            lastOrderCompany.text = "Пока нет заказов"
            lastOrderCargoType.text = ""
            lastOrderTime.text = ""
            lastOrderPrice.text = ""
            lastOrderTariffType.text = ""
            lastOrderAskButton.visibility = View.GONE
        } else {
            val order = DataRepository.orders.first()
            val tariff = order.tariff
            lastOrderCompany.text = tariff.company
            lastOrderCargoType.text = "Тип груза: ${tariff.cargoType}"
            lastOrderTime.text = "Время в пути: ${tariff.days} дн."
            lastOrderPrice.text = "Стоимость: ${tariff.price} ₽"
            lastOrderTariffType.text = tariff.tariffType
            lastOrderAskButton.visibility = View.VISIBLE
        }
    }

    private fun toggleOrderExpanded() {
        if (isOrderExpanded) {
            collapseOrderDetails()
        } else {
            orderActionsContainer.visibility = View.VISIBLE
            showAllButton.visibility = View.GONE
            isOrderExpanded = true
        }
    }

    private fun collapseOrderDetails() {
        orderActionsContainer.visibility = View.GONE
        showAllButton.visibility = View.VISIBLE
        isOrderExpanded = false
    }
}