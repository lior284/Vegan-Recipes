package com.example.vegan_recipes

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.net.toUri

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val back = findViewById<TextView>(R.id.userDetails_backArrow_iv)
        val phone = findViewById<TextView>(R.id.about_bugPhone_tv)

        back.setOnClickListener {
            finish()
        }

        phone.setOnClickListener {
            val messageIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = "smsto:+972584261208".toUri()
            }
            startActivity(messageIntent)
        }
    }
}
