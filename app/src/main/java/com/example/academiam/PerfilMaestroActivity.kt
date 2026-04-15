package com.example.academiam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PerfilMaestroActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""

    // Listas para manejar los datos localmente (como en tu JS)
    private var todasLasClases = mutableListOf<Map<String, Any>>()
    private var historialFiltrado = mutableListOf<Map<String, Any>>()

    // Paginación local
    private var paginaActual = 1
    private val filasPorPagina = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_maestro)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        if (teacherId.isNotEmpty()) {
            cargarDatosMaestro()
            cargarYProcesarClases()
        }

        // Botones de paginación
        findViewById<ImageButton>(R.id.btnAntHistorial).setOnClickListener {
            if (paginaActual > 1) {
                paginaActual--
                renderizarTablaHistorial()
            }
        }

        findViewById<ImageButton>(R.id.btnSigHistorial).setOnClickListener {
            val totalPaginas = Math.ceil(historialFiltrado.size.toDouble() / filasPorPagina).toInt()
            if (paginaActual < totalPaginas) {
                paginaActual++
                renderizarTablaHistorial()
            }
        }

        findViewById<Button>(R.id.btnRegresarMaestro).setOnClickListener { finish() }
    }

    private fun cargarDatosMaestro() {
        db.collection("teachers").document(teacherId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                findViewById<TextView>(R.id.txtNombreMaestro).text = doc.getString("nombre")
                findViewById<TextView>(R.id.txtUsuarioMaestro).text = "User: ${doc.getString("usuario")}"

                // Cargar Especialidades
                val instrumentos = doc.get("instrumentos") as? List<String>
                val chipGroup = findViewById<ChipGroup>(R.id.groupEspecialidades)
                instrumentos?.forEach { nombre ->
                    val chip = Chip(this)
                    chip.text = nombre
                    chipGroup.addView(chip)
                }

                // Cargar Días
                val dias = doc.get("diasDisponibles") as? List<String>
                val containerDias = findViewById<LinearLayout>(R.id.containerDias)
                dias?.forEach { dia ->
                    val tv = TextView(this)
                    tv.text = dia
                    tv.setPadding(20, 10, 20, 10)
                    tv.setBackgroundResource(R.drawable.circulo_negro_dia)
                    tv.setTextColor(android.graphics.Color.WHITE)
                    val params = LinearLayout.LayoutParams(90, 90)
                    params.setMargins(0, 0, 15, 0)
                    tv.layoutParams = params
                    tv.gravity = android.view.Gravity.CENTER
                    containerDias.addView(tv)
                }
            }
        }
    }

    private fun cargarYProcesarClases() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { query ->
                todasLasClases.clear()
                historialFiltrado.clear()

                var fijos = 0
                var muestrasPasadas = 0
                var muestrasFuturas = 0

                for (doc in query) {
                    val data = doc.data
                    val tipo = data["type"] as? String ?: ""
                    val fecha = data["date"] as? String ?: ""
                    val fechaFin = data["fechaFin"] as? String ?: ""

                    // 1. Lógica de Estadísticas (Igual a tu JS)
                    if (tipo == "fija") {
                        if (fechaFin.isEmpty() || fechaFin >= todayStr) fijos++
                    } else if (tipo == "muestra") {
                        if (fecha < todayStr) muestrasPasadas++
                        else muestrasFuturas++
                    }

                    // 2. Filtro para el Historial (Excluir fijas)
                    if (tipo != "fija") {
                        historialFiltrado.add(data)
                    }
                }

                // Actualizar números en pantalla
                findViewById<TextView>(R.id.statAlumnos).text = fijos.toString()
                findViewById<TextView>(R.id.statPendientes).text = muestrasFuturas.toString()
                findViewById<TextView>(R.id.statPasadas).text = muestrasPasadas.toString()

                // Ordenar historial por fecha (Descendente)
                historialFiltrado.sortByDescending { it["date"] as? String ?: "" }

                renderizarTablaHistorial()
            }
    }

    private fun renderizarTablaHistorial() {
        val container = findViewById<LinearLayout>(R.id.containerHistorialMaestro)
        val txtPagina = findViewById<TextView>(R.id.txtPaginaHistorial)
        container.removeAllViews()

        if (historialFiltrado.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No hay clases registradas en el historial."
            empty.setPadding(0, 20, 0, 20)
            empty.gravity = android.view.Gravity.CENTER
            container.addView(empty)
            return
        }

        // Paginación manual
        val inicio = (paginaActual - 1) * filasPorPagina
        var fin = inicio + filasPorPagina
        if (fin > historialFiltrado.size) fin = historialFiltrado.size

        val subLista = historialFiltrado.subList(inicio, fin)
        txtPagina.text = "Página $paginaActual"

        for (clase in subLista) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_historial_maestro, container, false)

            view.findViewById<TextView>(R.id.histFecha).text = clase["date"] as? String
            view.findViewById<TextView>(R.id.histAlumno).text = clase["studentName"] as? String
            view.findViewById<TextView>(R.id.histInstrumento).text = clase["instrument"] as? String

            val tipo = (clase["type"] as? String ?: "").uppercase()
            val tvTipo = view.findViewById<TextView>(R.id.histTipo)
            tvTipo.text = tipo

            // Colores por tipo
            when(tipo.lowercase()) {
                "muestra" -> tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#7B1FA2"))
                "unica" -> tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#1976D2"))
                else -> tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#757575"))
            }

            container.addView(view)
        }
    }
}