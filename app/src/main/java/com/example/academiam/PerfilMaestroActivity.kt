package com.example.academiam

import android.os.Bundle
import android.view.LayoutInflater
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
    private var historialFiltrado = mutableListOf<Map<String, Any>>()

    // Variables de paginación
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

        // Configuración de botones de paginación
        val btnAnt = findViewById<ImageButton>(R.id.btnAntHistorial)
        val btnSig = findViewById<ImageButton>(R.id.btnSigHistorial)

        btnAnt.setOnClickListener {
            if (paginaActual > 1) {
                paginaActual--
                renderizarTablaHistorial()
            }
        }

        btnSig.setOnClickListener {
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
                findViewById<TextView>(R.id.txtStatusMaestro).text = (doc.getString("status") ?: "ACTIVO").uppercase()

                // Cargar Chips de Especialidades
                val instrumentos = doc.get("instrumentos") as? List<String>
                val chipGroup = findViewById<ChipGroup>(R.id.groupEspecialidades)
                chipGroup.removeAllViews()
                instrumentos?.forEach { nombre ->
                    val chip = Chip(this)
                    chip.text = nombre
                    chipGroup.addView(chip)
                }

                // Cargar Burbujas de Días
                val dias = doc.get("diasDisponibles") as? List<String>
                val containerDias = findViewById<LinearLayout>(R.id.containerDias)
                containerDias.removeAllViews()
                dias?.forEach { dia ->
                    val tv = TextView(this)
                    tv.text = dia
                    tv.setPadding(0, 0, 0, 0)
                    tv.setBackgroundResource(R.drawable.circulo_negro_dia) // Tu drawable circular
                    tv.setTextColor(android.graphics.Color.WHITE)
                    tv.gravity = android.view.Gravity.CENTER

                    val params = LinearLayout.LayoutParams(90, 90)
                    params.setMargins(0, 0, 15, 0)
                    tv.layoutParams = params
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
                historialFiltrado.clear()

                var fijos = 0
                var muestrasPasadas = 0
                var muestrasFuturas = 0

                for (doc in query) {
                    val data = doc.data
                    val tipo = data["type"] as? String ?: ""
                    val fecha = data["date"] as? String ?: ""
                    val fechaFin = data["fechaFin"] as? String ?: ""

                    // 1. Lógica de Estadísticas (Espejo de la web)
                    if (tipo == "fija") {
                        if (fechaFin.isEmpty() || fechaFin >= todayStr) fijos++
                    } else if (tipo == "muestra") {
                        if (fecha < todayStr) muestrasPasadas++
                        else muestrasFuturas++
                    }

                    // 2. Filtro para el Historial (Excluir clases fijas activas)
                    if (tipo != "fija") {
                        historialFiltrado.add(data)
                    }
                }

                // Actualizar contadores en la UI
                findViewById<TextView>(R.id.statAlumnos).text = fijos.toString()
                findViewById<TextView>(R.id.statPendientes).text = muestrasFuturas.toString()
                findViewById<TextView>(R.id.statPasadas).text = muestrasPasadas.toString()

                // --- ORDENAMIENTO POR FECHA (MÁS RECIENTE ARRIBA) ---
                historialFiltrado.sortByDescending { it["date"] as? String ?: "" }

                // Iniciar renderizado
                paginaActual = 1
                renderizarTablaHistorial()
            }
    }

    private fun renderizarTablaHistorial() {
        val container = findViewById<LinearLayout>(R.id.containerHistorialMaestro)
        val txtPagina = findViewById<TextView>(R.id.txtPaginaHistorial)
        container.removeAllViews()

        if (historialFiltrado.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Sin registros históricos."
            empty.gravity = android.view.Gravity.CENTER
            empty.setPadding(0, 50, 0, 50)
            container.addView(empty)
            txtPagina.text = "Página 0 de 0"
            return
        }

        // Cálculo de sub-lista para paginación
        val totalItems = historialFiltrado.size
        val totalPaginas = Math.ceil(totalItems.toDouble() / filasPorPagina).toInt()

        val inicio = (paginaActual - 1) * filasPorPagina
        var fin = inicio + filasPorPagina
        if (fin > totalItems) fin = totalItems

        val subLista = historialFiltrado.subList(inicio, fin)
        txtPagina.text = "Página $paginaActual de $totalPaginas"

        // Inflar cada fila del historial
        for (clase in subLista) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_historial_maestro, container, false)

            view.findViewById<TextView>(R.id.histFecha).text = clase["date"] as? String
            view.findViewById<TextView>(R.id.histAlumno).text = clase["studentName"] as? String
            view.findViewById<TextView>(R.id.histInstrumento).text = clase["instrument"] as? String

            val tipo = (clase["type"] as? String ?: "").uppercase()
            val tvTipo = view.findViewById<TextView>(R.id.histTipo)
            tvTipo.text = tipo

            // Colores dinámicos para los badges
            when(tipo.lowercase()) {
                "muestra" -> {
                    tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#7B1FA2")) // Morado
                    tvTipo.setTextColor(android.graphics.Color.WHITE)
                }
                "unica" -> {
                    tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#1976D2")) // Azul
                    tvTipo.setTextColor(android.graphics.Color.WHITE)
                }
                else -> {
                    tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE")) // Gris
                    tvTipo.setTextColor(android.graphics.Color.BLACK)
                }
            }

            container.addView(view)
        }
    }
}