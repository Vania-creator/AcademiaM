package com.example.academiam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialTareasActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var studentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_tareas)

        studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        val studentName = intent.getStringExtra("STUDENT_NAME") ?: "Alumno"

        findViewById<TextView>(R.id.txtTituloHistorialTareas).text = "Tareas: $studentName"
        findViewById<AppCompatButton>(R.id.btnRegresarHistorialT).setOnClickListener { finish() }

        cargarTareas()
    }

    private fun cargarTareas() {
        val container = findViewById<LinearLayout>(R.id.containerTareasList)

        db.collection("tasks")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                container.removeAllViews()

                for (doc in query) {
                    val view = LayoutInflater.from(this).inflate(R.layout.item_tarea_historial, container, false)

                    val taskId = doc.id
                    val status = doc.getString("status") ?: "Pendiente"
                    val titulo = doc.getString("title") ?: "Sin título"
                    val desc = doc.getString("description") ?: "Sin descripción"
                    val insignia = doc.getString("motivationInsignia") ?: "Ninguna"

                    val tvStatus = view.findViewById<TextView>(R.id.txtStatusItem)
                    val btnAbrir = view.findViewById<AppCompatButton>(R.id.btnAbrirTareaItem)

                    tvStatus.text = status.uppercase()

                    if (status.lowercase() == "hecha" || status.lowercase() == "completada") {
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                        btnAbrir.visibility = View.GONE
                    } else {
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFCDD2"))
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                        btnAbrir.visibility = View.VISIBLE
                    }

                    view.findViewById<TextView>(R.id.txtTituloTareaItem).text = titulo
                    view.findViewById<TextView>(R.id.txtFechaTareaItem).text = "Asignada el: ${doc.getString("dateAssigned")}"
                    view.findViewById<TextView>(R.id.txtDescTareaItem).text = desc

                    // Al presionar el botón de la tarea específica
                    btnAbrir.setOnClickListener {
                        mostrarDialogoTarea(taskId, titulo, desc, insignia)
                    }

                    container.addView(view)
                }
            }
    }

    private fun mostrarDialogoTarea(taskId: String, titulo: String, desc: String, insignia: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_completar_tarea, null)
        val dialogBuilder = android.app.AlertDialog.Builder(this).setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.dialogTituloTarea).text = titulo
        dialogView.findViewById<TextView>(R.id.dialogDescTarea).text = desc
        dialogView.findViewById<TextView>(R.id.dialogInsigniaTarea).text = "🏅 $insignia"

        dialogView.findViewById<AppCompatButton>(R.id.btnCerrarDialogTarea).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<AppCompatButton>(R.id.btnMarcarCompletada).setOnClickListener {
            marcarTareaComoHecha(taskId, alertDialog)
        }

        alertDialog.show()
    }

    private fun marcarTareaComoHecha(taskId: String, dialog: android.app.AlertDialog) {
        db.collection("tasks").document(taskId)
            .update("status", "Hecha")
            .addOnSuccessListener {
                Toast.makeText(this, "¡Tarea completada!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                cargarTareas() // Recarga la lista para que la tarjeta se actualice a verde
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }
}