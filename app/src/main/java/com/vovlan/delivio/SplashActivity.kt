package com.vovlan.delivio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем разметку с надписью Delivio
        setContentView(R.layout.activity_splash)

        // Запускаем таймер на 500 миллисекунд
        Handler(Looper.getMainLooper()).postDelayed({
            // Создаём намерение открыть главный экран
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Закрываем заставку, чтобы по "назад" не возвращаться на неё
            finish()
        }, 500)
    }
}