package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MenuMaestroActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_maestro)

        // 1. Recuperamos el ID que viene desde el Login
        teacherId = intent.getStringExtra("TEACHER_ID")

        // Referencias a los botones
        val btnMisAlumnos = findViewById<Button>(R.id.btnMisAlumnos)
        val btnHorario = findViewById<Button>(R.id.btnHorario)
        val btnReporteClase = findViewById<Button>(R.id.btnReporteClase)
        val btnTarea = findViewById<Button>(R.id.btnTarea)
        val btnSeleccionarLibro = findViewById<Button>(R.id.btnSeleccionarLibro)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // 2. Configuración de Clicks (Uno solo por botón)
        btnMisAlumnos.setOnClickListener {
            val intent = Intent(this, MisAlumnosActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId) // Pasamos el ID a la lista
            startActivity(intent)
        }

        btnHorario.setOnClickListener {
            val intent = Intent(this, HorarioActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        btnReporteClase.setOnClickListener {
            startActivity(Intent(this, ReporteClaseActivity::class.java))
        }

        btnTarea.setOnClickListener {
            startActivity(Intent(this, TareaActivity::class.java))
        }

        btnSeleccionarLibro.setOnClickListener {
            startActivity(Intent(this, LibrosActivity::class.java))
        }

        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 3. Cargar datos del perfil en el Header
        if (teacherId != null) {
            db.collection("teachers").document(teacherId!!).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        findViewById<TextView>(R.id.txtNombre).text = doc.getString("nombre")
                        val lista = doc.get("instrumentos") as? List<String>
                        findViewById<TextView>(R.id.txtInstrumento).text = lista?.joinToString(", ")
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                }
        }
    }
}