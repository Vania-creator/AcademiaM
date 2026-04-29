package com.example.academiam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlin.math.ln
import kotlin.math.roundToInt

class EjercicioNotasActivity : AppCompatActivity() {

    private lateinit var tvNotaObjetivo: TextView
    private lateinit var tvResultado: TextView
    private lateinit var dispatcher: AudioDispatcher

    private var notaObjetivo = "C"
    private val CODIGO_PERMISO_MIC = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ejercicio_notas)

        tvNotaObjetivo = findViewById(R.id.tvNotaObjetivo)
        tvResultado = findViewById(R.id.tvResultado)

        // Recibir la nota objetivo enviada desde Lecciones o Libros
        notaObjetivo = intent.getStringExtra("NOTA_OBJETIVO") ?: "C"
        tvNotaObjetivo.text = "Toca la nota: $notaObjetivo"

        // 🔥 PASO 1: Verificar permisos de micrófono antes de encender el motor de audio
        verificarPermisosYComenzar()
    }

    private fun verificarPermisosYComenzar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Si no hay permiso, lo pedimos formalmente al usuario
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), CODIGO_PERMISO_MIC)
        } else {
            // Si ya hay permiso, iniciamos la detección
            iniciarDeteccion()
        }
    }

    // 🔥 PASO 2: Manejar la respuesta del usuario a la solicitud de permiso
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_PERMISO_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarDeteccion()
            } else {
                Toast.makeText(this, "El permiso de micrófono es necesario para evaluar las notas.", Toast.LENGTH_LONG).show()
                finish() // Regresar a la pantalla anterior si no hay permiso
            }
        }
    }

    private fun iniciarDeteccion() {
        val handler = PitchDetectionHandler { result, _ ->
            val pitch = result.pitch
            if (pitch > 0) {
                val notaDetectada = getNoteFromFrequency(pitch)
                runOnUiThread {
                    verificarNota(notaDetectada)
                }
            }
        }

        val processor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            22050f,
            1024,
            handler
        )

        try {
            // Inicializar el despachador de audio específico para Android
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
            dispatcher.addAudioProcessor(processor)

            // Iniciar el hilo de procesamiento
            Thread(dispatcher, "Audio Dispatcher").start()
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Error al iniciar micrófono: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarNota(notaDetectada: String) {
        // Comparamos si la nota detectada es la que buscamos
        if (notaDetectada.startsWith(notaObjetivo)) {
            tvResultado.text = "✔ Nota Correcta ($notaDetectada)"
            tvResultado.setTextColor(Color.GREEN)

            // 🎯 DISEÑO: No detenemos el dispatcher ni cerramos la actividad.
            // Esto permite que sigas practicando la nota y veas el cambio en tiempo real.

        } else {
            tvResultado.text = "❌ Incorrecto ($notaDetectada)"
            tvResultado.setTextColor(Color.RED)
        }
    }

    private fun getNoteFromFrequency(freq: Float): String {
        // Cálculo matemático para convertir frecuencia (Hz) a nota musical
        val noteNumber = (12 * (ln(freq / 440.0) / ln(2.0)) + 69).roundToInt()

        // 🔥 PROTECCIÓN: Evita índices negativos por ruidos extraños
        val index = (noteNumber % 12 + 12) % 12
        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        return notes[index]
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar el micrófono al salir de la pantalla para evitar fugas de memoria
        if (::dispatcher.isInitialized) {
            dispatcher.stop()
        }
    }
}