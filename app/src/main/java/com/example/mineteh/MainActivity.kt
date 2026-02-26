package com.example.mineteh

import android.content.Intent
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getStarted = findViewById<MaterialButton>(R.id.getStarted)
        getStarted.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}
