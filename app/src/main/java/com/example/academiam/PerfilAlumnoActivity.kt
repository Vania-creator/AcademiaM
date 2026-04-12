package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        // 1. Datos personales del alumno (Colección: students)
        db.collection("students").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                findViewById<TextView>(R.id.txtNombrePerfil).text = doc.getString("nombre")
                findViewById<TextView>(R.id.txtTutorPerfil).text = "Tutor: ${doc.getString("nombreTutor") ?: "N/A"}"
                findViewById<TextView>(R.id.txtRachaPerfil).text = "${doc.getLong("racha") ?: 0} Días"

            }
        }

// 2. BUSCAR TODOS LOS HORARIOS FIJOS (Filtrando repetidos)
        val containerHorarios = findViewById<LinearLayout>(R.id.containerHorarios)

        db.collection("classes")
            .whereEqualTo("studentId", id)
            .whereEqualTo("type", "fija")
            .get()
            .addOnSuccessListener { query ->
                containerHorarios.removeAllViews()

                if (!query.isEmpty) {
                    // Usamos un Set para guardar las combinaciones únicas
                    val horariosYaMostrados = mutableSetOf<String>()

                    for (clase in query) {
                        val dia = clase.getString("dayOfWeek") ?: ""
                        val hora = clase.getString("time") ?: ""
                        val instrumento = clase.getString("instrument") ?: ""
                        val maestro = clase.getString("teacherName") ?: "Sin asignar"

                        // Creamos una "llave" única para comparar
                        val llaveUnica = "$instrumento-$dia-$hora-$maestro".lowercase().trim()

                        // Si esta combinación NO ha sido mostrada, la agregamos
                        if (!horariosYaMostrados.contains(llaveUnica)) {
                            horariosYaMostrados.add(llaveUnica)

                            val tvHorario = TextView(this)
                            tvHorario.text = "• $instrumento: $dia $hora con $maestro"
                            tvHorario.setTextColor(android.graphics.Color.parseColor("#555555"))
                            tvHorario.textSize = 14f
                            tvHorario.setPadding(0, 4, 0, 4)

                            containerHorarios.addView(tvHorario)

                        }
                    }
                } else {
                    val tvVacio = TextView(this)
                    tvVacio.text = "Sin clases fijas asignadas"
                    containerHorarios.addView(tvVacio)
                }
            }

        // 3. Tarea más reciente (Colección: tasks)
        db.collection("tasks")
            .whereEqualTo("studentId", id)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val tarea = query.documents[0]
                    findViewById<TextView>(R.id.txtTareaDesc).text = tarea.getString("description")
                    findViewById<TextView>(R.id.txtTareaAsignadaPor).text = "Asignada por: ${tarea.getString("teacherName")}"
                }
            }

        // 4. Último reporte (Colección: reports)
        db.collection("reports")
            .whereEqualTo("studentId", id)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val reporte = query.documents[0]
                    findViewById<TextView>(R.id.txtReporteDesc).text = reporte.getString("content")
                }
            }
    }
}