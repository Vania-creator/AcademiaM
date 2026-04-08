package com.example.academiam

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class LeccionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecciones)

        val nombreLibro = intent.getStringExtra("nombre_libro")

        val itemLeccion1 = findViewById<LinearLayout>(R.id.itemLeccion1)
        val itemLeccion2 = findViewById<LinearLayout>(R.id.itemLeccion2)
        val itemLeccion3 = findViewById<LinearLayout>(R.id.itemLeccion3)
        val itemLeccion4 = findViewById<LinearLayout>(R.id.itemLeccion4)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarLecciones)

        itemLeccion1.setOnClickListener {
        }

        itemLeccion2.setOnClickListener {
        }

        itemLeccion3.setOnClickListener {
        }

        itemLeccion4.setOnClickListener {
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }
}