package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PerfilAlumnoActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_alumno)

        val studentId = intent.getStringExtra("STUDENT_ID") ?: ""

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarPerfil)
        val btnGrabaciones = findViewById<AppCompatButton>(R.id.btnGrabaciones)

        btnRegresar.setOnClickListener { finish() }

        if (studentId.isNotEmpty()) {
            cargarDatosAlumno(studentId)
        } else {
            Toast.makeText(this, "Error al cargar el alumno", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatosAlumno(id: String) {
        // 1. Datos básicos del alumno
        db.collection("students").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                findViewById<TextView>(R.id.txtNombrePerfil).text = doc.getString("nombre")
                findViewById<TextView>(R.id.txtTutorPerfil).text = "Tutor: ${doc.getString("nombreTutor") ?: "N/A"}"
                findViewById<TextView>(R.id.txtRachaPerfil).text = "${doc.getLong("racha") ?: 0} Días"
            }
        }

        // 2. REPORTES E INSIGNIAS (Lógica unificada)
        db.collection("reports")
            .whereEqualTo("studentId", id)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    // Mostrar el último reporte (el primero de la lista descendente)
                    val ultimoDoc = query.documents[0]
                    findViewById<TextView>(R.id.txtFechaReportePerfil).text = "Clase del: ${ultimoDoc.getString("date")}"
                    findViewById<TextView>(R.id.txtReporteDesc).text = ultimoDoc.getString("content")

                    // Procesar insignias únicas ganadas
                    val containerInsignias = findViewById<LinearLayout>(R.id.containerInsignias)
                    containerInsignias.removeAllViews()
                    val insigniasGanadas = mutableSetOf<String>()

                    for (doc in query) {
                        val ins = doc.getString("insignia")
                        if (ins != null && ins != "Ninguna" && ins.isNotEmpty()) {
                            insigniasGanadas.add(ins)
                        }
                    }

                    for (nombreInsignia in insigniasGanadas) {
                        val img = ImageView(this)
                        val params = LinearLayout.LayoutParams(110, 110)
                        params.setMargins(8, 0, 8, 0)
                        img.layoutParams = params
                        img.setImageResource(R.drawable.logo)
                        img.setBackgroundResource(R.drawable.circulo_gris)
                        img.setPadding(10, 10, 10, 10)
                        containerInsignias.addView(img)
                    }
                } else {
                    findViewById<TextView>(R.id.txtReporteDesc).text = "Sin reportes registrados"
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Error en reportes: ${e.message}")
            }

        // 3. Configurar botón para ver historial de reportes
        findViewById<TextView>(R.id.btnVerHistorialReportes).setOnClickListener {
            val intent = Intent(this, HistorialReportesActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }

        // 4. CONSULTAR ÚLTIMA TAREA (El bloque que me pasaste)
        db.collection("tasks")
            .whereEqualTo("studentId", id)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (query.isEmpty) {
                    Log.d("TAREAS_DEBUG", "No hay tareas para el alumno: $id")
                    findViewById<TextView>(R.id.txtTareaDesc).text = "Sin tareas pendientes"
                } else {
                    val doc = query.documents[0]
                    val status = doc.getString("status") ?: "Pendiente"

                    findViewById<TextView>(R.id.txtTareaTituloPerfil).text = doc.getString("title")
                    findViewById<TextView>(R.id.txtTareaDesc).text = doc.getString("description")

                    val tvStatus = findViewById<TextView>(R.id.txtStatusTareaPerfil)
                    tvStatus.text = status.uppercase()

                    // Colores visuales según el estado
                    if (status.lowercase() == "hecha") {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                    } else {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFCDD2"))
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TAREAS_ERROR", "Error de Firestore: ${e.message}")
            }

        // 5. Configurar botón para ver todas las tareas
        findViewById<TextView>(R.id.btnVerTodasTareas).setOnClickListener {
            val intent = Intent(this, HistorialTareasActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }

        // 6. Consultar Última Tarea Asignada
        db.collection("tasks")
            .whereEqualTo("studentId", id)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val tarea = query.documents[0]
                    val status = tarea.getString("status") ?: "Pendiente"

                    findViewById<TextView>(R.id.txtTareaTituloPerfil).text = tarea.getString("title")
                    findViewById<TextView>(R.id.txtTareaDesc).text = tarea.getString("description")

                    val tvStatus = findViewById<TextView>(R.id.txtStatusTareaPerfil)
                    tvStatus.text = status.uppercase()

                    // Cambiar color según el estado
                    if (status.lowercase() == "hecha") {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                    } else {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFCDD2"))
                    }
                }
            }

// 7. Configurar botón para ver todas las tareas
        findViewById<TextView>(R.id.btnVerTodasTareas).setOnClickListener {
            val intent = Intent(this, HistorialTareasActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }
    }
}