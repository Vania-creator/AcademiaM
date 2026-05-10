package com.example.academiam

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AsignarRecompensaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""
    private var studentIdSeleccionado: String = ""
    private var studentNameSeleccionado: String = ""

    // Almacena el ID interno ("primersonido") y el nombre legible ("Primer Sonido")
    private var claveInsigniaSeleccionada: String = ""
    private var nombreInsigniaSeleccionada: String = ""

    private val clasesDelDia = mutableListOf<Map<String, Any>>()
    private val listaVistasInsignias = mutableListOf<ImageView>()

    private lateinit var btnSeleccionarFecha: TextView
    private lateinit var btnSeleccionarAlumno: TextView
    private lateinit var txtErrorRecompensa: TextView

    // 🔥 EL DICCIONARIO MAESTRO DE TUS 39 INSIGNIAS (13 x 3 Niveles)
    private val catalogoInsignias = mapOf(
        "Primer Sonido" to Pair("primersonido", listOf(50L, 100L, 150L)),
        "En Marcha" to Pair("enmarcha", listOf(80L, 160L, 240L)),
        "Dominio Básico" to Pair("dominiobasico", listOf(100L, 200L, 300L)),
        "Talento en Ascenso" to Pair("talentoacenso", listOf(150L, 300L, 450L)),
        "Teclas Maestras" to Pair("teclasmaestras", listOf(120L, 240L, 360L)),
        "Constancia" to Pair("constancia", listOf(90L, 180L, 270L)),
        "Disciplina Total" to Pair("diciplinatotal", listOf(200L, 400L, 600L)),
        "Tiempo Invertido" to Pair("tiempoinvertido", listOf(160L, 360L, 480L)),
        "Conocimiento Musical" to Pair("conocimientomusical", listOf(110L, 220L, 330L)),
        "Formación Sólida" to Pair("formacionsolida", listOf(180L, 360L, 450L)),
        "Excelencia" to Pair("excelencia",              listOf(250L, 500L, 750L)),
        "Progreso Acelerado" to Pair("progresoacelerado", listOf(140L, 280L, 360L)),
        "Pasión por la Música" to Pair("pasionporlamusica", listOf(300L, 600L, 900L))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar_recompensa)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""
        if (teacherId.isEmpty()) {
            ToastHelper.mostrarMensaje(this, "Error: Sesión de maestro no encontrada")
            finish()
        }

        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFechaRec)
        btnSeleccionarAlumno = findViewById(R.id.btnSeleccionarAlumnoRec)
        txtErrorRecompensa = findViewById(R.id.txtErrorRecompensa)

        btnSeleccionarFecha.setOnClickListener { mostrarDatePicker() }

        btnSeleccionarAlumno.setOnClickListener {
            if (clasesDelDia.isNotEmpty()) mostrarSelectorAlumnos()
            else ToastHelper.mostrarMensaje(this, "Elige una fecha con alumnos primero")
        }

        cargarCatalogoInsignias()

        findViewById<AppCompatButton>(R.id.btnRegresarRec).setOnClickListener { finish() }
        findViewById<AppCompatButton>(R.id.btnAsignarRec).setOnClickListener {
            validarYGuardar()
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

        btnSeleccionarAlumno.text = "Buscando tus alumnos..."

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
            .setTitle("¿A quién le darás la insignia?")
            .setItems(opciones) { _, which ->
                val seleccion = clasesDelDia[which]
                studentIdSeleccionado = seleccion["studentId"].toString()
                studentNameSeleccionado = seleccion["studentName"].toString()
                btnSeleccionarAlumno.text = studentNameSeleccionado
            }.show()
    }

    // 🔥 CÓDIGO ACTUALIZADO: Carga las imágenes PNG dinámicamente
    private fun cargarCatalogoInsignias() {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayoutInsignias)

        for ((nombre, datos) in catalogoInsignias) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_insignia_grid, gridLayout, false)

            val img = view.findViewById<ImageView>(R.id.imgInsignia)
            val txt = view.findViewById<TextView>(R.id.txtNombreInsignia)

            txt.text = nombre
            listaVistasInsignias.add(img)

            // --- LÓGICA DE IMÁGENES DINÁMICAS ---
            val clave = datos.first
            // Construimos el nombre del archivo de Nivel 1 (ej: "primersonido1")
            val nombreArchivo = "${clave}1"

            // Buscamos el ID en la carpeta drawable
            val imageResId = resources.getIdentifier(nombreArchivo, "drawable", packageName)

            if (imageResId != 0) {
                img.setImageResource(imageResId) // Asigna la imagen PNG correcta
            } else {
                img.setImageResource(R.drawable.logo) // Si te falta alguna foto, pone el logo de Neumastudio por defecto
            }
            // ------------------------------------

            view.setOnClickListener {
                listaVistasInsignias.forEach { it.alpha = 0.3f }
                img.alpha = 1.0f

                claveInsigniaSeleccionada = clave
                nombreInsigniaSeleccionada = nombre
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            view.layoutParams = params

            gridLayout.addView(view)
        }
    }

    private fun validarYGuardar() {
        if (studentIdSeleccionado.isEmpty()) {
            txtErrorRecompensa.text = "Selecciona un alumno primero"
            txtErrorRecompensa.visibility = View.VISIBLE
            return
        }

        if (claveInsigniaSeleccionada.isEmpty()) {
            txtErrorRecompensa.text = "Selecciona la insignia que quieres entregar"
            txtErrorRecompensa.visibility = View.VISIBLE
            return
        }

        txtErrorRecompensa.visibility = View.GONE
        findViewById<AppCompatButton>(R.id.btnAsignarRec).isEnabled = false

        // 🔥 TRANSACCIÓN DE NIVELES Y EXPERIENCIA
        val docRef = db.collection("students").document(studentIdSeleccionado)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val expActual = snapshot.getLong("expTotal") ?: 0L

            val nivelActualInsignia = snapshot.getLong("insignias_progreso.$claveInsigniaSeleccionada") ?: 0L
            val nuevoNivelInsignia = nivelActualInsignia + 1

            if (nuevoNivelInsignia > 3) {
                throw Exception("MAX_LEVEL")
            }

            val arreglosXP = catalogoInsignias[nombreInsigniaSeleccionada]?.second
            val xpGanada = arreglosXP?.getOrNull((nuevoNivelInsignia - 1).toInt()) ?: 0L

            val nuevaExp = expActual + xpGanada
            val nuevoNivelGeneral = (nuevaExp / 500).toInt()

            transaction.update(docRef, "expTotal", nuevaExp)
            transaction.update(docRef, "nivel", nuevoNivelGeneral)
            transaction.update(docRef, "insignias_progreso.$claveInsigniaSeleccionada", nuevoNivelInsignia)

            mapOf("xp" to xpGanada, "nivel" to nuevoNivelInsignia)

        }.addOnSuccessListener { resultado ->
            val xp = resultado["xp"]
            val lvl = resultado["nivel"]
            registrarReporteLogro(lvl as Long, xp as Long)

        }.addOnFailureListener { e ->
            findViewById<AppCompatButton>(R.id.btnAsignarRec).isEnabled = true
            if (e.message == "MAX_LEVEL") {
                ToastHelper.mostrarMensaje(this, "El alumno ya tiene el nivel 3 (Máximo) de esta insignia")
            } else {
                ToastHelper.mostrarMensaje(this, "Error de red: ${e.message}")
            }
        }
    }

    private fun registrarReporteLogro(nivelLogrado: Long, xpGanada: Long) {
        val reporteLogro = hashMapOf(
            "studentId" to studentIdSeleccionado,
            "studentName" to studentNameSeleccionado,
            "teacherId" to teacherId,
            "date" to btnSeleccionarFecha.text.toString(),
            "asistio" to true,
            "tipoClase" to "Logro",
            "instrument" to "N/A",
            "cuantoEstudio" to "N/A",
            "content" to "¡Felicidades! Desbloqueaste $nombreInsigniaSeleccionada Nivel $nivelLogrado. (+$xpGanada XP)",
            "insignia" to nombreInsigniaSeleccionada,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("reports").add(reporteLogro).addOnSuccessListener {
            ToastHelper.mostrarMensaje(this, "Insignia nivel $nivelLogrado otorgada con éxito!")
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentIdSeleccionado)
            startActivity(intent)
            finish()
        }
    }
}