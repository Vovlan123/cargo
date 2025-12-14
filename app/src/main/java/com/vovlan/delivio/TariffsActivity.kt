package com.vovlan.delivio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class TariffsActivity : AppCompatActivity() {

    private val BASE_URL = "http://127.0.0.1:8000"
    // Если сервер на ПК, а запускаешь с эмулятора:
    // private val BASE_URL = "http://10.0.2.2:8000"

    private val httpClient = OkHttpClient()
    private val gson = Gson()

    // Layout-состояния
    private lateinit var loadingLayout: View
    private lateinit var contentLayout: View
    private lateinit var emptyLayout: View

    // Элементы контента (основные)
    private lateinit var cityText: TextView
    private lateinit var weightText: TextView
    private lateinit var avgPriceText: TextView
    private lateinit var avgTimeText: TextView
    private lateinit var strategyText: TextView
    private lateinit var tariffsContainer: LinearLayout
    private lateinit var changeDataButton: Button
    private lateinit var filterButton: Button

    // Элементы emptyLayout (дубликаты)
    private lateinit var emptyCityText: TextView
    private lateinit var emptyWeightText: TextView
    private lateinit var emptyFilterButton: Button
    private lateinit var emptyChangeDataButton: Button

    // Нижняя панель (вкладки) - основные
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabFeedback: LinearLayout

    // Нижняя панель (вкладки) - emptyLayout
    private lateinit var emptyTabHistory: LinearLayout
    private lateinit var emptyTabHome: LinearLayout
    private lateinit var emptyTabFeedback: LinearLayout

    // Параметры запроса
    private var fromCity: String = ""
    private var city: String = ""
    private var weightKg: Double = 0.0

    // Тарифы с сервера
    private var originalTariffs: List<Tariff> = emptyList()
    private var currentFilter: FilterStrategy = FilterStrategy.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tariffs)

        // Читаем параметры из Intent
        fromCity = intent.getStringExtra("fromCity") ?: "Москва"
        city = intent.getStringExtra("city") ?: ""
        weightKg = intent.getDoubleExtra("weightKg", 0.0)

        // Инициализация всех View
        initViews()

        // Обновляем данные во всех состояниях
        updateAllDataDisplays()

        // Настраиваем все кнопки
        setupAllButtons()

        showLoading()
        loadTariffsFromServer()
    }

    private fun initViews() {
        // Основные layouts
        loadingLayout = findViewById(R.id.loadingLayout)
        contentLayout = findViewById(R.id.contentLayout)
        emptyLayout = findViewById(R.id.emptyLayout)

        // Основной контент
        cityText = findViewById(R.id.cityText)
        weightText = findViewById(R.id.weightText)
        avgPriceText = findViewById(R.id.avgPriceText)
        avgTimeText = findViewById(R.id.avgTimeText)
        strategyText = findViewById(R.id.strategyText)
        tariffsContainer = findViewById(R.id.tariffsContainer)
        changeDataButton = findViewById(R.id.changeDataButton)
        filterButton = findViewById(R.id.filterButton)

        // Empty layout контент
        emptyCityText = findViewById(R.id.emptyCityText)
        emptyWeightText = findViewById(R.id.emptyWeightText)
        emptyFilterButton = findViewById(R.id.emptyFilterButton)
        emptyChangeDataButton = findViewById(R.id.emptyChangeDataButton)

        // Основная нижняя панель
        tabHistory = findViewById(R.id.tabHistory)
        tabHome = findViewById(R.id.tabHome)
        tabFeedback = findViewById(R.id.tabFeedback)

        // Нижняя панель в emptyLayout
        emptyTabHistory = findViewById(R.id.emptyTabHistory)
        emptyTabHome = findViewById(R.id.emptyTabHome)
        emptyTabFeedback = findViewById(R.id.emptyTabFeedback)
    }

    private fun updateAllDataDisplays() {
        // Обновляем город/вес во всех местах
        val cityDisplay = "$fromCity → $city"
        val weightDisplay = "Вес: $weightKg кг"

        cityText.text = cityDisplay
        weightText.text = weightDisplay
        emptyCityText.text = cityDisplay
        emptyWeightText.text = weightDisplay
    }

    private fun setupAllButtons() {
        // Кнопка "Изменить данные" - везде одинаково
        changeDataButton.setOnClickListener { goToNewOrder() }
        emptyChangeDataButton.setOnClickListener { goToNewOrder() }

        // Фильтр - везде одинаково
        filterButton.setOnClickListener { showFilterDialog() }
        emptyFilterButton.setOnClickListener { showFilterDialog() }

        // Навигация - везде одинаково
        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        // History
        tabHistory.setOnClickListener { goToHistory() }
        emptyTabHistory.setOnClickListener { goToHistory() }

        // Home
        tabHome.setOnClickListener { goToHome() }
        emptyTabHome.setOnClickListener { goToHome() }

        // Feedback
        tabFeedback.setOnClickListener { goToFeedback() }
        emptyTabFeedback.setOnClickListener { goToFeedback() }
    }

    private fun goToNewOrder() {
        val intent = Intent(this, NewOrderActivity::class.java)
        intent.putExtra("fromCity", fromCity)
        intent.putExtra("city", city)
        intent.putExtra("weightKg", weightKg)
        startActivity(intent)
    }

    private fun goToHistory() {
        startActivity(Intent(this, HistoryActivity::class.java))
        finish()
    }

    private fun goToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun goToFeedback() {
        startActivity(Intent(this, FeedbackActivity::class.java))
        finish()
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
            .setTitle("Сортировка тарифов")
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
                applyFilter()
            }
            .show()
    }

    private fun updateStrategyText() {
        val text = when (currentFilter) {
            FilterStrategy.NONE -> "Фильтр"
            FilterStrategy.CHEAPEST -> "Самая дешёвая"
            FilterStrategy.FASTEST -> "Самая быстрая"
            FilterStrategy.BALANCED -> "Сбалансированная"
            FilterStrategy.TYPE1 -> "Тип тарифа 1"
            FilterStrategy.TYPE2 -> "Тип тарифа 2"
            FilterStrategy.TYPE3 -> "Тип тарифа 3"
        }
        strategyText.text = text
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
        emptyLayout.visibility = View.GONE
    }

    private fun showContent() {
        loadingLayout.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        emptyLayout.visibility = View.GONE
    }

    private fun showEmpty() {
        loadingLayout.visibility = View.GONE
        contentLayout.visibility = View.GONE
        emptyLayout.visibility = View.VISIBLE
    }

    // ------- Загрузка с сервера (без изменений) -------
    private fun loadTariffsFromServer() {
        val url = "$BASE_URL/api/tariffs"

        val requestDto = TariffsRequestDto(
            fromCity = fromCity,
            city = city,
            weight = weightKg,
            strategy = "none"
        )
        val jsonBody = gson.toJson(requestDto)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@TariffsActivity,
                        "Ошибка сети: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showEmpty()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@TariffsActivity,
                                "Ошибка сервера: ${resp.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                            showEmpty()
                        }
                        return
                    }

                    val bodyStr = resp.body?.string()
                    if (bodyStr.isNullOrBlank()) {
                        runOnUiThread {
                            Toast.makeText(
                                this@TariffsActivity,
                                "Пустой ответ сервера",
                                Toast.LENGTH_SHORT
                            ).show()
                            showEmpty()
                        }
                        return
                    }

                    try {
                        val dto = gson.fromJson(bodyStr, TariffsResponseDto::class.java)

                        if (dto.tariffs.isEmpty()) {
                            runOnUiThread { showEmpty() }
                        } else {
                            originalTariffs = dto.tariffs.map { t ->
                                Tariff(
                                    company = t.company,
                                    cargoType = t.cargoType,
                                    tariffType = t.tariffType,
                                    price = t.price,
                                    days = t.days,
                                    isPriceRestored = t.isPriceRestored,
                                    isTimeRestored = t.isTimeRestored,
                                    sourceUrl = t.sourceUrl
                                )
                            }

                            runOnUiThread {
                                applyFilter()
                            }
                        }

                    } catch (e: JsonSyntaxException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@TariffsActivity,
                                "Ошибка формата ответа",
                                Toast.LENGTH_SHORT
                            ).show()
                            showEmpty()
                        }
                    }
                }
            }
        })
    }

    // Остальные методы без изменений (applyFilter, showTariffs, getSizeByWeight)
    private fun applyFilter() {
        if (originalTariffs.isEmpty()) {
            showEmpty()
            return
        }

        val list = when (currentFilter) {
            FilterStrategy.NONE -> originalTariffs
            FilterStrategy.CHEAPEST -> originalTariffs.sortedBy { it.price }
            FilterStrategy.FASTEST -> originalTariffs.sortedBy { it.days }
            FilterStrategy.BALANCED -> {
                val maxPrice = originalTariffs.maxOf { it.price }
                val maxDays = originalTariffs.maxOf { it.days }
                originalTariffs.sortedBy {
                    (it.price / maxPrice) + (it.days.toDouble() / maxDays)
                }
            }
            FilterStrategy.TYPE1 ->
                originalTariffs.filter { it.tariffType == "Тип тарифа 1" }
            FilterStrategy.TYPE2 ->
                originalTariffs.filter { it.tariffType == "Тип тарифа 2" }
            FilterStrategy.TYPE3 ->
                originalTariffs.filter { it.tariffType == "Тип тарифа 3" }
        }

        if (list.isEmpty()) {
            showEmpty()
        } else {
            updateStrategyText()
            showTariffs(list)
        }
    }

    private fun showTariffs(tariffs: List<Tariff>) {
        // ... (код без изменений, как был раньше)
        if (tariffs.isEmpty()) {
            showEmpty()
            return
        }

        showContent()

        val avgPrice = tariffs.map { it.price }.average()
        val avgDays = tariffs.map { it.days }.average()

        avgPriceText.text = "Средняя стоимость: %.2f ₽".format(avgPrice)
        avgTimeText.text = "Среднее время: %.1f дней".format(avgDays)

        tariffsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (tariff in tariffs) {
            val itemView = inflater.inflate(R.layout.item_tariff, tariffsContainer, false)

            val companyText: TextView = itemView.findViewById(R.id.companyText)
            val cargoTypeText: TextView = itemView.findViewById(R.id.cargoTypeText)
            val tariffTypeText: TextView = itemView.findViewById(R.id.tariffTypeText)
            val timeText: TextView = itemView.findViewById(R.id.timeText)
            val priceText: TextView = itemView.findViewById(R.id.priceText)
            val sourceText: TextView = itemView.findViewById(R.id.sourceText)
            val askButton: Button = itemView.findViewById(R.id.askButton)

            val actionsContainer: LinearLayout =
                itemView.findViewById(R.id.tariffActionsContainer)
            val selectTariffButton: Button =
                itemView.findViewById(R.id.selectTariffButton)
            val openSiteButton: Button =
                itemView.findViewById(R.id.openSiteButtonTariff)

            companyText.text = tariff.company
            cargoTypeText.text = "Тип груза: ${tariff.cargoType}"
            tariffTypeText.text = tariff.tariffType
            timeText.text = "Время в пути: ${tariff.days} дн."
            priceText.text = "Стоимость: ${tariff.price} ₽"

            sourceText.text = if (tariff.isPriceRestored || tariff.isTimeRestored) {
                "Стоимость/срок СПРОГНОЗИРОВАНЫ"
            } else {
                "Данные ОТ ПЕРЕВОЗЧИКА"
            }

            var isExpanded = false
            itemView.setOnClickListener {
                isExpanded = !isExpanded
                actionsContainer.visibility =
                    if (isExpanded) View.VISIBLE else View.GONE
            }

            askButton.setOnClickListener {
                val message = "Вопрос по тарифу:\n" +
                        "${tariff.company}, ${tariff.tariffType}\n" +
                        "Маршрут: $fromCity → $city, вес $weightKg кг\n" +
                        "Цена: ${tariff.price} ₽, срок: ${tariff.days} дн."

                val intent = Intent(this, FeedbackActivity::class.java)
                intent.putExtra("prefill_message", message)
                startActivity(intent)
            }

            selectTariffButton.setOnClickListener {
                val size = getSizeByWeight(weightKg)

                val normalizedDeliveryType = when {
                    tariff.tariffType.contains("экспресс", ignoreCase = true) ->
                        "экспресс лайт"
                    tariff.tariffType.contains("эконом", ignoreCase = true) ||
                            tariff.tariffType.contains("стандарт", ignoreCase = true) ->
                        "посылочка (Эконом)"
                    tariff.tariffType.contains("ems", ignoreCase = true) ->
                        "EMS отправление"
                    else ->
                        "посылочка (Эконом)"
                }

                val dbHelper = OrdersDbHelper(this)
                val dbId = try {
                    dbHelper.insertOrder(
                        companyName = tariff.company,
                        deliveryType = normalizedDeliveryType,
                        weightKg = weightKg,
                        size = size,
                        townFrom = fromCity,
                        townTo = city,
                        priceRub = tariff.price.toInt(),
                        timeDays = tariff.days
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Ошибка сохранения заказа: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    -1L
                }

                if (dbId <= 0L) {
                    Toast.makeText(
                        this,
                        "Не удалось сохранить заказ в базу данных",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val order = Order(
                    city = city,
                    weightKg = weightKg,
                    tariff = tariff,
                    isFinished = false
                )
                DataRepository.orders.add(0, order)

                Toast.makeText(
                    this,
                    "Тариф добавлен в заказы",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            openSiteButton.setOnClickListener {
                val url = if (!tariff.sourceUrl.isNullOrBlank()) {
                    tariff.sourceUrl
                } else {
                    val query = Uri.encode(tariff.company)
                    "https://www.google.com/search?q=$query"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }

            tariffsContainer.addView(itemView)
        }
    }

    private fun getSizeByWeight(weightKg: Double): String = when {
        weightKg <= 0.5 -> "XS"
        weightKg <= 2.0 -> "S"
        weightKg <= 5.0 -> "M"
        weightKg <= 10.0 -> "L"
        weightKg <= 20.0 -> "XL"
        else -> "XXL"
    }
}
