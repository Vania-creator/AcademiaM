package com.example.academiam

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialTareasActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_tareas)

        val studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        val studentName = intent.getStringExtra("STUDENT_NAME") ?: "Alumno"

        findViewById<TextView>(R.id.txtTituloHistorialTareas).text = "Tareas: $studentName"
        findViewById<AppCompatButton>(R.id.btnRegresarHistorialT).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.containerTareasList)

        db.collection("tasks")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                container.removeAllViews()
                for (doc in query) {
                    val view = LayoutInflater.from(this).inflate(R.layout.item_tarea_historial, container, false)

                    val status = doc.getString("status") ?: "Pendiente"
                    val tvStatus = view.findViewById<TextView>(R.id.txtStatusItem)

                    tvStatus.text = status.uppercase()
                    if (status.lowercase() == "hecha") {
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    } else {
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFCDD2"))
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                    }

                    view.findViewById<TextView>(R.id.txtTituloTareaItem).text = doc.getString("title")
                    view.findViewById<TextView>(R.id.txtFechaTareaItem).text = "Asignada el: ${doc.getString("dateAssigned")}"
                    view.findViewById<TextView>(R.id.txtDescTareaItem).text = doc.getString("description")

                    container.addView(view)
                }
            }
    }
}