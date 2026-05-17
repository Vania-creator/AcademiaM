package com.example.academiam

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class PartituraCapasActivity : AppCompatActivity() {

    private lateinit var ivCapaPartitura: ImageView
    private lateinit var gestureDetector: GestureDetector

    // Aquí defines el orden de las 3 imágenes que se van a ciclar.
    // Asegúrate de que los nombres coincidan exactamente con los archivos PNG en res/drawable/
    private val capas = intArrayOf(
        R.drawable.original, // Nivel 1: Partitura normal
        R.drawable.capa1,    // Nivel 2: Primera ayuda visual
        R.drawable.capa2,
        R.drawable.capa3// Nivel 3: Ayuda visual avanzada
    )

    private var indiceCapaActual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partitura_capas)

        ivCapaPartitura = findViewById(R.id.ivCapaPartitura)
        val btnAtras = findViewById<AppCompatButton>(R.id.btnAtrasCapas)

        btnAtras.setOnClickListener { finish() }

        // Cargar la primera imagen al abrir la pantalla
        ivCapaPartitura.setImageResource(capas[indiceCapaActual])

        // Configurar el detector de gestos para atrapar el "Doble Toque"
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                cambiarCapaConAnimacion()
                return true
            }
        })

        // Asignar el detector de gestos al ImageView
        ivCapaPartitura.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true // Devuelve true para indicar que consumimos el toque
        }
    }

    private fun cambiarCapaConAnimacion() {
        // Avanzamos al siguiente índice. Si llega a 3, regresa al 0 gracias al operador módulo (%)
        indiceCapaActual = (indiceCapaActual + 1) % capas.size

        // Animación de Crossfade (Desvanecimiento cruzado)
        ivCapaPartitura.animate()
            .alpha(0f) // Desvanece la imagen actual
            .setDuration(250) // Duración de 250 milisegundos
            .withEndAction {
                // Justo cuando se vuelve invisible, cambiamos la imagen de fondo
                ivCapaPartitura.setImageResource(capas[indiceCapaActual])

                // Y la hacemos aparecer suavemente
                ivCapaPartitura.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
    }
}