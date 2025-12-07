package com.vovlan.delivio

import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
fun AppCompatActivity.noActivityTransition() {
    overridePendingTransition(0, 0)
}