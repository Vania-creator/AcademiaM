package com.example.academiam

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
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

        // 🎯 LECCIÓN 1 → NOTA C
        itemLeccion1.setOnClickListener {
            abrirEjercicio("C")
        }

        // 🎯 LECCIÓN 2 → NOTA D
        itemLeccion2.setOnClickListener {
            abrirEjercicio("D")
        }

        // 🎯 LECCIÓN 3 → NOTA E
        itemLeccion3.setOnClickListener {
            abrirEjercicio("E")
        }

        // 🎯 LECCIÓN 4 → NOTA F
        itemLeccion4.setOnClickListener {
            abrirEjercicio("F")
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    // 🔥 FUNCIÓN PARA ABRIR EL EJERCICIO (Actualizada con protección anti-cierres)
    private fun abrirEjercicio(nota: String) {
        try {
            //  val intent = Intent(this, EjercicioNotasActivity::class.java)
            intent.putExtra("NOTA_OBJETIVO", nota)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Este error saldrá si faltó configurar el Manifest
            Toast.makeText(this, "Error: EjercicioNotasActivity no está en el Manifest", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Este error saldrá si hay otro problema crítico (como falta de permisos de micrófono)
            Toast.makeText(this, "Error al abrir: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}