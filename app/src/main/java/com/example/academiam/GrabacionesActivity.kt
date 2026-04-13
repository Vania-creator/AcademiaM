package com.example.academiam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class GrabacionesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout
    private lateinit var txtError: TextView
    private var studentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grabaciones)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        container = findViewById(R.id.containerGrabaciones)
        txtError = findViewById(R.id.txtErrorCarga)

        findViewById<AppCompatButton>(R.id.btnRegresarGrabaciones).setOnClickListener { finish() }

        if (studentId.isNotEmpty()) {
            cargarInfoAlumno()
            cargarGrabaciones()
        } else {
            mostrarError("Error: ID de alumno no recibido")
        }

        findViewById<AppCompatButton>(R.id.btnRecompensa).setOnClickListener {
            Toast.makeText(this, "Función de premios próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarInfoAlumno() {
        db.collection("students").document(studentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    findViewById<TextView>(R.id.txtNombreGr).text = doc.getString("nombre")
                    findViewById<TextView>(R.id.txtInstrumentoGr).text = doc.getString("instrumento") ?: "Instrumento no asignado"
                    findViewById<TextView>(R.id.txtRachaGr).text = "${doc.getLong("racha") ?: 0} Días"
                }
            }
    }

    private fun cargarGrabaciones() {
        // IMPORTANTE: Esta consulta requiere un ÍNDICE en Firebase
        db.collection("recordings")
            .whereEqualTo("studentId", studentId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                container.removeAllViews()
                txtError.visibility = View.GONE

                if (query.isEmpty) {
                    mostrarError("El alumno no tiene grabaciones aún")
                    return@addOnSuccessListener
                }

                for (doc in query) {
                    try {
                        val item = LayoutInflater.from(this).inflate(R.layout.item_grabacion, container, false)

                        val titulo = doc.getString("titulo") ?: "Sin título"
                        val detalles = doc.getString("detalles") ?: ""
                        val fecha = doc.getString("fecha") ?: ""
                        val url = doc.getString("videoUrl") ?: ""

                        item.findViewById<TextView>(R.id.txtTituloGr).text = titulo
                        item.findViewById<TextView>(R.id.txtDetallesGr).text = detalles
                        item.findViewById<TextView>(R.id.txtFechaGr).text = fecha

                        item.findViewById<ImageView>(R.id.btnPlayGr).setOnClickListener {
                            if (url.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "El video no tiene una URL válida", Toast.LENGTH_SHORT).show()
                            }
                        }
                        container.addView(item)
                    } catch (e: Exception) {
                        Log.e("GRABACIONES", "Error al inflar item: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                // Si sale error aquí, revisa el Logcat y busca un link de Firebase
                Log.e("FIRESTORE_ERROR", "Detalle: ${e.message}")
                mostrarError("Error de base de datos. ¿Creaste el índice?")
            }
    }

    private fun mostrarError(msj: String) {
        txtError.text = msj
        txtError.visibility = View.VISIBLE
    }
}