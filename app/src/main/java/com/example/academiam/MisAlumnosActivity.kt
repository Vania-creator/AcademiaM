package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore

data class Alumno(
    val id: String,
    val nombre: String,
    val instrumentoReal: String,
    val racha: Long
)

class MisAlumnosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val listaCompletaAlumnos = mutableListOf<Alumno>()
    private var teacherId: String = ""
    private var mostrandoTodos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_alumnos)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        val container = findViewById<LinearLayout>(R.id.containerAlumnos)
        val etBuscar = findViewById<EditText>(R.id.etBuscarAlumno)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresar)
        val btnToggleLista = findViewById<AppCompatButton>(R.id.btnVerTodosInstrumento)
        val txtSinAlumnos = findViewById<TextView>(R.id.txtSinAlumnos)

        btnRegresar.setOnClickListener { finish() }

        if (teacherId.isNotEmpty()) {
            obtenerMisAlumnos(container, txtSinAlumnos)
        }

        btnToggleLista.setOnClickListener {
            if (!mostrandoTodos) {
                obtenerTodosPorInstrumento(container, txtSinAlumnos)
                btnToggleLista.text = "Mostrar solo mis alumnos"
                btnToggleLista.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE"))
                btnToggleLista.setTextColor(android.graphics.Color.parseColor("#C62828"))
                mostrandoTodos = true
            } else {
                obtenerMisAlumnos(container, txtSinAlumnos)
                btnToggleLista.text = "Ver todos los alumnos por instrumento"
                btnToggleLista.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E1F5FE"))
                btnToggleLista.setTextColor(android.graphics.Color.parseColor("#0277BD"))
                mostrandoTodos = false
            }
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarYMostrarAlumnos(s.toString(), container, txtSinAlumnos)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun obtenerMisAlumnos(container: LinearLayout, txtSinAlumnos: TextView) {
        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("type", "fija")
            .get()
            .addOnSuccessListener { query ->
                procesarResultados(query, container, txtSinAlumnos)
            }
    }

    private fun obtenerTodosPorInstrumento(container: LinearLayout, txtSinAlumnos: TextView) {
        db.collection("teachers").document(teacherId).get()
            .addOnSuccessListener { doc ->
                val misInstrumentos = doc.get("instrumentos") as? List<String> ?: listOf()
                if (misInstrumentos.isEmpty()) {
                    txtSinAlumnos.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                db.collection("classes")
                    .whereIn("instrument", misInstrumentos)
                    .whereEqualTo("type", "fija")
                    .get()
                    .addOnSuccessListener { query ->
                        procesarResultados(query, container, txtSinAlumnos)
                    }
            }
    }

    private fun procesarResultados(query: com.google.firebase.firestore.QuerySnapshot, container: LinearLayout, txtSinAlumnos: TextView) {
        listaCompletaAlumnos.clear()
        container.removeAllViews()

        if (query.isEmpty) {
            txtSinAlumnos.visibility = View.VISIBLE
            return
        } else {
            txtSinAlumnos.visibility = View.GONE
        }

        for (classDoc in query) {
            val idAlumno = classDoc.getString("studentId")
            val instrumentoClase = classDoc.getString("instrument") ?: "N/A"

            if (idAlumno != null) {
                db.collection("students").document(idAlumno).get().addOnSuccessListener { studentDoc ->
                    if (studentDoc.exists()) {
                        val alumno = Alumno(
                            idAlumno,
                            studentDoc.getString("nombre") ?: "Desconocido",
                            instrumentoClase,
                            studentDoc.getLong("racha") ?: 0
                        )

                        if (!listaCompletaAlumnos.any { it.id == idAlumno && it.instrumentoReal == instrumentoClase }) {
                            listaCompletaAlumnos.add(alumno)
                        }
                        filtrarYMostrarAlumnos("", container, txtSinAlumnos)
                    }
                }
            }
        }
    }

    private fun filtrarYMostrarAlumnos(query: String, container: LinearLayout, txtSinAlumnos: TextView) {
        container.removeAllViews()
        val listaFiltrada = listaCompletaAlumnos.filter {
            it.nombre.contains(query, ignoreCase = true)
        }

        // Si después de filtrar no hay nada, mostramos el mensaje
        if (listaFiltrada.isEmpty()) {
            txtSinAlumnos.visibility = View.VISIBLE
            if (query.isNotEmpty()) {
                txtSinAlumnos.text = "No se encontraron alumnos con ese nombre"
            } else {
                txtSinAlumnos.text = "No tienes aún alumnos registrados"
            }
        } else {
            txtSinAlumnos.visibility = View.GONE
        }

        for (alumno in listaFiltrada) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_alumno_maestro, container, false)
            itemView.findViewById<TextView>(R.id.txtNombreAlumno).text = alumno.nombre
            itemView.findViewById<TextView>(R.id.txtInstrumentoAlumno).text = alumno.instrumentoReal
            itemView.findViewById<TextView>(R.id.txtRacha).text = "🔥 ${alumno.racha} Días"

            itemView.setOnClickListener {
                val intent = Intent(this, PerfilAlumnoActivity::class.java)
                intent.putExtra("STUDENT_ID", alumno.id)
                startActivity(intent)
            }
            container.addView(itemView)
        }
    }
}