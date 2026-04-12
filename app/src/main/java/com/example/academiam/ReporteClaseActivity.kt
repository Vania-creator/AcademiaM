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

class ReporteClaseActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""
    private var studentIdSeleccionado: String = ""
    private var insigniaSeleccionada: String = "Ninguna"
    private var instrumentoSeleccionado: String = ""

    // Lista para guardar las clases sin duplicados
    private val clasesDelDia = mutableListOf<Map<String, Any>>()

    private lateinit var btnSeleccionarFecha: TextView
    private lateinit var btnSeleccionarAlumno: TextView
    private lateinit var txtTipoClaseAuto: TextView
    private lateinit var txtInstrumentoAuto: TextView
    private lateinit var txtErrorReporte: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_clase)

        // IMPORTANTE: Si el ID no llega, mostramos error
        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""
        if (teacherId.isEmpty()) {
            Toast.makeText(this, "Error: Sesión de maestro no encontrada", Toast.LENGTH_LONG).show()
            finish()
        }

        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        btnSeleccionarAlumno = findViewById(R.id.btnSeleccionarAlumno)
        txtTipoClaseAuto = findViewById(R.id.txtTipoClaseAuto)
        txtInstrumentoAuto = findViewById(R.id.txtInstrumentoAuto)
        txtErrorReporte = findViewById(R.id.txtErrorReporte)

        val etCuantoEstudio = findViewById<EditText>(R.id.etCuantoEstudio)
        val etNotasClase = findViewById<EditText>(R.id.etNotasClase)
        val cbAsistio = findViewById<CheckBox>(R.id.cbAsistio)

        btnSeleccionarFecha.setOnClickListener { mostrarDatePicker() }

        btnSeleccionarAlumno.setOnClickListener {
            if (clasesDelDia.isNotEmpty()) mostrarSelectorAlumnos()
            else Toast.makeText(this, "Elige una fecha con alumnos primero", Toast.LENGTH_SHORT).show()
        }

        configurarInsignias()

        findViewById<AppCompatButton>(R.id.btnRegresarReporte).setOnClickListener { finish() }
        findViewById<AppCompatButton>(R.id.btnIngresarReporte).setOnClickListener {
            validarYGuardar(cbAsistio.isChecked, etCuantoEstudio.text.toString(), etNotasClase.text.toString())
        }
    }

    private fun mostrarDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val fechaFormateada = "%04d-%02d-%02d".format(y, m + 1, d)
            btnSeleccionarFecha.text = fechaFormateada
            buscarAlumnosTusClases(y, m, d, fechaFormateada)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun buscarAlumnosTusClases(y: Int, m: Int, d: Int, fechaCompleta: String) {
        val calendar = Calendar.getInstance()
        calendar.set(y, m, d)
        val sdf = SimpleDateFormat("EEE", Locale("es", "MX"))
        var diaSemana = sdf.format(calendar.time).lowercase()

        diaSemana = when {
            diaSemana.contains("lu") -> "Lun"
            diaSemana.contains("ma") -> "Mar"
            diaSemana.contains("mi") -> "Mie"
            diaSemana.contains("ju") -> "Jue"
            diaSemana.contains("vi") -> "Vie"
            diaSemana.contains("sá") || diaSemana.contains("sa") -> "Sab"
            else -> "Dom"
        }

        clasesDelDia.clear()
        val idsAgregados = mutableSetOf<String>() // Para evitar que se repita la clase

        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { query ->
                for (doc in query) {
                    val dbDate = doc.getString("date") ?: ""
                    val dbDay = doc.getString("dayOfWeek") ?: ""
                    val dbType = doc.getString("type") ?: ""
                    val studentId = doc.getString("studentId") ?: ""
                    val hora = doc.getString("time") ?: ""

                    // LLAVE ÚNICA: Alumno + Hora + Tipo (Evita duplicados)
                    val llaveUnica = "$studentId-$hora-$dbType"

                    if (!idsAgregados.contains(llaveUnica)) {
                        if ((dbType == "fija" && dbDay == diaSemana) || dbDate == fechaCompleta) {
                            clasesDelDia.add(doc.data)
                            idsAgregados.add(llaveUnica)
                        }
                    }
                }

                if (clasesDelDia.isEmpty()) {
                    btnSeleccionarAlumno.text = "Sin clases tuyas hoy"
                    btnSeleccionarAlumno.isEnabled = false
                } else {
                    btnSeleccionarAlumno.text = "Toca para elegir alumno"
                    btnSeleccionarAlumno.isEnabled = true
                }
            }
    }

    private fun mostrarSelectorAlumnos() {
        val opciones = clasesDelDia.map {
            "${it["studentName"]} - ${it["time"]} [${it["instrument"]}]"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecciona a tu alumno")
            .setItems(opciones) { _, which ->
                val seleccion = clasesDelDia[which]
                studentIdSeleccionado = seleccion["studentId"].toString()
                instrumentoSeleccionado = seleccion["instrument"].toString()

                btnSeleccionarAlumno.text = seleccion["studentName"].toString()
                txtTipoClaseAuto.text = seleccion["type"].toString().uppercase()
                txtInstrumentoAuto.text = instrumentoSeleccionado
            }.show()
    }

    private fun configurarInsignias() {
        val imgs = listOf<ImageView>(
            findViewById(R.id.imgInsignia1), findViewById(R.id.imgInsignia2),
            findViewById(R.id.imgInsignia3), findViewById(R.id.imgInsignia4)
        )

        imgs.forEachIndexed { i, img ->
            img.setOnClickListener {
                val nombreInsignia = "Insignia ${i + 1}"

                if (studentIdSeleccionado.isEmpty()) {
                    Toast.makeText(this, "Selecciona primero un alumno", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // VERIFICAR SI YA EXISTE
                db.collection("reports")
                    .whereEqualTo("studentId", studentIdSeleccionado)
                    .whereEqualTo("insignia", nombreInsignia)
                    .get()
                    .addOnSuccessListener { query ->
                        if (!query.isEmpty) {
                            // YA LA TIENE
                            Toast.makeText(this, "Insignia ya existente en el perfil del alumno", Toast.LENGTH_SHORT).show()
                            img.alpha = 0.1f // Se ve casi invisible indicando que no se puede usar
                        } else {
                            // NO LA TIENE, SELECCIONARLA
                            imgs.forEach { it.alpha = 0.3f }
                            img.alpha = 1.0f
                            insigniaSeleccionada = nombreInsignia
                            Toast.makeText(this, "$nombreInsignia seleccionada", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun validarYGuardar(asistio: Boolean, estudio: String, notas: String) {
        if (studentIdSeleccionado.isEmpty()) {
            Toast.makeText(this, "Selecciona un alumno primero", Toast.LENGTH_SHORT).show()
            return
        }

        if (asistio && notas.isEmpty()) {
            txtErrorReporte.text = "Escribe las notas de la clase"
            txtErrorReporte.visibility = View.VISIBLE
            return
        }

        val reporte = hashMapOf(
            "studentId" to studentIdSeleccionado,
            "studentName" to btnSeleccionarAlumno.text.toString(),
            "teacherId" to teacherId,
            "date" to btnSeleccionarFecha.text.toString(),
            "asistio" to asistio,
            "tipoClase" to txtTipoClaseAuto.text.toString(),
            "instrument" to instrumentoSeleccionado,
            "cuantoEstudio" to if(asistio) estudio else "N/A",
            "content" to if(asistio) notas else "Falta: El alumno no asistió",
            "insignia" to if(asistio) insigniaSeleccionada else "Ninguna",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("reports").add(reporte).addOnSuccessListener {
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentIdSeleccionado)
            startActivity(intent)
            finish()
        }
    }
}