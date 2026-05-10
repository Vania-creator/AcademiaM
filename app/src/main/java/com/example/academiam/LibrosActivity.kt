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

        // 🎯 1: Escala de notas completas
        itemLibro1.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("C", "D", "E", "F", "G", "A", "B", "C"),
                "Actividad 1: Escala Completa",
                R.drawable.partirura1 // Cambia esto por el nombre de tu imagen
            )
        }

        // 🎯 2: Principiantes (> 10 notas) - Himno a la Alegría
        itemLibro2.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("E", "E", "F", "G", "G", "F", "E", "D", "C", "C", "D", "E", "E", "D", "D"),
                "Actividad 2: Principiantes",
                R.drawable.partitura2 // Cambia esto por el nombre de tu imagen
            )
        }

        // 🎯 3: Canción sencilla - Estrellita
        itemLibro3.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("C", "C", "G", "G", "A", "A", "G", "F", "F", "E", "E", "D", "D", "C"),
                "Actividad 3: Canción Sencilla",
                R.drawable.partitura3 // Cambia esto por el nombre de tu imagen
            )
        }

        // 🎯 4: Partitura de la imagen (Acordes rítmicos)
        itemLibro4.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("B", "B", "B", "B", "B", "G", "A", "B"),
                "Actividad 4: Rítmica Avanzada",
                R.drawable.partirura4 // Pon aquí el nombre de la imagen que me enviaste
            )
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun abrirSecuenciaDeAudio(secuencia: ArrayList<String>, titulo: String, imagenResId: Int) {
        try {
            val intent = Intent(this, PartituraInteractivaActivity::class.java)
            intent.putExtra("TITULO_CANCION", titulo)
            intent.putStringArrayListExtra("SECUENCIA_NOTAS", secuencia)
            intent.putExtra("IMAGEN_PARTITURA", imagenResId) // 🔥 Mandamos el ID de la imagen
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastHelper.mostrarMensaje(this, "Error: Revisa que PartituraInteractivaActivity esté en el Manifest")
        }
    }
}