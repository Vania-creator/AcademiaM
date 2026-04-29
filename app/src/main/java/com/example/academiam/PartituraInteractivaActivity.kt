package com.example.academiam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
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

class PartituraInteractivaActivity : AppCompatActivity() {

    private lateinit var dispatcher: AudioDispatcher
    private lateinit var containerNotas: LinearLayout
    private lateinit var scrollPartitura: HorizontalScrollView
    private lateinit var tvNotaEscuchada: TextView
    private lateinit var tvFeedback: TextView

    private val CODIGO_PERMISO_MIC = 1001

    // --- LÓGICA DE LA PARTITURA ---
    private var melodia = listOf<String>()
    private var indiceActual = 0
    private var ultimoAciertoMs: Long = 0
    private val COOLDOWN_NOTAS_MS = 600 // Pausa de medio segundo para no saltar notas dobles de golpe

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partitura_interactiva)

        containerNotas = findViewById(R.id.containerNotas)
        scrollPartitura = findViewById(R.id.scrollPartitura)
        tvNotaEscuchada = findViewById(R.id.tvNotaEscuchada)
        tvFeedback = findViewById(R.id.tvFeedback)

        val titulo = intent.getStringExtra("TITULO_CANCION") ?: "Práctica Libre"
        findViewById<TextView>(R.id.tvTituloCancion).text = titulo

        val notasArray = intent.getStringArrayListExtra("SECUENCIA_NOTAS")
        if (notasArray != null && notasArray.isNotEmpty()) {
            melodia = notasArray.toList()
            dibujarPartitura()
            verificarPermisosYComenzar()
        } else {
            Toast.makeText(this, "Error: Melodía vacía", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun dibujarPartitura() {
        containerNotas.removeAllViews()
        for (nota in melodia) {
            val tvNota = TextView(this).apply {
                text = nota
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.circulo_gris)

                val params = LinearLayout.LayoutParams(120, 120)
                params.setMargins(15, 0, 15, 0)
                layoutParams = params
            }
            containerNotas.addView(tvNota)
        }
        actualizarMarcadorVisual()
    }

    private fun actualizarMarcadorVisual() {
        for (i in 0 until containerNotas.childCount) {
            val tv = containerNotas.getChildAt(i) as TextView
            when {
                i < indiceActual -> { // YA TOCADA
                    tv.setTextColor(Color.WHITE)
                    tv.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde
                    tv.alpha = 0.5f
                    tv.scaleX = 0.8f
                    tv.scaleY = 0.8f
                }
                i == indiceActual -> { // TURNO ACTUAL
                    tv.setTextColor(Color.BLACK)
                    tv.setBackgroundColor(Color.parseColor("#FFC107")) // Amarillo
                    tv.alpha = 1.0f
                    tv.scaleX = 1.2f
                    tv.scaleY = 1.2f

                    // Auto-scroll
                    val scrollX = (tv.left + tv.right) / 2 - scrollPartitura.width / 2
                    scrollPartitura.smoothScrollTo(scrollX, 0)
                }
                else -> { // FUTURAS
                    tv.setTextColor(Color.WHITE)
                    tv.setBackgroundResource(R.drawable.circulo_gris)
                    tv.alpha = 0.8f
                    tv.scaleX = 1.0f
                    tv.scaleY = 1.0f
                }
            }
        }
    }

    // 🔥 CÓDIGO INTACTO DE TU COMPAÑERO ABAJO DE ESTA LÍNEA 🔥

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
                Toast.makeText(this, "El permiso de micrófono es necesario.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun iniciarDeteccion() {
        val handler = PitchDetectionHandler { result, _ ->
            val pitch = result.pitch

            // 🔥 LA SOLUCIÓN: result.isPitched diferencia la música del ruido de fondo
            if (pitch > 0 && result.isPitched) {
                val notaDetectada = getNoteFromFrequency(pitch)
                runOnUiThread {
                    procesarJugada(notaDetectada)
                }
            } else {
                // Si es puro ruido, limpiamos el texto para que no se quede pegada la "G"
                runOnUiThread {
                    tvNotaEscuchada.text = "Esperando nota..."
                }
            }
        }

        val processor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, 22050f, 1024, handler)

        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
            dispatcher.addAudioProcessor(processor)
            Thread(dispatcher, "Audio Dispatcher").start()
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Error al iniciar micrófono: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun getNoteFromFrequency(freq: Float): String {
        val noteNumber = (12 * (ln(freq / 440.0) / ln(2.0)) + 69).roundToInt()
        val index = (noteNumber % 12 + 12) % 12
        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        return notes[index]
    }

    // --- CONEXIÓN ENTRE LA IA Y EL JUEGO ---
    private fun procesarJugada(notaDetectada: String) {
        // Mostramos lo que la IA lee, exactamente como lo hacía tu compañero
        tvNotaEscuchada.text = "Escuchando: $notaDetectada"

        if (indiceActual >= melodia.size) return

        val tiempoActual = System.currentTimeMillis()
        val notaObjetivo = melodia[indiceActual]

        // Comparamos si le atinó
        if (notaDetectada.startsWith(notaObjetivo) && (tiempoActual - ultimoAciertoMs > COOLDOWN_NOTAS_MS)) {
            ultimoAciertoMs = tiempoActual
            indiceActual++
            actualizarMarcadorVisual()

            if (indiceActual == melodia.size) {
                tvFeedback.text = "¡Felicidades! 🎉"
                tvFeedback.setBackgroundColor(Color.parseColor("#4CAF50"))
                tvNotaEscuchada.text = "Partitura Completada"
                if (::dispatcher.isInitialized) dispatcher.stop()
            } else {
                tvFeedback.text = "Siguiente nota: ${melodia[indiceActual]}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::dispatcher.isInitialized && !dispatcher.isStopped) {
            dispatcher.stop()
        }
    }
}