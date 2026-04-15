package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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

        // Referencias a los componentes del XML
        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        val btnMisAlumnos = findViewById<Button>(R.id.btnMisAlumnos)
        val btnHorario = findViewById<Button>(R.id.btnHorario)
        val btnReporteClase = findViewById<Button>(R.id.btnReporteClase)
        val btnAsignarRecompensa = findViewById<Button>(R.id.btnAsignarRecompensa) // Nuevo
        val btnTarea = findViewById<Button>(R.id.btnTarea)
        val btnSeleccionarLibro = findViewById<Button>(R.id.btnSeleccionarLibro) // Nuevo
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // 2. --- NAVEGACIÓN AL PERFIL DEL MAESTRO ---
        imgPerfil.setOnClickListener {
            if (teacherId != null) {
                val intent = Intent(this, PerfilMaestroActivity::class.java)
                intent.putExtra("TEACHER_ID", teacherId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Configuración de Clicks
        btnMisAlumnos.setOnClickListener {
            val intent = Intent(this, MisAlumnosActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        btnHorario.setOnClickListener {
            val intent = Intent(this, HorarioActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        btnReporteClase.setOnClickListener {
            val intent = Intent(this, ReporteClaseActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        btnAsignarRecompensa.setOnClickListener {
            // Aquí puedes mandarlo a una actividad de recompensas o al mismo Perfil del Alumno
            Toast.makeText(this, "Módulo de Recompensas", Toast.LENGTH_SHORT).show()
        }

        btnTarea.setOnClickListener {
            val intent = Intent(this, TareaActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        btnSeleccionarLibro.setOnClickListener {

            val intent = Intent(this, LibrosActivity::class.java)

            intent.putExtra("TEACHER_ID", teacherId)

            startActivity(intent)
        }
        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 4. Cargar datos básicos en el Header del Menú
        if (teacherId != null) {
            db.collection("teachers").document(teacherId!!).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        findViewById<TextView>(R.id.txtNombre).text = doc.getString("nombre")
                        val lista = doc.get("instrumentos") as? List<String>
                        findViewById<TextView>(R.id.txtInstrumento).text = lista?.joinToString(", ")
                    }
                }
        }
    }
}