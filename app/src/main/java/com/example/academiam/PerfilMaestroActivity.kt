package com.example.academiam

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PerfilMaestroActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_maestro)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        if (teacherId.isNotEmpty()) {
            cargarDatosMaestro()
            cargarHistorialMaestro()
        }

        findViewById<Button>(R.id.btnRegresarMaestro).setOnClickListener { finish() }
    }

    private fun cargarDatosMaestro() {
        db.collection("teachers").document(teacherId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                findViewById<TextView>(R.id.txtNombreMaestro).text = doc.getString("nombre")
                findViewById<TextView>(R.id.txtUsuarioMaestro).text = "User: ${doc.getString("usuario")}"

                // Estadísticas
                findViewById<TextView>(R.id.statAlumnos).text = doc.getLong("alumnosCount")?.toString() ?: "0"
                findViewById<TextView>(R.id.statPendientes).text = doc.getLong("muestrasPendientes")?.toString() ?: "0"
                findViewById<TextView>(R.id.statPasadas).text = doc.getLong("muestrasPasadas")?.toString() ?: "0"

                // Especialidades (Instrumentos)
                val instrumentos = doc.get("instrumentos") as? List<String>
                val chipGroup = findViewById<ChipGroup>(R.id.groupEspecialidades)
                instrumentos?.forEach { nombre ->
                    val chip = Chip(this)
                    chip.text = nombre
                    chip.setChipBackgroundColorResource(R.color.white) // O el color crema de tu imagen
                    chipGroup.addView(chip)
                }

                // Días (Burbujas)
                val dias = doc.get("diasDisponibles") as? List<String>
                val containerDias = findViewById<LinearLayout>(R.id.containerDias)
                dias?.forEach { dia ->
                    val tv = TextView(this)
                    tv.text = dia
                    tv.setPadding(20, 10, 20, 10)
                    tv.setBackgroundResource(R.drawable.circulo_negro_dia) // Crea un drawable circular
                    tv.setTextColor(android.graphics.Color.WHITE)
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 15, 0)
                    tv.layoutParams = params
                    containerDias.addView(tv)
                }
            }
        }
    }

    private fun cargarHistorialMaestro() {
        val container = findViewById<LinearLayout>(R.id.containerHistorialMaestro)

        // Filtramos: Solo clases de este maestro que NO sean fijas
        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                container.removeAllViews()
                for (doc in query) {
                    val tipo = doc.getString("type") ?: ""

                    // REGLA DE ORO: Si es fija, no la mostramos aquí
                    if (tipo.lowercase() == "fija") continue

                    val view = LayoutInflater.from(this).inflate(R.layout.item_historial_maestro, container, false)

                    view.findViewById<TextView>(R.id.histFecha).text = doc.getString("date")
                    view.findViewById<TextView>(R.id.histAlumno).text = doc.getString("studentName")
                    view.findViewById<TextView>(R.id.histInstrumento).text = doc.getString("instrument")

                    val tvTipo = view.findViewById<TextView>(R.id.histTipo)
                    tvTipo.text = tipo.uppercase()

                    // Color dinámico según tipo
                    when(tipo.lowercase()) {
                        "muestra" -> tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#F3E5F5"))
                        "baja" -> tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                        else -> tvTipo.setBackgroundColor(android.graphics.Color.parseColor("#E1F5FE"))
                    }

                    container.addView(view)
                }
            }
    }
}