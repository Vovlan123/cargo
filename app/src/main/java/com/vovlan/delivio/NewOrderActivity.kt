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

    private lateinit var cityField: TextView
    private lateinit var weightInput: EditText
    private lateinit var unitSpinner: Spinner
    private lateinit var findTariffsButton: Button

    private lateinit var cities: List<String>
    private var selectedCity: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_order)

        cityField = findViewById(R.id.cityField)
        weightInput = findViewById(R.id.weightInput)
        unitSpinner = findViewById(R.id.unitSpinner)
        findTariffsButton = findViewById(R.id.findTariffsButton)

        // 1. Загружаем города из файла res/raw/cities.txt
        cities = resources.openRawResource(R.raw.cities)
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 2. Настраиваем спиннер единиц измерения (кг/г)
        val units = listOf("кг", "г")
        val unitAdapter = ArrayAdapter(
            this,
            R.layout.item_unit_spinner,
            units
        )
        unitAdapter.setDropDownViewResource(R.layout.item_unit_spinner_dropdown)
        unitSpinner.adapter = unitAdapter

        // 3. Если пришли из "Изменить данные"
        val cityFromIntent = intent.getStringExtra("city")
        val weightFromIntent = intent.getDoubleExtra("weightKg", 0.0)

        if (!cityFromIntent.isNullOrBlank()) {
            selectedCity = cityFromIntent
            cityField.text = cityFromIntent
        }

        if (weightFromIntent > 0.0) {
            weightInput.setText(weightFromIntent.toString())
            unitSpinner.setSelection(0) // кг
        }

        // 4. По нажатию на поле города открываем диалог выбора
        cityField.setOnClickListener {
            showCityPickerDialog()
        }

        // 5. Кнопка "Найти тарифы"
        findTariffsButton.setOnClickListener {
            val city = selectedCity
            val weightText = weightInput.text.toString().trim()
            val unit = unitSpinner.selectedItem.toString()

            if (city.isNullOrBlank()) {
                Toast.makeText(this, "Выберите город из списка", Toast.LENGTH_SHORT).show()
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
            intent.putExtra("city", city)
            intent.putExtra("weightKg", weightKg)
            startActivity(intent)

            finish()
        }
    }

    // Диалог с поиском города и списком всех городов
    private fun showCityPickerDialog() {
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
            .create()

        // Делаем фон окна диалога полностью прозрачным, чтобы не было тёмных углов
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        // Выбор города из списка
        listView.setOnItemClickListener { _, _, position, _ ->
            val city = filteredCities[position]
            selectedCity = city
            cityField.text = city
            dialog.dismiss()
        }

        dialog.show()
    }
}