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
    private var instrumentoSeleccionado: String = ""

    // 🔥 Variables para la insignia del reporte
    private var insigniaSeleccionada: String = "Ninguna"
    private var claveInsigniaSeleccionada: String = ""

    private val clasesDelDia = mutableListOf<Map<String, Any>>()

    private lateinit var btnSeleccionarFecha: TextView
    private lateinit var btnSeleccionarAlumno: TextView
    private lateinit var txtTipoClaseAuto: TextView
    private lateinit var txtInstrumentoAuto: TextView
    private lateinit var txtErrorReporte: TextView

    // Experiencia base por tomar la clase
    private val XP_POR_ASISTENCIA = 50L

    // 🔥 Las 4 Insignias Rápidas ideales para un Reporte de Clase Diario
    private val insigniasClase = mapOf(
        "Constancia" to Pair("constancia", listOf(90L, 180L, 270L)),
        "Disciplina Total" to Pair("diciplinatotal", listOf(200L, 400L, 600L)),
        "Progreso Acelerado" to Pair("progresoacelerado", listOf(140L, 280L, 360L)),
        "Conocimiento Musical" to Pair("conocimientomusical", listOf(110L, 220L, 330L))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_clase)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""
        if (teacherId.isEmpty()) {
            ToastHelper.mostrarMensaje(this, "Error: Sesión de maestro no encontrada")
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
            else ToastHelper.mostrarMensaje(this, "Elige una fecha con alumnos primero")
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
        val idsAgregados = mutableSetOf<String>()

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

    // 🔥 CARGA DINÁMICA DE LAS 4 INSIGNIAS AL ABRIR LA PANTALLA
    private fun configurarInsignias() {
        val imgs = listOf<ImageView>(
            findViewById(R.id.imgInsignia1), findViewById(R.id.imgInsignia2),
            findViewById(R.id.imgInsignia3), findViewById(R.id.imgInsignia4)
        )

        val nombresInsignias = insigniasClase.keys.toList()

        imgs.forEachIndexed { i, img ->
            val nombreReal = nombresInsignias[i]
            val clave = insigniasClase[nombreReal]?.first ?: ""

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

                // Apagamos todas y prendemos solo la seleccionada
                imgs.forEach { it.alpha = 0.3f }
                img.alpha = 1.0f

                insigniaSeleccionada = nombreReal
                claveInsigniaSeleccionada = clave

                ToastHelper.mostrarMensaje(this, "$nombreReal seleccionada")
            }
        }
    }

    private fun validarYGuardar(asistio: Boolean, estudio: String, notas: String) {
        if (studentIdSeleccionado.isEmpty()) {
            ToastHelper.mostrarMensaje(this, "Selecciona un alumno primero")
            return
        }

        if (asistio && notas.isEmpty()) {
            txtErrorReporte.text = "Escribe las notas de la clase"
            txtErrorReporte.visibility = View.VISIBLE
            return
        }

        findViewById<AppCompatButton>(R.id.btnIngresarReporte).isEnabled = false

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
            procesarProgresoAlumno(asistio)
        }
    }

    // 🔥 TRANSACCIÓN DE EXPERIENCIA COMBINADA (Asistencia + Insignia)
    private fun procesarProgresoAlumno(asistio: Boolean) {
        val docRef = db.collection("students").document(studentIdSeleccionado)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            var expActual = snapshot.getLong("expTotal") ?: 0L
            val rachaActual = snapshot.getLong("racha") ?: 0L

            if (asistio) {
                // 1. Sumamos la experiencia por la pura asistencia a clase
                expActual += XP_POR_ASISTENCIA
                val nuevaRacha = rachaActual + 1

                // 2. Si el maestro seleccionó una insignia, calculamos su nivel y XP extra
                if (claveInsigniaSeleccionada.isNotEmpty() && insigniaSeleccionada != "Ninguna") {
                    val nivelActualInsignia = snapshot.getLong("insignias_progreso.$claveInsigniaSeleccionada") ?: 0L

                    // Solo la subimos de nivel y damos XP si aún no llega al máximo (3)
                    if (nivelActualInsignia < 3) {
                        val nuevoNivelInsignia = nivelActualInsignia + 1
                        val arreglosXP = insigniasClase[insigniaSeleccionada]?.second
                        val xpGanada = arreglosXP?.getOrNull((nuevoNivelInsignia - 1).toInt()) ?: 0L

                        expActual += xpGanada
                        transaction.update(docRef, "insignias_progreso.$claveInsigniaSeleccionada", nuevoNivelInsignia)
                    }
                }

                // 3. Calculamos el nuevo nivel general del alumno (cada 500 XP = 1 Nivel)
                val nuevoNivelGeneral = (expActual / 500).toInt()

                transaction.update(docRef, "expTotal", expActual)
                transaction.update(docRef, "nivel", nuevoNivelGeneral)
                transaction.update(docRef, "racha", nuevaRacha)
            } else {
                // Faltó: La racha se pierde y cae a cero (no se dan insignias ni XP)
                transaction.update(docRef, "racha", 0L)
            }
        }.addOnSuccessListener {
            ToastHelper.mostrarMensaje(this, "Reporte guardado con éxito")
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentIdSeleccionado)
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            ToastHelper.mostrarMensaje(this, "Error actualizando progreso: ${e.message}")
            findViewById<AppCompatButton>(R.id.btnIngresarReporte).isEnabled = true
        }
    }
}