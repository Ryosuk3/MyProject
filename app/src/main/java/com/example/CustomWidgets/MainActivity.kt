package com.example.CustomWidgets

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

data class Widgets(val name: String, val image: String)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}