package com.example.academiam

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MenuAlumnoActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var studentId: String = ""
    private var studentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_alumno)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""

        if (studentId.isEmpty()) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDatosAlumno()
        cargarUltimaTarea()

        // --- CLICKS DEL MENÚ ---

        // Ir al perfil (Mi progreso)
        findViewById<AppCompatButton>(R.id.btnMiProgreso).setOnClickListener {
            irAPerfil()
        }

        findViewById<ImageView>(R.id.imgPerfilAlumno).setOnClickListener {
            irAPerfil()
        }

        // Ver historial de tareas
        findViewById<TextView>(R.id.btnVerTodasTareasAl).setOnClickListener {
            val intent = Intent(this, HistorialTareasActivity::class.java)
            intent.putExtra("STUDENT_ID", studentId)
            intent.putExtra("STUDENT_NAME", studentName)
            startActivity(intent)
        }

        // Ir a Libros
        findViewById<AppCompatButton>(R.id.btnSeleccionarLibroAl).setOnClickListener {
            val intent = Intent(this, LibrosActivity::class.java)
            startActivity(intent)
        }

        // Ir a Historial de Reportes
        findViewById<AppCompatButton>(R.id.btnReportesAl).setOnClickListener {
            val intent = Intent(this, HistorialReportesActivity::class.java)
            intent.putExtra("STUDENT_ID", studentId)
            intent.putExtra("STUDENT_NAME", studentName)
            startActivity(intent)
        }

        // Cerrar sesión
        findViewById<AppCompatButton>(R.id.btnLogoutAlumno).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun cargarDatosAlumno() {
        db.collection("students").document(studentId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    studentName = doc.getString("nombre") ?: "Alumno"
                    val racha = doc.getLong("racha") ?: 0

                    findViewById<TextView>(R.id.txtNombreAlumno).text = studentName
                    findViewById<TextView>(R.id.txtRachaAlumno).text = "Racha: $racha Días 🔥"
                }
            }
    }

    // Variables para guardar la tarea actual
    private var currentTaskId: String = ""
    private var currentTaskTitle: String = ""
    private var currentTaskDesc: String = ""
    private var currentTaskInsignia: String = ""

    private fun cargarUltimaTarea() {
        val tvTitulo = findViewById<TextView>(R.id.txtTituloTareaMenu)
        val tvStatus = findViewById<TextView>(R.id.txtStatusTareaMenu)
        val tvDesc = findViewById<TextView>(R.id.txtDescTareaMenu)
        val btnAbrir = findViewById<AppCompatButton>(R.id.btnAbrirTarea)

        db.collection("tasks")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (query.isEmpty) {
                    tvTitulo.text = "No tienes tareas asignadas :)"
                    tvDesc.text = "¡Disfruta tu día!"
                    tvStatus.visibility = View.GONE
                    btnAbrir.visibility = View.GONE
                } else {
                    val doc = query.documents[0]
                    val status = doc.getString("status") ?: "Pendiente"

                    // Guardamos los datos temporalmente para el Dialog
                    currentTaskId = doc.id
                    currentTaskTitle = doc.getString("title") ?: "Sin título"
                    currentTaskDesc = doc.getString("description") ?: "Sin descripción"
                    currentTaskInsignia = doc.getString("motivationInsignia") ?: "Ninguna"

                    tvStatus.visibility = View.VISIBLE
                    tvTitulo.text = currentTaskTitle
                    tvDesc.text = currentTaskDesc
                    tvStatus.text = status.uppercase()

                    if (status.lowercase() == "hecha" || status.lowercase() == "completada") {
                        tvStatus.setTextColor(Color.parseColor("#1B5E20"))
                        tvStatus.setBackgroundColor(Color.parseColor("#C8E6C9"))
                        btnAbrir.visibility = View.GONE // Si ya la hizo, ocultamos el botón
                    } else {
                        tvStatus.setTextColor(Color.parseColor("#B71C1C"))
                        tvStatus.setBackgroundColor(Color.parseColor("#FFCDD2"))
                        btnAbrir.visibility = View.VISIBLE // Si está pendiente, mostramos el botón
                    }
                }
            }

        // Evento click del botón
        btnAbrir.setOnClickListener {
            mostrarDialogoTarea()
        }
    }

    private fun mostrarDialogoTarea() {
        // Inflamos nuestro diseño de Dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_completar_tarea, null)
        val dialogBuilder = android.app.AlertDialog.Builder(this)
            .setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Llenamos los datos
        dialogView.findViewById<TextView>(R.id.dialogTituloTarea).text = currentTaskTitle
        dialogView.findViewById<TextView>(R.id.dialogDescTarea).text = currentTaskDesc
        dialogView.findViewById<TextView>(R.id.dialogInsigniaTarea).text = "🏅 $currentTaskInsignia"

        // Botón Cerrar
        dialogView.findViewById<AppCompatButton>(R.id.btnCerrarDialogTarea).setOnClickListener {
            alertDialog.dismiss()
        }

        // Botón Completar
        dialogView.findViewById<AppCompatButton>(R.id.btnMarcarCompletada).setOnClickListener {
            marcarTareaComoHecha(alertDialog)
        }

        alertDialog.show()
    }

    private fun marcarTareaComoHecha(dialog: android.app.AlertDialog) {
        if (currentTaskId.isEmpty()) return

        // Actualizamos el estado en Firebase a "Hecha"
        db.collection("tasks").document(currentTaskId)
            .update("status", "Hecha")
            .addOnSuccessListener {
                Toast.makeText(this, "¡Felicidades! Tarea completada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Recargamos la tarjeta para que se ponga verde y desaparezca el botón
                cargarUltimaTarea()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun irAPerfil() {
        val intent = Intent(this, PerfilAlumnoActivity::class.java)
        intent.putExtra("STUDENT_ID", studentId)
        startActivity(intent)
    }
}