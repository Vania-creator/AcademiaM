package com.example.academiam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
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

    // UI
    private lateinit var framePartitura: FrameLayout
    private lateinit var viewBarra: View
    private lateinit var tvNotaEscuchada: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvSecuenciaNotas: TextView
    private lateinit var btnAtras: AppCompatButton
    private lateinit var btnGrabar: AppCompatButton

    private val CODIGO_PERMISO_MIC = 1001

    // Lógica
    private var melodia = ArrayList<String>()
    private var indiceActual = 0
    private var ultimaNotaEscuchada = ""
    private var estaPreparado = false // 🔥 Controla que no lea notas hasta terminar el "Prepárate"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partitura_interactiva)

        framePartitura = findViewById(R.id.framePartitura)
        viewBarra = findViewById(R.id.viewBarra)
        tvNotaEscuchada = findViewById(R.id.tvNotaEscuchada)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvSecuenciaNotas = findViewById(R.id.tvSecuenciaNotas)
        btnAtras = findViewById(R.id.btnAtras)
        btnGrabar = findViewById(R.id.btnGrabar)

        val titulo = intent.getStringExtra("TITULO_CANCION") ?: "Práctica Libre"
        findViewById<TextView>(R.id.tvTituloCancion).text = titulo

        // 🔥 FUNCIONES DE LOS BOTONES
        btnAtras.setOnClickListener {
            finish() // Regresa a la pantalla de libros
        }

        btnGrabar.setOnClickListener {
            Toast.makeText(this, "Próximamente: Se activará la grabación para el maestro.", Toast.LENGTH_SHORT).show()
            // Aquí irá el código para grabar la sesión en un archivo y subirlo a Firebase
        }

        val notasArray = intent.getStringArrayListExtra("SECUENCIA_NOTAS")
        if (notasArray != null && notasArray.isNotEmpty()) {
            melodia = ArrayList(notasArray)
            tvSecuenciaNotas.text = "Secuencia: ${melodia.joinToString(" - ")}"

            moverBarraVisual()
            iniciarCuentaRegresiva() // 🔥 Iniciamos la secuencia de "Prepárate"
        } else {
            Toast.makeText(this, "Error: Melodía vacía", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // --- NUEVO: CUENTA REGRESIVA DE PREPARACIÓN ---
    private fun iniciarCuentaRegresiva() {
        tvFeedback.text = "¡Prepárate!"
        tvFeedback.setTextColor(Color.parseColor("#FF9800")) // Naranja

        // Un timer de 3 segundos (3000ms), que se actualiza cada segundo (1000ms)
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000 + 1
                tvFeedback.text = "Empezamos en... $segundos"
            }

            override fun onFinish() {
                estaPreparado = true // Desbloqueamos el juego
                actualizarTextoTurno()
                verificarPermisosYComenzar() // Ahora sí, encendemos el micrófono
            }
        }.start()
    }

    private fun actualizarTextoTurno() {
        if (indiceActual < melodia.size) {
            tvFeedback.text = "Turno de tocar: ${melodia[indiceActual]}"
            tvFeedback.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    private fun moverBarraVisual() {
        framePartitura.post {
            val anchoTotal = framePartitura.width.toFloat()
            val tamañoPaso = anchoTotal / melodia.size
            val nuevaPosicionX = tamañoPaso * indiceActual

            viewBarra.animate()
                .translationX(nuevaPosicionX)
                .setDuration(250)
                .start()
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
                Toast.makeText(this, "Permiso denegado.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun iniciarDeteccion() {
        val handler = PitchDetectionHandler { result, _ ->
            val pitch = result.pitch
            if (pitch > 0) {
                val notaDetectada = getNoteFromFrequency(pitch)
                runOnUiThread {
                    procesarJugada(notaDetectada)
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
                Toast.makeText(this, "Error al iniciar micrófono: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun procesarJugada(notaDetectada: String) {
        // 🔥 Si aún no termina el "Prepárate", ignoramos el sonido
        if (!estaPreparado) return

        tvNotaEscuchada.text = "Escuchando: $notaDetectada"

        if (indiceActual >= melodia.size) return

        if (notaDetectada == ultimaNotaEscuchada) return
        ultimaNotaEscuchada = notaDetectada

        val notaObjetivoActual = melodia[indiceActual]

        if (notaDetectada.startsWith(notaObjetivoActual)) {
            indiceActual++
            moverBarraVisual()

            if (indiceActual == melodia.size) {
                tvFeedback.text = "¡COMPLETADO! 🎉"
                tvFeedback.setTextColor(Color.parseColor("#4CAF50"))
                tvSecuenciaNotas.text = "¡Bien hecho!"
                tvNotaEscuchada.text = "Has tocado toda la secuencia."

                if (::dispatcher.isInitialized && !dispatcher.isStopped) {
                    dispatcher.stop()
                }
            } else {
                actualizarTextoTurno()
            }
        }
    }

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