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
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grabaciones)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        container = findViewById(R.id.containerGrabaciones)
        txtError = findViewById(R.id.txtErrorCarga)

        findViewById<AppCompatButton>(R.id.btnRegresarGrabaciones).setOnClickListener { finish() }

        if (studentId.isNotEmpty()) {
            cargarInfoAlumno()
            cargarGrabacionesLocales()
        } else {
            mostrarError("Error: ID de alumno no recibido")
        }

        findViewById<AppCompatButton>(R.id.btnRecompensa).setOnClickListener {
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentId)
            startActivity(intent)
        }
    }

    private fun cargarInfoAlumno() {
        db.collection("students").document(studentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    findViewById<TextView>(R.id.txtNombreGr).text = doc.getString("nombre")
                    findViewById<TextView>(R.id.txtRachaGr).text = "${doc.getLong("racha") ?: 0} Días"

                    // 🔥 CARGAR IMAGEN DEL ALUMNO
                    val imgAlumno = findViewById<ImageView>(R.id.imgAlumnoGr)
                    val avatarName = doc.getString("avatar") ?: "logo"
                    val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                    if (resId != 0) imgAlumno.setImageResource(resId) else imgAlumno.setImageResource(R.drawable.logo)
                }
            }
    }

    private fun cargarGrabacionesLocales() {
        container.removeAllViews()
        txtError.visibility = View.GONE

        val directorio = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (directorio == null || !directorio.exists()) {
            mostrarError("No se encontró la carpeta de audios.")
            return
        }

        val archivos = directorio.listFiles { file -> file.extension == "3gp" }
        if (archivos == null || archivos.isEmpty()) {
            mostrarError("El alumno no tiene grabaciones guardadas.")
            return
        }

        archivos.sortByDescending { it.lastModified() }
        val formatoFecha = SimpleDateFormat("dd/MMM/yyyy hh:mm a", Locale.getDefault())

        for (archivo in archivos) {
            try {
                val item = LayoutInflater.from(this).inflate(R.layout.item_grabacion, container, false)
                val fechaLegible = formatoFecha.format(Date(archivo.lastModified()))

                item.findViewById<TextView>(R.id.txtTituloGr).text = "Práctica Reciente"
                item.findViewById<TextView>(R.id.txtDetallesGr).text = "Archivo: ${archivo.name}"
                item.findViewById<TextView>(R.id.txtFechaGr).text = fechaLegible

                // 🔥 PONER ICONO DE ACTIVIDAD (Usamos una partitura o icono musical)
                val imgActividad = item.findViewById<ImageView>(R.id.imgActividadGr)
                imgActividad.setImageResource(android.R.drawable.ic_btn_speak_now) // Puedes cambiarlo por R.drawable.tu_icono_musical

                item.findViewById<ImageView>(R.id.btnPlayGr).setOnClickListener {
                    reproducirAudio(archivo.absolutePath)
                }

                item.findViewById<ImageView>(R.id.btnBorrarGr).setOnClickListener {
                    confirmarEliminacion(archivo)
                }

                container.addView(item)
            } catch (e: Exception) {
                Log.e("GRABACIONES", "Error: ${e.message}")
            }
        }
    }

    private fun reproducirAudio(ruta: String) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(ruta)
                prepare()
                start()
            }
            Toast.makeText(this, "Reproduciendo...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al reproducir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminacion(archivo: File) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Borrar?")
            .setMessage("Se eliminará permanentemente.")
            .setPositiveButton("Borrar") { _, _ -> borrarArchivo(archivo) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun borrarArchivo(archivo: File) {
        if (archivo.exists() && archivo.delete()) {
            ToastHelper.mostrarMensaje(this, "Eliminada")
            cargarGrabacionesLocales()
        }
    }

    private fun mostrarError(msj: String) {
        txtError.text = msj
        txtError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}