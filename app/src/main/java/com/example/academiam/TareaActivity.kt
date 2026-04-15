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
    private var insigniaMotivacional: String = "Ninguna"

    // Lista para guardar alumnos únicos
    private val listaAlumnosFiltrados = mutableListOf<Map<String, Any>>()

    private lateinit var btnFecha: TextView
    private lateinit var btnAlumno: TextView
    private lateinit var txtError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarea)

        // RECUPERAR ID DEL MAESTRO
        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        // Log para que tú veas en el Logcat si el ID está llegando
        Log.d("DEBUG_TAREA", "Teacher ID recibido: $teacherId")

        if (teacherId.isEmpty()) {
            Toast.makeText(this, "Error: No se detectó al maestro", Toast.LENGTH_SHORT).show()
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
            else Toast.makeText(this, "Elige una fecha con clases primero", Toast.LENGTH_SHORT).show()
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

        // Normalizar día para que coincida con la DB
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
        val idsAgregados = mutableSetOf<String>() // PARA EVITAR REPETIDOS

        btnAlumno.text = "Buscando tus alumnos..."

        // FILTRO ESTRICTO: Solo donde teacherId sea igual al mío
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

                    // Verificamos si es clase fija de hoy O evento especial de hoy
                    if ((tipo == "fija" && dbDay == dia) || dbDate == fechaComp) {

                        // LLAVE ÚNICA: Alumno + Hora (Para no repetir si tiene 2 registros)
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

    private fun configurarIconosMotivacion() {
        val icons = listOf<ImageView>(findViewById(R.id.icono1), findViewById(R.id.icono2), findViewById(R.id.icono3), findViewById(R.id.icono4))
        icons.forEach { it.alpha = 0.3f }
        icons.forEachIndexed { i, img ->
            img.setOnClickListener {
                icons.forEach { it.alpha = 0.3f }
                img.alpha = 1.0f
                insigniaMotivacional = "Insignia Tarea ${i + 1}"
            }
        }
    }

    private fun guardarTarea(titulo: String, notas: String) {
        if (studentIdSeleccionado.isEmpty() || titulo.isEmpty() || notas.isEmpty()) {
            txtError.visibility = View.VISIBLE
            return
        }

        val tarea = hashMapOf(
            "studentId" to studentIdSeleccionado,
            "studentName" to btnAlumno.text.toString(),
            "teacherId" to teacherId,
            "title" to titulo,
            "description" to notas,
            "motivationInsignia" to insigniaMotivacional,
            "dateAssigned" to btnFecha.text.toString(),
            "status" to "Pendiente",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("tasks").add(tarea).addOnSuccessListener {
            Toast.makeText(this, "¡Tarea asignada con éxito!", Toast.LENGTH_SHORT).show()

            // Ir al perfil del alumno para verificar
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentIdSeleccionado)
            startActivity(intent)
            finish()
        }
    }
}