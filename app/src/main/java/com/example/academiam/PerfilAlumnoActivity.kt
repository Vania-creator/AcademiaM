package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        findViewById<AppCompatButton>(R.id.btnRegresarPerfil).setOnClickListener { finish() }

        findViewById<AppCompatButton>(R.id.btnGrabaciones).setOnClickListener {
            if (studentId.isNotEmpty()) {
                val intent = Intent(this, GrabacionesActivity::class.java)
                intent.putExtra("STUDENT_ID", studentId)
                startActivity(intent)
            }
        }

        if (studentId.isNotEmpty()) {
            cargarDatosAlumno(studentId)
        }
    }

    private fun cargarDatosAlumno(id: String) {
        // 1. DATOS BÁSICOS
        db.collection("students").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                findViewById<TextView>(R.id.txtNombrePerfil).text = doc.getString("nombre")
                findViewById<TextView>(R.id.txtTutorPerfil).text = "Tutor: ${doc.getString("nombreTutor") ?: "N/A"}"
                findViewById<TextView>(R.id.txtRachaPerfil).text = "${doc.getLong("racha") ?: 0} Días"
            }
        }

// --- PARTE 2: INFORMACIÓN DE CLASES FIJAS (Filtrado Estricto 🛡️) ---
        db.collection("classes")
            .whereEqualTo("studentId", id)
            .get()
            .addOnSuccessListener { query ->
                val txtInstrumento = findViewById<TextView>(R.id.txtInstrumentoPerfil)
                val txtHorario = findViewById<TextView>(R.id.txtHorarioPerfil)

                if (!query.isEmpty) {
                    val infoClases = StringBuilder()
                    val instrumentosUnicos = mutableSetOf<String>()
                    val horariosVistos = mutableSetOf<String>()

                    for (doc in query) {
                        // Filtramos por tipo: Solo las que son "fija"
                        val tipo = doc.getString("type") ?: doc.getString("tipo") ?: ""

                        if (tipo == "fija") {
                            val dia = doc.getString("dayOfWeek") ?: ""
                            val hora = doc.getString("time") ?: ""
                            val inst = doc.getString("instrument") ?: ""
                            val maestro = doc.getString("teacherName") ?: "Profe"

                            val llaveUnica = "$dia-$hora"

                            if (!horariosVistos.contains(llaveUnica)) {
                                horariosVistos.add(llaveUnica)
                                instrumentosUnicos.add(inst)
                                // Formato compacto: "• Lun 4:00pm [Piano] con César"
                                infoClases.append("• $dia $hora [$inst] con $maestro\n")
                            }
                        }
                    }

                    // Si después de filtrar no quedaron fijas
                    if (horariosVistos.isEmpty()) {
                        txtInstrumento.text = "Sin asignar"
                        txtHorario.text = "No tiene clases fijas este semestre"
                    } else {
                        txtInstrumento.text = instrumentosUnicos.joinToString(" + ")
                        txtHorario.text = infoClases.toString().trim()
                    }
                } else {
                    txtInstrumento.text = "Sin asignar"
                    txtHorario.text = "Sin clases registradas"
                }
            }
// 3. REPORTES E INSIGNIAS (ÚNICAS CON IMÁGENES Y NOMBRES)
        db.collection("reports")
            .whereEqualTo("studentId", id)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val ultimoDoc = query.documents[0]
                    findViewById<TextView>(R.id.txtFechaReportePerfil).text = "Clase del: ${ultimoDoc.getString("date")}"
                    findViewById<TextView>(R.id.txtReporteDesc).text = ultimoDoc.getString("content")

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
                        // 1. Creamos el contenedor para la imagen
                        val img = ImageView(this)
                        val params = LinearLayout.LayoutParams(110, 110)
                        params.setMargins(8, 0, 8, 0)
                        img.layoutParams = params

                        // 2. Lógica para asignar una imagen diferente según el nombre
                        val iconoAsignado = when (nombreInsignia) {
                            "Súper Práctica" -> android.R.drawable.btn_star_big_on // Estrella brillante
                            "Oído de Oro" -> android.R.drawable.ic_media_play // Un play (música)
                            "Ritmo Perfecto" -> android.R.drawable.ic_menu_compass // Brújula (precisión)
                            "Gran Avance" -> android.R.drawable.ic_menu_upload // Flecha arriba
                            "As de Escalas" -> android.R.drawable.ic_menu_sort_by_size // Escalones
                            "Creatividad" -> android.R.drawable.ic_menu_gallery // Galería/Arte
                            "Puntualidad" -> android.R.drawable.ic_menu_recent_history // Relojito
                            "Dedos Rápidos" -> android.R.drawable.ic_menu_send // Avión de papel (rapidez)
                            "Teoría Master" -> android.R.drawable.ic_menu_agenda // Libro/Agenda
                            else -> R.drawable.logo // Por defecto tu logo
                        }

                        img.setImageResource(iconoAsignado)
                        img.setBackgroundResource(R.drawable.circulo_gris)
                        img.setPadding(15, 15, 15, 15)

                        // 3. Agregamos el "Click" para que diga su nombre
                        img.setOnClickListener {
                            Toast.makeText(this, "Insignia: $nombreInsignia", Toast.LENGTH_SHORT).show()
                        }

                        // 4. Lo añadimos a la pantalla
                        containerInsignias.addView(img)
                    }
                } else {
                    findViewById<TextView>(R.id.txtReporteDesc).text = "Sin reportes registrados"
                }
            }

        // 4. ÚLTIMA TAREA ASIGNADA
        db.collection("tasks")
            .whereEqualTo("studentId", id)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                val tvTitulo = findViewById<TextView>(R.id.txtTareaTituloPerfil)
                val tvDesc = findViewById<TextView>(R.id.txtTareaDesc)
                val tvStatus = findViewById<TextView>(R.id.txtStatusTareaPerfil)

                if (query.isEmpty) {
                    tvTitulo.text = "No hay tareas"
                    tvDesc.text = "El maestro no ha asignado tareas aún."
                    tvStatus.visibility = android.view.View.GONE
                } else {
                    val doc = query.documents[0]
                    val status = doc.getString("status") ?: "Pendiente"
                    tvStatus.visibility = android.view.View.VISIBLE
                    tvTitulo.text = doc.getString("title")
                    tvDesc.text = doc.getString("description")
                    tvStatus.text = status.uppercase()

                    if (status.lowercase() == "hecha") {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                    } else {
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFCDD2"))
                    }
                }
            }

        // 5. BOTONES DE HISTORIAL
        findViewById<TextView>(R.id.btnVerHistorialReportes).setOnClickListener {
            val intent = Intent(this, HistorialReportesActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnVerTodasTareas).setOnClickListener {
            val intent = Intent(this, HistorialTareasActivity::class.java)
            intent.putExtra("STUDENT_ID", id)
            intent.putExtra("STUDENT_NAME", findViewById<TextView>(R.id.txtNombrePerfil).text.toString())
            startActivity(intent)
        }
    }
}