package com.example.academiam

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TareaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""
    private var studentIdSeleccionado: String = ""

    // 🔥 Variables para la recompensa de la tarea
    private var insigniaSeleccionada: String = "Ninguna"
    private var claveInsigniaSeleccionada: String = "ninguna"

    private val listaAlumnosFiltrados = mutableListOf<Map<String, Any>>()

    private lateinit var btnFecha: TextView
    private lateinit var btnAlumno: TextView
    private lateinit var txtError: TextView

    // 🔥 Las 4 Insignias ideales para recompensar Tareas en Casa
    private val insigniasTarea = mapOf(
        "Tiempo Invertido" to Pair("tiempoinvertido", listOf(160L, 360L, 480L)),
        "En Marcha" to Pair("enmarcha", listOf(80L, 160L, 240L)),
        "Teclas Maestras" to Pair("teclasmaestras", listOf(120L, 240L, 360L)),
        "Formación Sólida" to Pair("formacionsolida", listOf(180L, 360L, 450L))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarea)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        if (teacherId.isEmpty()) {
            ToastHelper.mostrarMensaje(this, "Error: No se detectó al maestro")
            finish()
        }

        btnFecha = findViewById(R.id.btnSeleccionarFechaTarea)
        btnAlumno = findViewById(R.id.btnSeleccionarAlumnoTarea)
        txtError = findViewById(R.id.txtErrorTarea)
        val etTitulo = findViewById<EditText>(R.id.etTitulo)
        val etNotas = findViewById<EditText>(R.id.etNotasTarea)

        btnFecha.setOnClickListener { mostrarDatePicker() }
        btnAlumno.setOnClickListener {
            if (listaAlumnosFiltrados.isNotEmpty()) mostrarSelectorAlumnos()
            else ToastHelper.mostrarMensaje(this, "Elige una fecha con clases primero")
        }

        configurarIconosMotivacion()

        findViewById<AppCompatButton>(R.id.btnRegresarTarea).setOnClickListener { finish() }
        findViewById<AppCompatButton>(R.id.btnIngresarTarea).setOnClickListener {
            guardarTarea(etTitulo.text.toString(), etNotas.text.toString())
        }
    }

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val fechaFormateada = "%04d-%02d-%02d".format(y, m + 1, d)
            btnFecha.text = fechaFormateada
            buscarMisAlumnosDelDia(y, m, d, fechaFormateada)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun buscarMisAlumnosDelDia(y: Int, m: Int, d: Int, fechaComp: String) {
        val calendar = Calendar.getInstance()
        calendar.set(y, m, d)
        val sdf = SimpleDateFormat("EEE", Locale("es", "MX"))
        var dia = sdf.format(calendar.time).lowercase()

        dia = when {
            dia.contains("lu") -> "Lun"
            dia.contains("ma") -> "Mar"
            dia.contains("mi") -> "Mie"
            dia.contains("ju") -> "Jue"
            dia.contains("vi") -> "Vie"
            dia.contains("sá") || dia.contains("sa") -> "Sab"
            else -> "Dom"
        }

        listaAlumnosFiltrados.clear()
        val idsAgregados = mutableSetOf<String>()

        btnAlumno.text = "Buscando tus alumnos..."

        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { query ->
                for (doc in query) {
                    val dbDay = doc.getString("dayOfWeek") ?: ""
                    val dbDate = doc.getString("date") ?: ""
                    val sId = doc.getString("studentId") ?: ""
                    val hora = doc.getString("time") ?: ""
                    val tipo = doc.getString("type") ?: ""

                    if ((tipo == "fija" && dbDay == dia) || dbDate == fechaComp) {
                        val llave = "$sId-$hora"
                        if (!idsAgregados.contains(llave)) {
                            listaAlumnosFiltrados.add(doc.data)
                            idsAgregados.add(llave)
                        }
                    }
                }

                if (listaAlumnosFiltrados.isEmpty()) {
                    btnAlumno.text = "Sin alumnos este día"
                    btnAlumno.isEnabled = false
                } else {
                    btnAlumno.text = "Toca para elegir alumno"
                    btnAlumno.isEnabled = true
                }
            }
    }

    private fun mostrarSelectorAlumnos() {
        val opciones = listaAlumnosFiltrados.map {
            "${it["studentName"]} - ${it["time"]} [${it["instrument"]}]"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("¿A quién asignarás tarea?")
            .setItems(opciones) { _, i ->
                val sel = listaAlumnosFiltrados[i]
                studentIdSeleccionado = sel["studentId"].toString()
                btnAlumno.text = sel["studentName"].toString()
            }.show()
    }

    // 🔥 CARGA DINÁMICA DE LAS 4 INSIGNIAS DE TAREA
    private fun configurarIconosMotivacion() {
        val icons = listOf<ImageView>(
            findViewById(R.id.icono1), findViewById(R.id.icono2),
            findViewById(R.id.icono3), findViewById(R.id.icono4)
        )

        val nombresInsignias = insigniasTarea.keys.toList()

        icons.forEachIndexed { i, img ->
            val nombreReal = nombresInsignias[i]
            val clave = insigniasTarea[nombreReal]?.first ?: ""

            // Construimos el nombre del PNG (Nivel 1 por defecto en la interfaz)
            val nombreArchivo = "${clave}1"
            val imageResId = resources.getIdentifier(nombreArchivo, "drawable", packageName)

            if (imageResId != 0) {
                img.setImageResource(imageResId)
            }

            img.setOnClickListener {
                if (studentIdSeleccionado.isEmpty()) {
                    ToastHelper.mostrarMensaje(this, "Selecciona primero un alumno")
                    return@setOnClickListener
                }

                icons.forEach { it.alpha = 0.3f }
                img.alpha = 1.0f

                insigniaSeleccionada = nombreReal
                claveInsigniaSeleccionada = clave

                ToastHelper.mostrarMensaje(this, "Recompensa: $nombreReal")
            }
        }
    }

    private fun guardarTarea(titulo: String, notas: String) {
        if (studentIdSeleccionado.isEmpty() || titulo.isEmpty() || notas.isEmpty()) {
            txtError.visibility = View.VISIBLE
            return
        }

        findViewById<AppCompatButton>(R.id.btnIngresarTarea).isEnabled = false

        // 🔥 Guardamos la promesa de la insignia en la base de datos
        val tarea = hashMapOf(
            "studentId" to studentIdSeleccionado,
            "studentName" to btnAlumno.text.toString(),
            "teacherId" to teacherId,
            "title" to titulo,
            "description" to notas,
            "motivationInsigniaName" to insigniaSeleccionada,     // Ej: "Tiempo Invertido"
            "motivationInsigniaKey" to claveInsigniaSeleccionada, // Ej: "tiempoinvertido"
            "dateAssigned" to btnFecha.text.toString(),
            "status" to "Pendiente",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("tasks").add(tarea).addOnSuccessListener {
            ToastHelper.mostrarMensaje(this, "¡Tarea asignada con éxito!")

            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentIdSeleccionado)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            findViewById<AppCompatButton>(R.id.btnIngresarTarea).isEnabled = true
            ToastHelper.mostrarMensaje(this, "Error al guardar tarea")
        }
    }
}