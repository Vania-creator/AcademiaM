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

        // 2. BUSCAR REPORTES (Aquí se generan las insignias y el último reporte)
        db.collection("reports")
            .whereEqualTo("studentId", id)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    // MOSTRAR ÚLTIMO REPORTE
                    val ultimoDoc = query.documents[0]
                    findViewById<TextView>(R.id.txtFechaReportePerfil).text = "Clase del: ${ultimoDoc.getString("date")}"
                    findViewById<TextView>(R.id.txtReporteDesc).text = ultimoDoc.getString("content")

                    // PROCESAR INSIGNIAS ÚNICAS
                    val containerInsignias = findViewById<LinearLayout>(R.id.containerInsignias)
                    containerInsignias.removeAllViews()

                    val insigniasUnicas = mutableSetOf<String>()
                    for (doc in query) {
                        val ins = doc.getString("insignia")
                        if (ins != null && ins != "Ninguna") {
                            insigniasUnicas.add(ins)
                        }
                    }

                    for (nombre in insigniasUnicas) {
                        val img = ImageView(this)
                        val params = LinearLayout.LayoutParams(120, 120)
                        params.setMargins(10, 0, 10, 0)
                        img.layoutParams = params
                        img.setImageResource(R.drawable.logo) // Aquí va tu icono de medalla
                        img.setBackgroundResource(R.drawable.circulo_gris)
                        img.setPadding(15, 15, 15, 15)
                        containerInsignias.addView(img)
                    }
                } else {
                    findViewById<TextView>(R.id.txtReporteDesc).text = "Sin reportes aún"
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ERROR", "Si ves un link aquí, hazle clic: ${e.message}")
            }

        // 3. Configurar botón de historial
        findViewById<TextView>(R.id.btnVerHistorialReportes).setOnClickListener {
            val intent = Intent(this, HistorialReportesActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }


// 4. Consultar Reportes para sacar la Racha, el Último Reporte y las Insignias
        db.collection("reports")
            .whereEqualTo("studentId", id)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    // MOSTRAR ÚLTIMO REPORTE (El primero de la lista por el DESCENDING)
                    val ultimoDoc = query.documents[0]
                    findViewById<TextView>(R.id.txtFechaReportePerfil).text = "Clase del: ${ultimoDoc.getString("date")}"
                    findViewById<TextView>(R.id.txtReporteDesc).text = ultimoDoc.getString("content")

                    // MOSTRAR INSIGNIAS ÚNICAS
                    val containerInsignias = findViewById<LinearLayout>(R.id.containerInsignias)
                    containerInsignias.removeAllViews()

                    val insigniasGanadas = mutableSetOf<String>()

                    for (doc in query) {
                        val insignia = doc.getString("insignia")
                        if (insignia != null && insignia != "Ninguna" && insignia.isNotEmpty()) {
                            insigniasGanadas.add(insignia)
                        }
                    }

                    // Dibujar las insignias que encontró
                    for (nombreInsignia in insigniasGanadas) {
                        val img = ImageView(this)
                        img.layoutParams = LinearLayout.LayoutParams(110, 110).apply { setMargins(8,0,8,0) }
                        img.setImageResource(R.drawable.logo) // Aquí usarías el icono real
                        img.setBackgroundResource(R.drawable.circulo_gris)
                        img.setPadding(10,10,10,10)
                        containerInsignias.addView(img)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Error: ${e.message}")
                // Si aquí sale error de index, abre el link del Logcat
            }

// 5. Configurar botón para ver todo el historial
        findViewById<TextView>(R.id.btnVerHistorialReportes).setOnClickListener {
            val intent = Intent(this, HistorialReportesActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }
    }
}