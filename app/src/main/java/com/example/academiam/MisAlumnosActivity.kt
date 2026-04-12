package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore

// Modelo para manejar los datos del alumno y su clase específica
data class Alumno(
    val id: String,
    val nombre: String,
    val instrumentoReal: String,
    val racha: Long
)

class MisAlumnosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val listaCompletaAlumnos = mutableListOf<Alumno>() // Lista para el buscador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_alumnos)

        // 1. Recuperar el ID del maestro enviado desde el menú
        val teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        // Referencias a la UI
        val container = findViewById<LinearLayout>(R.id.containerAlumnos)
        val etBuscar = findViewById<EditText>(R.id.etBuscarAlumno)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresar)

        btnRegresar.setOnClickListener { finish() }

        // 2. Cargar datos iniciales si tenemos el ID
        if (teacherId.isNotEmpty()) {
            obtenerAlumnosDeFirestore(teacherId, container)
        } else {
            Log.e("ACADEMIA", "Error: No se recibió el TEACHER_ID")
            Toast.makeText(this, "Error de sesión del maestro", Toast.LENGTH_SHORT).show()
        }

        // 3. Configuración del Buscador en tiempo real
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarYMostrarAlumnos(s.toString(), container)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun obtenerAlumnosDeFirestore(teacherId: String, container: LinearLayout) {
        // Consultamos las clases fijas asignadas a este maestro
        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .whereEqualTo("type", "fija")
            .get()
            .addOnSuccessListener { querySnapshot ->

                listaCompletaAlumnos.clear()

                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "No tienes alumnos asignados", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (classDoc in querySnapshot) {
                    val idAlumno = classDoc.getString("studentId")
                    val instrumentoClase = classDoc.getString("instrument") ?: "N/A"

                    if (idAlumno != null) {
                        // Buscamos los detalles (nombre, racha) en la colección de students
                        db.collection("students").document(idAlumno).get()
                            .addOnSuccessListener { studentDoc ->
                                if (studentDoc.exists()) {
                                    val alumno = Alumno(
                                        idAlumno,
                                        studentDoc.getString("nombre") ?: "Desconocido",
                                        instrumentoClase, // Instrumento real de la clase
                                        studentDoc.getLong("racha") ?: 0
                                    )

                                    // Evitamos duplicados en la lista local
                                    if (!listaCompletaAlumnos.any { it.id == idAlumno && it.instrumentoReal == instrumentoClase }) {
                                        listaCompletaAlumnos.add(alumno)
                                    }

                                    // Actualizamos la vista
                                    filtrarYMostrarAlumnos("", container)
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ACADEMIA", "Error al consultar Firestore", e)
                Toast.makeText(this, "Error al conectar con la base de datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filtrarYMostrarAlumnos(query: String, container: LinearLayout) {
        container.removeAllViews() // Limpiar la lista actual en pantalla

        // Filtramos la lista local (ignora mayúsculas/minúsculas)
        val listaFiltrada = listaCompletaAlumnos.filter {
            it.nombre.contains(query, ignoreCase = true)
        }

        for (alumno in listaFiltrada) {
            // Inflar el diseño de la tarjeta (item_alumno_maestro.xml)
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_alumno_maestro, container, false)

            // Asignar los datos a la tarjeta
            itemView.findViewById<TextView>(R.id.txtNombreAlumno).text = alumno.nombre
            itemView.findViewById<TextView>(R.id.txtInstrumentoAlumno).text = alumno.instrumentoReal
            itemView.findViewById<TextView>(R.id.txtRacha).text = "🔥 ${alumno.racha} Días"

            // Click para ir al perfil detallado del alumno
            itemView.setOnClickListener {
                val intent = Intent(this, PerfilAlumnoActivity::class.java)
                intent.putExtra("STUDENT_ID", alumno.id)
                startActivity(intent)
            }

            container.addView(itemView)
        }
    }
}