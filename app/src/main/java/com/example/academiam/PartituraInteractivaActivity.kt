package com.example.academiam

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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
import java.io.File
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
    private lateinit var ivPartitura: ImageView
    // Cambia o añade estas variables
    private var ultimoAciertoMs: Long = 0
    private val COOLDOWN_NOTAS_MS = 250 // Tiempo mínimo entre notas (en milisegundos)

    private val CODIGO_PERMISO_MIC = 1001

    // Lógica del juego
    private var melodia = ArrayList<String>()
    private var indiceActual = 0
    private var ultimaNotaEscuchada = ""
    private var estaPreparado = false

    // Lógica de grabación local
    private var mediaRecorder: MediaRecorder? = null
    private var rutaAudio: String = ""
    private var timerGrabacion: CountDownTimer? = null

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
        ivPartitura = findViewById(R.id.ivPartitura)

        // 1. Recibir Título
        val titulo = intent.getStringExtra("TITULO_CANCION") ?: "Práctica Libre"
        findViewById<TextView>(R.id.tvTituloCancion).text = titulo

        // 2. Recibir Imagen Dinámica
        val imagenResId = intent.getIntExtra("IMAGEN_PARTITURA", R.drawable.logo) // Cambia esto si tienes un logo por defecto
        ivPartitura.setImageResource(imagenResId)

        // 🔥 FUNCIONES DE LOS BOTONES
        btnAtras.setOnClickListener {
            finish()
        }

        btnGrabar.setOnClickListener {
            if (mediaRecorder == null) {
                iniciarGrabacion()
            } else {
                detenerYGuardarAudio()
            }
        }

        // 3. Recibir Secuencia
        val notasArray = intent.getStringArrayListExtra("SECUENCIA_NOTAS")
        if (notasArray != null && notasArray.isNotEmpty()) {
            melodia = ArrayList(notasArray)
            tvSecuenciaNotas.text = "Secuencia: ${melodia.joinToString(" - ")}"

            moverBarraVisual()
            iniciarCuentaRegresiva()
        } else {
            Toast.makeText(this, "Error: Melodía vacía", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // --- LÓGICA DE GRABACIÓN LOCAL (SIN FIREBASE) ---

    private fun iniciarGrabacion() {
        // 🔥 Creamos una carpeta segura en el almacenamiento del teléfono
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
                    override fun onFinish() {
                        detenerYGuardarAudio()
                    }
                }.start()

            } catch (e: Exception) {
                Toast.makeText(this@PartituraInteractivaActivity, "Error al iniciar grabadora: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun detenerYGuardarAudio() {
        try {
            // Apagamos la grabadora
            timerGrabacion?.cancel()
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            // 🔥 Avisamos que se guardó localmente
            Toast.makeText(this, "¡Audio guardado exitosamente!", Toast.LENGTH_LONG).show()

            // Restauramos el botón
            btnGrabar.text = "🔴 Grabar Nueva Práctica"
            btnGrabar.isEnabled = true
            btnGrabar.setBackgroundColor(Color.parseColor("#D32F2F"))

        } catch(e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el audio", Toast.LENGTH_SHORT).show()
        }
    }

    // --- LÓGICA DE PARTITURA INTERACTIVA ---

    private fun iniciarCuentaRegresiva() {
        tvFeedback.text = "¡Prepárate!"
        tvFeedback.setTextColor(Color.parseColor("#FF9800"))

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

    // --- ANIMACIÓN DE LA BARRA (ACTUALIZADA) ---
    private fun moverBarraVisual() {
        framePartitura.post {
            val anchoTotal = framePartitura.width.toFloat()

            // 1. Calcular el espacio muerto (Clave de Sol) y el espacio útil
            // 0.18f = Ignora el primer 18% de la imagen (ajusta este número si necesitas que empiece más a la derecha o izquierda)
            val margenInicial = anchoTotal * 0.18f
            // 0.05f = Deja un 5% de margen al final para que la última nota no quede pegada al marco
            val margenFinal = anchoTotal * 0.05f

            val espacioParaNotas = anchoTotal - margenInicial - margenFinal

            // 2. Calcular cuánto avanza por cada nota
            val tamañoPaso = if (melodia.size > 1) {
                espacioParaNotas / (melodia.size - 1)
            } else {
                0f
            }

            // 3. Asegurar que al terminar la canción la barra se quede en la última nota
            val indiceSeguro = if (indiceActual >= melodia.size) melodia.size - 1 else indiceActual

            // 4. Mover la barra a la posición exacta
            val nuevaPosicionX = margenInicial + (tamañoPaso * indiceSeguro)

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
                ToastHelper.mostrarMensaje(this, "Permiso de micrófono denegado.")
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
                ToastHelper.mostrarMensaje(this, "Error al iniciar micrófono: ${e.message}")
            }
        }
    }

    private fun procesarJugada(notaDetectada: String) {
        if (!estaPreparado) return

        val tiempoActual = System.currentTimeMillis()
        tvNotaEscuchada.text = "Escuchando: $notaDetectada"

        if (indiceActual >= melodia.size) return

        // 1. Filtro de tiempo: Si intentas tocar antes de que pasen 250ms, el sistema ignora el input.
        // Esto evita que una sola pulsación larga se registre como 10 aciertos.
        if (tiempoActual - ultimoAciertoMs < COOLDOWN_NOTAS_MS) return

        val notaObjetivoActual = melodia[indiceActual]

        // 2. Verificación de la nota
        if (notaDetectada.startsWith(notaObjetivoActual)) {
            // Guardamos el momento exacto del acierto para habilitar el siguiente disparo
            ultimoAciertoMs = tiempoActual

            indiceActual++
            moverBarraVisual()

            if (indiceActual == melodia.size) {
                tvFeedback.text = "¡COMPLETADO! 🎉"
                tvFeedback.setTextColor(Color.parseColor("#4CAF50"))
                tvSecuenciaNotas.text = "¡Bien hecho!"
                tvNotaEscuchada.text = "Has terminado la secuencia."

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
        // Liberar motor de audio
        if (::dispatcher.isInitialized && !dispatcher.isStopped) {
            dispatcher.stop()
        }
        // Liberar grabadora si se salió sin detenerla
        mediaRecorder?.release()
        timerGrabacion?.cancel()
    }
}