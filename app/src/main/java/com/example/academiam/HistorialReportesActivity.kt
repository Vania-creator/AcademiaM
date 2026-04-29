package com.example.academiam

import android.R.attr.data
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialReportesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // Lista maestra para guardar todo y no tener que descargar de Firebase cada vez que busques
    private val listaCompletaReportes = mutableListOf<Map<String, Any>>()
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_reportes)

        val studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        val studentName = intent.getStringExtra("STUDENT_NAME") ?: "Alumno"

        findViewById<TextView>(R.id.txtTituloHistorial).text = "Historial: $studentName"
        findViewById<AppCompatButton>(R.id.btnRegresarHistorial).setOnClickListener { finish() }

        container = findViewById(R.id.containerReportes)
        val etBuscador = findViewById<EditText>(R.id.etBuscadorReportes)

        // 1. Descargamos los datos una sola vez
        cargarHistorialDesdeFirebase(studentId)

        // 2. Escuchamos cada letra que el usuario escribe
        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarYMostrarReportes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun cargarHistorialDesdeFirebase(studentId: String) {
        db.collection("reports")
            .whereEqualTo("studentId", studentId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                listaCompletaReportes.clear()

                for (doc in query) {
                    listaCompletaReportes.add(doc.data)
                }

                // Mostrar todos por defecto al inicio
                filtrarYMostrarReportes("")
            }
    }

    private fun filtrarYMostrarReportes(textoBusqueda: String) {
        container.removeAllViews()
        val filtro = textoBusqueda.lowercase().trim()

// Filtramos la lista maestra
        val listaFiltrada = if (filtro.isEmpty()) {
            listaCompletaReportes
        } else {
            listaCompletaReportes.filter { dato ->
                // ¡AQUÍ ESTABA EL ERROR! Cambiamos "data" por "dato"
                val fecha = (dato["date"] as? String ?: "").lowercase()
                val tipo = (dato["tipoClase"] as? String ?: "").lowercase()
                val contenido = (dato["content"] as? String ?: "").lowercase()
                val insignia = (dato["insignia"] as? String ?: "").lowercase()

                // Si lo que escribiste coincide con la fecha, las notas o la insignia, lo mostramos
                fecha.contains(filtro) || contenido.contains(filtro) || tipo.contains(filtro) || insignia.contains(filtro)
            }
        }

        if (listaFiltrada.isEmpty()) {
            val tvError = TextView(this).apply {
                text = if (listaCompletaReportes.isEmpty()) "No hay historial disponible" else "No se encontraron resultados para '$textoBusqueda'"
                setTextColor(android.graphics.Color.GRAY)
                textSize = 16f
                setPadding(0, 40, 0, 0)
                gravity = android.view.Gravity.CENTER
            }
            container.addView(tvError)
            return
        }

        // Pintamos los resultados
        for (data in listaFiltrada) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_reporte_historial, container, false)

            val fecha = data["date"] as? String ?: "--"
            val tipo = data["tipoClase"] as? String ?: ""
            val contenido = data["content"] as? String ?: ""
            val asistio = if (data["asistio"] as? Boolean == true) "✅ Asistió" else "❌ Faltó"
            val insignia = data["insignia"] as? String ?: "Ninguna"

            view.findViewById<TextView>(R.id.txtFechaItem).text = "$fecha ($tipo)"
            view.findViewById<TextView>(R.id.txtAsistenciaItem).text = "$asistio | Motivación: $insignia"
            view.findViewById<TextView>(R.id.txtContenidoItem).text = contenido

            container.addView(view)
        }
    }
}