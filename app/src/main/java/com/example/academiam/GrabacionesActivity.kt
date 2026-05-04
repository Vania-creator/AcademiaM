package com.example.academiam

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GrabacionesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private lateinit var txtError: TextView
    private var studentId: String = ""

    // Reproductor interno de audio
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grabaciones)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        container = findViewById(R.id.containerGrabaciones)
        txtError = findViewById(R.id.txtErrorCarga)

        findViewById<AppCompatButton>(R.id.btnRegresarGrabaciones).setOnClickListener { finish() }

        if (studentId.isNotEmpty()) {
            cargarInfoAlumno()
            cargarGrabacionesLocales() // 🔥 Cambiamos a lectura local
        } else {
            mostrarError("Error: ID de alumno no recibido")
        }

        findViewById<AppCompatButton>(R.id.btnRecompensa).setOnClickListener {
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cargarInfoAlumno() {
        // Esta parte sigue usando Firebase para ver el perfil del alumno
        db.collection("students").document(studentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    findViewById<TextView>(R.id.txtNombreGr).text = doc.getString("nombre")
                    findViewById<TextView>(R.id.txtInstrumentoGr).text = doc.getString("instrumento") ?: "Instrumento no asignado"
                    findViewById<TextView>(R.id.txtRachaGr).text = "${doc.getLong("racha") ?: 0} Días"
                }
            }
    }

    // 🔥 NUEVA FUNCIÓN: Lee los archivos locales en lugar de buscar en Firebase
    private fun cargarGrabacionesLocales() {
        container.removeAllViews()
        txtError.visibility = View.GONE

        // 1. Buscamos la carpeta donde guardamos las prácticas
        val directorio = getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        if (directorio == null || !directorio.exists()) {
            mostrarError("No se encontró la carpeta de audios.")
            return
        }

        // 2. Obtenemos todos los archivos .3gp y los ordenamos del más nuevo al más viejo
        val archivos = directorio.listFiles { file -> file.extension == "3gp" }

        if (archivos == null || archivos.isEmpty()) {
            mostrarError("El alumno no tiene grabaciones guardadas en este dispositivo.")
            return
        }

        archivos.sortByDescending { it.lastModified() }

        // 3. Pintamos cada archivo en la pantalla
        val formatoFecha = SimpleDateFormat("dd/MMM/yyyy hh:mm a", Locale.getDefault())

        for (archivo in archivos) {
            try {
                val item = LayoutInflater.from(this).inflate(R.layout.item_grabacion, container, false)

                val fechaLegible = formatoFecha.format(Date(archivo.lastModified()))

                item.findViewById<TextView>(R.id.txtTituloGr).text = "Práctica Reciente"
                item.findViewById<TextView>(R.id.txtDetallesGr).text = "Archivo: ${archivo.name}"
                item.findViewById<TextView>(R.id.txtFechaGr).text = fechaLegible

                // Botón Play
                item.findViewById<ImageView>(R.id.btnPlayGr).setOnClickListener {
                    reproducirAudio(archivo.absolutePath)
                }

                // 🔥 BOTÓN BORRAR
                item.findViewById<ImageView>(R.id.btnBorrarGr).setOnClickListener {
                    confirmarEliminacion(archivo)
                }

                container.addView(item)

            } catch (e: Exception) {
                Log.e("GRABACIONES", "Error al inflar item: ${e.message}")
            }
        }
    }

    private fun reproducirAudio(ruta: String) {
        try {
            // Si ya hay un audio sonando, lo detenemos
            mediaPlayer?.stop()
            mediaPlayer?.release()

            // Reproducimos el nuevo audio
            mediaPlayer = MediaPlayer().apply {
                setDataSource(ruta)
                prepare()
                start()
            }
            Toast.makeText(this, "Reproduciendo práctica...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al reproducir el archivo", Toast.LENGTH_SHORT).show()
        }
    }
    private fun confirmarEliminacion(archivo: File) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("¿Borrar grabación?")
        builder.setMessage("Esta acción eliminará el audio permanentemente de tu celular.")

        builder.setPositiveButton("Borrar") { _, _ ->
            borrarArchivo(archivo)
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun borrarArchivo(archivo: File) {
        try {
            if (archivo.exists()) {
                val eliminado = archivo.delete() // 🔥 Borra el archivo físico
                if (eliminado) {
                    Toast.makeText(this, "Grabación eliminada", Toast.LENGTH_SHORT).show()
                    cargarGrabacionesLocales() // Recargamos la lista para que desaparezca el item
                } else {
                    Toast.makeText(this, "No se pudo borrar el archivo", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun mostrarError(msj: String) {
        txtError.text = msj
        txtError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberamos el reproductor al salir de la pantalla
        mediaPlayer?.release()
        mediaPlayer = null
    }
}