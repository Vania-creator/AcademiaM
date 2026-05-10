package com.example.academiam

import android.content.Intent
import android.os.Bundle
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

    // 🔥 DICCIONARIO PARA TRADUCIR LA CLAVE AL NOMBRE REAL 🔥
    private val nombresInsignias = mapOf(
        "primersonido" to "Primer Sonido",
        "enmarcha" to "En Marcha",
        "dominiobasico" to "Dominio Básico",
        "talentoacenso" to "Talento en Ascenso",
        "teclasmaestras" to "Teclas Maestras",
        "constancia" to "Constancia",
        "diciplinatotal" to "Disciplina Total",
        "tiempoinvertido" to "Tiempo Invertido",
        "conocimientomusical" to "Conocimiento Musical",
        "formacionsolida" to "Formación Sólida",
        "excelencia" to "Excelencia",
        "progresoacelerado" to "Progreso Acelerado",
        "pasionporlamusica" to "Pasión por la Música"
    )

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
            cargarInsigniasYReportes(studentId)
            cargarClasesFijas(studentId)
            cargarUltimaTarea(studentId)
            configurarBotonesHistorial(studentId)
        }
    }

    private fun cargarDatosAlumno(id: String) {
        db.collection("students").document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                findViewById<TextView>(R.id.txtNombrePerfil).text = doc.getString("nombre")
                findViewById<TextView>(R.id.txtTutorPerfil).text = "Tutor: ${doc.getString("nombreTutor") ?: "N/A"}"
                findViewById<TextView>(R.id.txtRachaPerfil).text = "${doc.getLong("racha") ?: 0} Días"

                val exp = doc.getLong("expTotal") ?: 0L
                val nivel = doc.getLong("nivel") ?: 0L
                findViewById<TextView>(R.id.txtNivelExpPerfil).text = "Nvl $nivel • $exp XP"

                val mapaProgreso = doc.get("insignias_progreso") as? Map<String, Long>
                val containerInsignias = findViewById<LinearLayout>(R.id.containerInsignias)
                containerInsignias.removeAllViews()

                if (mapaProgreso != null && mapaProgreso.isNotEmpty()) {
                    for ((clave, nivelAlcanzado) in mapaProgreso) {

                        val img = ImageView(this)
                        val params = LinearLayout.LayoutParams(140, 140)
                        params.setMargins(0, 0, 16, 0)
                        img.layoutParams = params

                        // Busca la imagen PNG (Ej: primersonido1.png, primersonido2.png)
                        val nombreArchivo = "${clave}${nivelAlcanzado}"
                        val imageResId = resources.getIdentifier(nombreArchivo, "drawable", packageName)

                        if (imageResId != 0) {
                            img.setImageResource(imageResId)
                        } else {
                            img.setImageResource(R.drawable.logo)
                        }

                        val nombreLegible = nombresInsignias[clave] ?: clave.uppercase()

                        img.setOnClickListener {
                            // 🔥 Usamos el mensaje personalizado con el logo 🔥
                            mostrarMensajeAcademia("$nombreLegible (Nivel $nivelAlcanzado)")
                        }

                        containerInsignias.addView(img)
                    }
                } else {
                    val txt = TextView(this)
                    txt.text = "Aún no ha obtenido insignias."
                    txt.setTextColor(android.graphics.Color.GRAY)
                    containerInsignias.addView(txt)
                }
            }
        }
    }

    private fun cargarInsigniasYReportes(id: String) {
        db.collection("reports")
            .whereEqualTo("studentId", id)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val ultimoDoc = query.documents[0]
                    findViewById<TextView>(R.id.txtFechaReportePerfil).text = "Clase del: ${ultimoDoc.getString("date")}"
                    findViewById<TextView>(R.id.txtReporteDesc).text = ultimoDoc.getString("content")
                } else {
                    findViewById<TextView>(R.id.txtReporteDesc).text = "Sin reportes registrados"
                }
            }
    }

    private fun cargarClasesFijas(id: String) {
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
                                infoClases.append("• $dia $hora [$inst] con $maestro\n")
                            }
                        }
                    }

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
    }

    private fun cargarUltimaTarea(id: String) {
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
    }

    private fun configurarBotonesHistorial(id: String) {
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

    // 🔥 Función de Mensaje Personalizado con Logo 🔥
    private fun mostrarMensajeAcademia(mensaje: String) {
        val layout = layoutInflater.inflate(R.layout.layout_toast_personalizado, null)
        val txt = layout.findViewById<TextView>(R.id.txtMensajeToast)
        txt.text = mensaje

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 150)
        toast.show()
    }
}