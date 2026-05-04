package com.example.academiam

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
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

        // 🎯 Mandamos una SECUENCIA de notas para cada libro y su título
        itemLibro1.setOnClickListener {
            abrirSecuenciaDeAudio(arrayListOf("C", "D", "E", "F"), "Libro 1: Notas Básicas")
        }

        itemLibro2.setOnClickListener {
            abrirSecuenciaDeAudio(arrayListOf("G", "A", "B", "C"), "Libro 2: Escala Alta")
        }

        itemLibro3.setOnClickListener {
            abrirSecuenciaDeAudio(arrayListOf("C", "C", "G", "G"), "Libro 3: Estrellita")
        }

        itemLibro4.setOnClickListener {
            abrirSecuenciaDeAudio(arrayListOf("E", "D", "C", "D", "E", "E", "E"), "Libro 4: Práctica")
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun abrirSecuenciaDeAudio(secuencia: ArrayList<String>, titulo: String) {
        try {
            // Mandamos a la actividad correcta: PartituraInteractivaActivity
            val intent = Intent(this, PartituraInteractivaActivity::class.java)
            intent.putExtra("TITULO_CANCION", titulo)
            intent.putStringArrayListExtra("SECUENCIA_NOTAS", secuencia)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Error: Revisa que PartituraInteractivaActivity esté en el Manifest", Toast.LENGTH_LONG).show()
        }
    }
}