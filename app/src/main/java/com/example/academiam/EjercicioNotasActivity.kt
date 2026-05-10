package com.example.academiam

import android.Manifest
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

class EjercicioSecuenciaActivity : AppCompatActivity() {

    private lateinit var tvNotaObjetivo: TextView
    private lateinit var tvResultado: TextView
    private lateinit var dispatcher: AudioDispatcher

    // Lógica para la secuencia
    private var melodia = ArrayList<String>()
    private var indiceActual = 0
    private val CODIGO_PERMISO_MIC = 1001

    // Seguro para que una nota no se registre 100 veces por segundo
    private var ultimaNotaEscuchada = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ejercicio_notas)

        tvNotaObjetivo = findViewById(R.id.tvNotaObjetivo)
        tvResultado = findViewById(R.id.tvResultado)

        // Recibir la lista de notas
        melodia = intent.getStringArrayListExtra("SECUENCIA_NOTAS") ?: arrayListOf("C")

        actualizarTextoObjetivo()
        verificarPermisosYComenzar()
    }

    private fun actualizarTextoObjetivo() {
        if (indiceActual < melodia.size) {
            tvNotaObjetivo.text = "Toca la nota: ${melodia[indiceActual]}"
        }
    }

    private fun verificarPermisosYComenzar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), CODIGO_PERMISO_MIC)
        } else {
            iniciarDeteccion()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODIGO_PERMISO_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarDeteccion()
            } else {
                ToastHelper.mostrarMensaje(this, "Permiso denegado.")
                finish()
            }
        }
    }

    // 🔥 EXACTAMENTE TU CÓDIGO ORIGINAL 🔥
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
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
            dispatcher.addAudioProcessor(processor)
            Thread(dispatcher, "Audio Dispatcher").start()
        } catch (e: Exception) {
            runOnUiThread {
                ToastHelper.mostrarMensaje(this, "Error al iniciar micrófono: ${e.message}")
            }
        }
    }

    private fun verificarNota(notaDetectada: String) {
        // Si ya completamos la canción, ignoramos el micrófono
        if (indiceActual >= melodia.size) return

        // Evitar saturación si se mantiene la misma nota sonando
        if (notaDetectada == ultimaNotaEscuchada) return
        ultimaNotaEscuchada = notaDetectada

        val notaObjetivoActual = melodia[indiceActual]

        if (notaDetectada.startsWith(notaObjetivoActual)) {
            tvResultado.text = "✔ Nota Correcta ($notaDetectada)"
            tvResultado.setTextColor(Color.GREEN)

            // Avanzamos de nota
            indiceActual++

            if (indiceActual == melodia.size) {
                // ¡FINALIZÓ LA PARTITURA!
                tvNotaObjetivo.text = "¡COMPLETADO! 🎉"
                tvNotaObjetivo.setTextColor(Color.parseColor("#4CAF50"))
                tvResultado.text = "Has tocado toda la secuencia correctamente."
                if (::dispatcher.isInitialized && !dispatcher.isStopped) {
                    dispatcher.stop()
                }
            } else {
                actualizarTextoObjetivo() // Mostramos la siguiente nota a tocar
            }

        } else {
            tvResultado.text = "❌ Incorrecto ($notaDetectada)"
            tvResultado.setTextColor(Color.RED)
        }
    }

    // 🔥 EXACTAMENTE TU CÓDIGO ORIGINAL 🔥
    private fun getNoteFromFrequency(freq: Float): String {
        val noteNumber = (12 * (ln(freq / 440.0) / ln(2.0)) + 69).roundToInt()
        val index = (noteNumber % 12 + 12) % 12
        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        return notes[index]
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dispatcher.isInitialized && !dispatcher.isStopped) {
            dispatcher.stop()
        }
    }
}