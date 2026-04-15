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

        // 🎯 TEMPORAL: Asignamos una nota de prueba a cada "libro"
        itemLibro1.setOnClickListener {
            abrirPruebaDeAudio("C")
        }

        itemLibro2.setOnClickListener {
            abrirPruebaDeAudio("D")
        }

        itemLibro3.setOnClickListener {
            abrirPruebaDeAudio("E")
        }

        itemLibro4.setOnClickListener {
            abrirPruebaDeAudio("F")
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    // 🔥 FUNCIÓN TEMPORAL: Salta directamente a la verificación de micrófono
    private fun abrirPruebaDeAudio(notaPrueba: String) {
        try {
            // Cambiamos el destino a EjercicioNotasActivity
            val intent = Intent(this, EjercicioNotasActivity::class.java)

            // Mandamos "NOTA_OBJETIVO" en lugar de "nombre_libro" para que la otra pantalla lo entienda
            intent.putExtra("NOTA_OBJETIVO", notaPrueba)

            startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Error: Revisa el Manifest", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}