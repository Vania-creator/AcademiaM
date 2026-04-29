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
    private var insigniaSeleccionada: String = ""

    // Lista para guardar las clases sin duplicados
    private val clasesDelDia = mutableListOf<Map<String, Any>>()
    private val listaVistasInsignias = mutableListOf<ImageView>()

    private lateinit var btnSeleccionarFecha: TextView
    private lateinit var btnSeleccionarAlumno: TextView
    private lateinit var txtErrorRecompensa: TextView

    // LA GRAN LISTA DE INSIGNIAS (Puedes agregar todas las que quieras)
    private val catalogoInsignias = listOf(
        "Súper Práctica", "Oído de Oro", "Ritmo Perfecto",
        "Gran Avance", "As de Escalas", "Creatividad",
        "Puntualidad", "Dedos Rápidos", "Teoría Master",
        "Concierto", "Primera Vista", "Compañerismo",
        "Rockstar", "Memoria Visual", "Afinación"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar_recompensa)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""
        if (teacherId.isEmpty()) {
            Toast.makeText(this, "Error: Sesión de maestro no encontrada", Toast.LENGTH_LONG).show()
            finish()
        }

        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFechaRec)
        btnSeleccionarAlumno = findViewById(R.id.btnSeleccionarAlumnoRec)
        txtErrorRecompensa = findViewById(R.id.txtErrorRecompensa)

        btnSeleccionarFecha.setOnClickListener { mostrarDatePicker() }

        btnSeleccionarAlumno.setOnClickListener {
            if (clasesDelDia.isNotEmpty()) mostrarSelectorAlumnos()
            else Toast.makeText(this, "Elige una fecha con alumnos primero", Toast.LENGTH_SHORT).show()
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

    // Usando exactamente tu misma lógica de búsqueda de ReporteClaseActivity
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

    private fun cargarCatalogoInsignias() {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayoutInsignias)

        for (nombre in catalogoInsignias) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_insignia_grid, gridLayout, false)

            val img = view.findViewById<ImageView>(R.id.imgInsignia)
            val txt = view.findViewById<TextView>(R.id.txtNombreInsignia)

            txt.text = nombre
            listaVistasInsignias.add(img)

            view.setOnClickListener {
                // Apagamos todas y prendemos solo la seleccionada
                listaVistasInsignias.forEach { it.alpha = 0.3f }
                img.alpha = 1.0f
                insigniaSeleccionada = nombre
            }

            // Configurar pesos para que quepan exactamente 3 columnas
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

        if (insigniaSeleccionada.isEmpty()) {
            txtErrorRecompensa.text = "Selecciona la insignia que quieres entregar"
            txtErrorRecompensa.visibility = View.VISIBLE
            return
        }

        // Lo guardamos en "reports" para que el PerfilAlumnoActivity lo detecte y dibuje el icono
        val reporteLogro = hashMapOf(
            "studentId" to studentIdSeleccionado,
            "studentName" to studentNameSeleccionado,
            "teacherId" to teacherId,
            "date" to btnSeleccionarFecha.text.toString(),
            "asistio" to true,
            "tipoClase" to "Logro",
            "instrument" to "N/A",
            "cuantoEstudio" to "N/A",
            "content" to "¡Felicidades! Se ha otorgado la insignia: $insigniaSeleccionada por excelente desempeño.",
            "insignia" to insigniaSeleccionada,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("reports").add(reporteLogro).addOnSuccessListener {
            Toast.makeText(this, "¡Insignia otorgada con éxito!", Toast.LENGTH_SHORT).show()

            // Te manda directo al perfil del alumno para ver la medalla en vivo
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            intent.putExtra("STUDENT_ID", studentIdSeleccionado)
            startActivity(intent)
            finish()
        }
    }
}