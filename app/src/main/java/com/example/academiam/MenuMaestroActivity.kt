package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore

class MenuMaestroActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_maestro)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""

        if (teacherId.isEmpty()) {
            ToastHelper.mostrarMensaje(this, "Error de sesión")
            finish()
            return
        }

        // Cargar datos iniciales
        cargarDatosMenuMaestro()

        // 🔥 Ir al perfil al tocar la imagen o los textos 🔥
        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        val contenedorPerfil = imgPerfil.parent as LinearLayout
        contenedorPerfil.setOnClickListener {
            val intent = Intent(this, PerfilMaestroActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        // --- CLICKS DEL MENÚ ---

        findViewById<AppCompatButton>(R.id.btnMisAlumnos).setOnClickListener {
            val intent = Intent(this, MisAlumnosActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        findViewById<AppCompatButton>(R.id.btnHorario).setOnClickListener {
            val intent = Intent(this, HorarioActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        findViewById<AppCompatButton>(R.id.btnReporteClase).setOnClickListener {
            val intent = Intent(this, ReporteClaseActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        findViewById<AppCompatButton>(R.id.btnAsignarRecompensa).setOnClickListener {
            val intent = Intent(this, AsignarRecompensaActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        findViewById<AppCompatButton>(R.id.btnTarea).setOnClickListener {
            val intent = Intent(this, TareaActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        findViewById<AppCompatButton>(R.id.btnSeleccionarLibro).setOnClickListener {
            val intent = Intent(this, LibrosActivity::class.java)
            intent.putExtra("TEACHER_ID", teacherId)
            startActivity(intent)
        }

        findViewById<AppCompatButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // 🔥 Usamos onResume para recargar la foto si el maestro la cambia en su perfil y regresa
    override fun onResume() {
        super.onResume()
        if (teacherId.isNotEmpty()) {
            cargarDatosMenuMaestro()
        }
    }

    private fun cargarDatosMenuMaestro() {
        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        val txtNombre = findViewById<TextView>(R.id.txtNombre)
        val txtInstrumento = findViewById<TextView>(R.id.txtInstrumento)

        db.collection("teachers").document(teacherId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    txtNombre.text = doc.getString("nombre") ?: "Maestro"

                    val instrumentos = doc.get("instrumentos") as? List<String>
                    txtInstrumento.text = instrumentos?.joinToString(", ") ?: "Música"

                    // 🔥 CARGA DINÁMICA DEL AVATAR 🔥
                    val avatarGuardado = doc.getString("avatar") ?: "logo"
                    val avatarResId = resources.getIdentifier(avatarGuardado, "drawable", packageName)

                    if (avatarResId != 0) {
                        imgPerfil.setImageResource(avatarResId)
                    } else {
                        imgPerfil.setImageResource(R.drawable.logo)
                    }
                }
            }
            .addOnFailureListener {
                imgPerfil.setImageResource(R.drawable.logo)
            }
    }
}