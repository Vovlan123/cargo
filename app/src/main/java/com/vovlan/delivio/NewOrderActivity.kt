package com.vovlan.delivio

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.graphics.drawable.ColorDrawable

class NewOrderActivity : AppCompatActivity() {

    // Поля городов
    private lateinit var fromCityField: TextView   // Город ОТКУДА
    private lateinit var cityField: TextView       // Город КУДА

    // Вес
    private lateinit var weightInput: EditText
    private lateinit var unitSpinner: Spinner
    private lateinit var findTariffsButton: Button

    // Список городов
    private lateinit var cities: List<String>

    private var selectedFromCity: String? = null   // выбранный город отправления
    private var selectedCity: String? = null       // выбранный город назначения

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        // 1. Инициализация вьюх
        fromCityField = findViewById(R.id.fromCityField)
        cityField = findViewById(R.id.cityField)

        weightInput = findViewById(R.id.weightInput)
        unitSpinner = findViewById(R.id.unitSpinner)
        findTariffsButton = findViewById(R.id.findTariffsButton)

        // 2. Загружаем города из файла res/raw/cities.txt
        cities = resources.openRawResource(R.raw.cities)
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 3. Настраиваем спиннер единиц измерения (кг/г)
        val units = listOf("кг", "г")
        val unitAdapter = ArrayAdapter(
            this,
            R.layout.item_unit_spinner,
            units
        )
        unitAdapter.setDropDownViewResource(R.layout.item_unit_spinner_dropdown)
        unitSpinner.adapter = unitAdapter

        // 4. Если пришли из "Изменить данные"
        val fromCityFromIntent = intent.getStringExtra("fromCity")
        val cityFromIntent = intent.getStringExtra("city")
        val weightFromIntent = intent.getDoubleExtra("weightKg", 0.0)

        if (!fromCityFromIntent.isNullOrBlank()) {
            selectedFromCity = fromCityFromIntent
            fromCityField.text = fromCityFromIntent
        }

        if (!cityFromIntent.isNullOrBlank()) {
            selectedCity = cityFromIntent
            cityField.text = cityFromIntent
        }

        if (weightFromIntent > 0.0) {
            weightInput.setText(weightFromIntent.toString())
            unitSpinner.setSelection(0) // кг
        }

        // 5. По нажатию на поле города отправления — диалог выбора
        fromCityField.setOnClickListener {
            showCityPickerDialog(isFromCity = true)
        }

        // 6. По нажатию на поле города назначения — такой же диалог выбора
        cityField.setOnClickListener {
            showCityPickerDialog(isFromCity = false)
        }

        // 7. Кнопка "Найти тарифы"
        findTariffsButton.setOnClickListener {
            val fromCity = selectedFromCity
            val toCity = selectedCity
            val weightText = weightInput.text.toString().trim()
            val unit = unitSpinner.selectedItem.toString()

            if (fromCity.isNullOrBlank()) {
                Toast.makeText(this, "Выберите город отправления", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (toCity.isNullOrBlank()) {
                Toast.makeText(this, "Выберите город назначения", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (weightText.isBlank()) {
                Toast.makeText(this, "Введите вес", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weightValue = weightText.toDoubleOrNull()
            if (weightValue == null || weightValue <= 0.0) {
                Toast.makeText(this, "Некорректный вес", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weightKg = if (unit == "г") {
                weightValue / 1000.0
            } else {
                weightValue
            }

            val intent = Intent(this, TariffsActivity::class.java)
            intent.putExtra("fromCity", fromCity)
            intent.putExtra("city", toCity)
            intent.putExtra("weightKg", weightKg)
            startActivity(intent)

            // После выбора тарифов этот экран нам больше не нужен
            finish()
        }
    }

    /**
     * Диалог с поиском города и списком всех городов.
     * isFromCity = true  -> выбираем город отправления, заполняем fromCityField
     * isFromCity = false -> выбираем город назначения, заполняем cityField
     */
    private fun showCityPickerDialog(isFromCity: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_city_picker, null)
        val searchInput = dialogView.findViewById<EditText>(R.id.searchCityInput)
        val listView = dialogView.findViewById<ListView>(R.id.cityListView)

        // Список, который будем фильтровать
        val filteredCities = cities.toMutableList()

        val adapter = ArrayAdapter(
            this,
            R.layout.item_city_list,
            filteredCities
        )
        listView.adapter = adapter

        // Фильтр по вводу
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                filteredCities.clear()
                if (query.isEmpty()) {
                    filteredCities.addAll(cities)
                } else {
                    filteredCities.addAll(
                        cities.filter { it.lowercase().contains(query) }
                    )
                }
                adapter.notifyDataSetChanged()
            }
        })

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // При нажатии вне окна диалога — закрываем диалог и возвращаемся в MainActivity
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            // диалог отменён (тап снаружи или кнопка "Назад") —
            // закрываем NewOrderActivity и возвращаемся на главный экран
            finish()
        }

        // Прозрачный фон, чтобы не было тёмных углов
        dialog.window?.setBackgroundDrawable(
            ColorDrawable(android.graphics.Color.TRANSPARENT)
        )

        // Выбор города
        listView.setOnItemClickListener { _, _, position, _ ->
            val city = filteredCities[position]
            if (isFromCity) {
                selectedFromCity = city
                fromCityField.text = city
            } else {
                selectedCity = city
                cityField.text = city
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}