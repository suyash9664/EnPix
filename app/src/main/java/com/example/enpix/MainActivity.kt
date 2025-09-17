package com.example.enpix

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEncrypt = findViewById<MaterialButton>(R.id.btnEncrypt)
        val btnDecrypt = findViewById<MaterialButton>(R.id.btnDecrypt)

        btnEncrypt.setOnClickListener {
            startActivity(Intent(this, EncryptActivity::class.java))
        }

        btnDecrypt.setOnClickListener {
            startActivity(Intent(this, DecryptActivity::class.java))
        }
    }
}
