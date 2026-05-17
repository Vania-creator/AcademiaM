package com.example.academiam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

class PartituraInteractivaActivity : AppCompatActivity() {

    private lateinit var dispatcher: AudioDispatcher

    // --- UI: NUEVAS REFERENCIAS PARA LA PARTITURA DINÁMICA ---
    private lateinit var hScrollViewPartitura: HorizontalScrollView
    private lateinit var partituraView: PartituraView // TU NUEVA CLASE PERSONALIZADA
    private lateinit var tvNotaEscuchada: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var tvSecuenciaNotas: TextView
    private lateinit var btnAtras: AppCompatButton
    private lateinit var btnGrabar: AppCompatButton
    private lateinit var btnRepetir: AppCompatButton

    // Lógica del juego y Firestore
    private var ultimoAciertoMs: Long = 0
    private val COOLDOWN_NOTAS_MS = 250
    private val CODIGO_PERMISO_MIC = 1001

    private var melodia = ArrayList<String>()
    private var indiceActual = 0
    private var estaPreparado = false
    private var erroresCometidos = 0
    private var studentId = ""
    private var juegoTerminado = false

    // Lógica de grabación local
    private var mediaRecorder: MediaRecorder? = null
    private var rutaAudio: String = ""
    private var timerGrabacion: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partitura_interactiva)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""

        // Conectando el código con el XML nuevo
        hScrollViewPartitura = findViewById(R.id.hScrollViewPartitura)
        partituraView = findViewById(R.id.partituraView) // YA NO EXISTE viewBarra ni ivPartitura

        tvNotaEscuchada = findViewById(R.id.tvNotaEscuchada)
        tvFeedback = findViewById(R.id.tvFeedback)
        tvSecuenciaNotas = findViewById(R.id.tvSecuenciaNotas)
        btnAtras = findViewById(R.id.btnAtras)
        btnGrabar = findViewById(R.id.btnGrabar)
        btnRepetir = findViewById(R.id.btnRepetir)

        val titulo = intent.getStringExtra("TITULO_CANCION") ?: "Práctica Libre"
        findViewById<TextView>(R.id.tvTituloCancion).text = titulo

        btnAtras.setOnClickListener { finish() }
        btnRepetir.setOnClickListener { reiniciarJuego() }

        btnGrabar.setOnClickListener {
            if (mediaRecorder == null) iniciarGrabacion() else detenerYGuardarAudio()
        }

        val notasArray = intent.getStringArrayListExtra("SECUENCIA_NOTAS")
        if (notasArray != null && notasArray.isNotEmpty()) {
            melodia = ArrayList(notasArray)
            tvSecuenciaNotas.text = "Secuencia: ${melodia.joinToString(" - ")}"

            // === LA MAGIA ===
            // Mandamos el arreglo a la vista para que dibuje las líneas y bolitas
            partituraView.setNotas(melodia)

            iniciarCuentaRegresiva()
        } else {
            Toast.makeText(this, "Error: Melodía vacía", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun reiniciarJuego() {
        if (::dispatcher.isInitialized && !dispatcher.isStopped) dispatcher.stop()

        indiceActual = 0
        erroresCometidos = 0
        juegoTerminado = false
        estaPreparado = false
        ultimoAciertoMs = 0

        tvFeedback.text = "¡Listo de nuevo!"
        tvFeedback.setTextColor(Color.BLACK)
        tvNotaEscuchada.text = "Esperando nota..."
        btnRepetir.visibility = View.GONE

        // Limpiamos los colores del lienzo y regresamos el scroll al principio
        partituraView.reiniciar()
        hScrollViewPartitura.scrollTo(0, 0)

        iniciarCuentaRegresiva()
    }

    private fun iniciarGrabacion() {
        val directorio = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val nombreArchivo = "Practica_${System.currentTimeMillis()}.3gp"
        rutaAudio = "${directorio?.absolutePath}/$nombreArchivo"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(rutaAudio)

            try {
                prepare()
                start()
                btnGrabar.setBackgroundColor(Color.GRAY)

                timerGrabacion = object : CountDownTimer(30000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        btnGrabar.text = "⏹ Grabando... (${millisUntilFinished / 1000}s restantes)"
                    }
                    override fun onFinish() { detenerYGuardarAudio() }
                }.start()
            } catch (e: Exception) {
                Toast.makeText(this@PartituraInteractivaActivity, "Error al iniciar grabadora: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun detenerYGuardarAudio() {
        try {
            timerGrabacion?.cancel()
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            Toast.makeText(this, "¡Audio guardado exitosamente!", Toast.LENGTH_LONG).show()
            btnGrabar.text = "🔴 Grabar Nueva Práctica"
            btnGrabar.isEnabled = true
            btnGrabar.setBackgroundColor(Color.parseColor("#D32F2F"))

        } catch(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarCuentaRegresiva() {
        tvFeedback.text = "¡Prepárate!"
        tvFeedback.setTextColor(Color.parseColor("#FF9800"))
        erroresCometidos = 0

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundos = millisUntilFinished / 1000 + 1
                tvFeedback.text = "Empezamos en... $segundos"
            }
            override fun onFinish() {
                estaPreparado = true
                actualizarTextoTurno()
                verificarPermisosYComenzar()
            }
        }.start()
    }

    private fun actualizarTextoTurno() {
        if (indiceActual < melodia.size) {
            tvFeedback.text = "Turno de tocar: ${melodia[indiceActual]}"
            tvFeedback.setTextColor(Color.parseColor("#4CAF50"))
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
                ToastHelper.mostrarMensaje(this, "Permiso de micrófono denegado.")
                finish()
            }
        }
    }

    // EL MICRÓFONO QUEDA INTACTO COMO PEDISTE
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

        val processor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, 22050f, 1024, handler)

        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
            dispatcher.addAudioProcessor(processor)
            Thread(dispatcher, "Audio Dispatcher").start()
        } catch (e: Exception) {
            runOnUiThread { ToastHelper.mostrarMensaje(this, "Error al iniciar micrófono") }
        }
    }

    private fun procesarJugada(notaDetectada: String) {
        if (!estaPreparado) return

        val tiempoActual = System.currentTimeMillis()
        tvNotaEscuchada.text = "Escuchando: $notaDetectada"

        if (indiceActual >= melodia.size) return
        if (tiempoActual - ultimoAciertoMs < COOLDOWN_NOTAS_MS) return

        val notaObjetivoActual = melodia[indiceActual]

        if (notaDetectada.startsWith(notaObjetivoActual)) {
            ultimoAciertoMs = tiempoActual

            // Pinta de VERDE la nota en la que acertó
            partituraView.marcarAcierto(indiceActual)
            indiceActual++

            // Movemos el scroll para centrar la siguiente nota (180 es el espaciado configurado)
            hScrollViewPartitura.smoothScrollBy(180, 0)

            if (indiceActual == melodia.size) {
                tvFeedback.text = "¡COMPLETADO! 🎉"
                tvFeedback.setTextColor(Color.parseColor("#4CAF50"))
                tvSecuenciaNotas.text = "Errores cometidos: $erroresCometidos"
                tvNotaEscuchada.text = "Has terminado la secuencia."

                runOnUiThread { btnRepetir.visibility = View.VISIBLE }
                if (::dispatcher.isInitialized && !dispatcher.isStopped) dispatcher.stop()

                evaluarYOthorgarInsignia()
            } else {
                actualizarTextoTurno()
            }
        } else {
            ultimoAciertoMs = tiempoActual
            erroresCometidos++

            // Pinta de ROJO la nota temporalmente
            partituraView.marcarError(indiceActual)

            tvFeedback.text = "¡Ups! Era $notaObjetivoActual"
            tvFeedback.setTextColor(Color.RED)
        }
    }

    private fun evaluarYOthorgarInsignia() {
        if (juegoTerminado) return

        if (studentId.isEmpty()) {
            Log.e("ERROR_GAME", "El studentId está vacío. No se puede dar insignia.")
            ToastHelper.mostrarMensaje(this, "Error: No se pudo identificar al alumno.")
            return
        }

        juegoTerminado = true
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("students").document(studentId)
        val claveInsignia = "teclasmaestras"
        val nombreInsignia = "Teclas Maestras"

        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val expActual = snapshot.getLong("expTotal") ?: 0L
                val insignias = snapshot.get("insignias_progreso") as? Map<String, Long> ?: emptyMap()
                val nivelActualInsignia = insignias[claveInsignia] ?: 0L

                if (nivelActualInsignia >= 3) {
                    ToastHelper.mostrarMensaje(this, "¡Ya obtuviste la insignia de máximo nivel!")
                    return@addOnSuccessListener
                }

                val nivelMerecido = if (erroresCometidos == 0) 3L else 1L
                val nivelFinal = maxOf(nivelActualInsignia, nivelMerecido)

                if (nivelFinal > nivelActualInsignia) {
                    val xpGanada = if (erroresCometidos == 0) 250L else 80L
                    val nuevaExp = expActual + xpGanada
                    val nuevoNivelGeneral = (nuevaExp / 500).toInt()

                    val actualizaciones = hashMapOf<String, Any>(
                        "expTotal" to nuevaExp,
                        "nivel" to nuevoNivelGeneral,
                        "insignias_progreso.$claveInsignia" to nivelFinal
                    )

                    docRef.update(actualizaciones).addOnSuccessListener {
                        val mensaje = if (nivelFinal == 3L) "¡Ejecución Perfecta! Ganaste: $nombreInsignia Nivel 3 🏅" else "¡Buen intento! Ganaste: $nombreInsignia Nivel 1"
                        ToastHelper.mostrarMensaje(this, mensaje)
                    }
                } else {
                    ToastHelper.mostrarMensaje(this, "¡Terminado! Sigue practicando para subir de nivel.")
                }
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
        if (::dispatcher.isInitialized && !dispatcher.isStopped) dispatcher.stop()
        mediaRecorder?.release()
        timerGrabacion?.cancel()
    }
}