package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class LibrosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libros)

        val itemLibro1 = findViewById<LinearLayout>(R.id.itemLibro1)
        val itemLibro2 = findViewById<LinearLayout>(R.id.itemLibro2)
        val itemLibro3 = findViewById<LinearLayout>(R.id.itemLibro3)
        val itemLibro4 = findViewById<LinearLayout>(R.id.itemLibro4)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarLibros)

        itemLibro1.setOnClickListener {
            abrirLecciones("Piano Adventure 1")
        }

        itemLibro2.setOnClickListener {
            abrirLecciones("Piano Adventure 2")
        }

        itemLibro3.setOnClickListener {
            abrirLecciones("Piano Adventure 3")
        }

        itemLibro4.setOnClickListener {
            abrirLecciones("Piano Adventure 4")
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun abrirLecciones(nombreLibro: String) {
        val intent = Intent(this, LeccionesActivity::class.java)
        intent.putExtra("nombre_libro", nombreLibro)
        startActivity(intent)
    }
}