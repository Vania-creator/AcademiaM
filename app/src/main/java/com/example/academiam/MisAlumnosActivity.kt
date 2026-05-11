package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore

// 🔥 MODIFICAMOS EL DATA CLASS PARA GUARDAR AVATAR Y VARIOS INSTRUMENTOS
data class Alumno(
    val id: String,
    val nombre: String,
    val instrumentos: MutableSet<String>, // Ahora guarda más de uno
    val racha: Long,
    val avatar: String
)

class MisAlumnosActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val listaCompletaAlumnos = mutableListOf<Alumno>()
    private var teacherId: String = ""
    private var mostrandoTodos = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
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

        val etBuscar = findViewById<EditText>(R.id.etBuscarAlumno)

        for (classDoc in query) {
            val idAlumno = classDoc.getString("studentId")
            val instrumentoClase = classDoc.getString("instrument") ?: "N/A"

            if (idAlumno != null) {
                db.collection("students").document(idAlumno).get().addOnSuccessListener { studentDoc ->
                    if (studentDoc.exists()) {
                        val avatarGuardado = studentDoc.getString("avatar") ?: "logo"

                        // 🔥 REVISAMOS SI EL ALUMNO YA ESTÁ EN LA LISTA
                        val alumnoExistente = listaCompletaAlumnos.find { it.id == idAlumno }

                        if (alumnoExistente != null) {
                            // Si ya existe, solo le agregamos el nuevo instrumento
                            alumnoExistente.instrumentos.add(instrumentoClase)
                        } else {
                            // Si no existe, lo creamos
                            val nuevoAlumno = Alumno(
                                id = idAlumno,
                                nombre = studentDoc.getString("nombre") ?: "Desconocido",
                                instrumentos = mutableSetOf(instrumentoClase),
                                racha = studentDoc.getLong("racha") ?: 0,
                                avatar = avatarGuardado
                            )
                            listaCompletaAlumnos.add(nuevoAlumno)
                        }

                        // Mantenemos el filtro que el usuario tenga escrito actualmente
                        filtrarYMostrarAlumnos(etBuscar.text.toString(), container, txtSinAlumnos)
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
            // 🔥 UNIMOS LOS INSTRUMENTOS CON COMA (Ej: "Piano, Canto")
            itemView.findViewById<TextView>(R.id.txtInstrumentoAlumno).text = alumno.instrumentos.joinToString(", ")
            itemView.findViewById<TextView>(R.id.txtRacha).text = "🔥 ${alumno.racha} Días"

            // 🔥 CARGAMOS EL AVATAR DINÁMICO
            val imgAvatar = itemView.findViewById<ImageView>(R.id.imgAvatarAlumno)
            val resId = resources.getIdentifier(alumno.avatar, "drawable", packageName)

            if (resId != 0) {
                imgAvatar.setImageResource(resId)
            } else {
                imgAvatar.setImageResource(R.drawable.logo)
            }

            itemView.setOnClickListener {
                val intent = Intent(this, PerfilAlumnoActivity::class.java)
                intent.putExtra("STUDENT_ID", alumno.id)
                startActivity(intent)
            }
            container.addView(itemView)
        }
    }
}