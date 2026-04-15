package com.example.academiam

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistorialReportesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_reportes)

        val studentId = intent.getStringExtra("STUDENT_ID") ?: ""
        val studentName = intent.getStringExtra("STUDENT_NAME") ?: "Alumno"

        findViewById<TextView>(R.id.txtTituloHistorial).text = "Historial: $studentName"
        findViewById<AppCompatButton>(R.id.btnRegresarHistorial).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.containerReportes)

        db.collection("reports")
            .whereEqualTo("studentId", studentId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                container.removeAllViews()
                if (query.isEmpty) {
                    val tv = TextView(this)
                    tv.text = "No hay historial disponible"
                    container.addView(tv)
                }
                for (doc in query) {
                    val view = LayoutInflater.from(this).inflate(R.layout.item_reporte_historial, container, false)

                    val fecha = doc.getString("date") ?: "--"
                    val tipo = doc.getString("tipoClase") ?: ""
                    val contenido = doc.getString("content") ?: ""
                    val asistio = if (doc.getBoolean("asistio") == true) "✅ Asistió" else "❌ Faltó"
                    val insignia = doc.getString("insignia") ?: "Ninguna"

                    view.findViewById<TextView>(R.id.txtFechaItem).text = "$fecha ($tipo)"
                    view.findViewById<TextView>(R.id.txtAsistenciaItem).text = "$asistio | Motivación: $insignia"
                    view.findViewById<TextView>(R.id.txtContenidoItem).text = contenido

                    container.addView(view)
                }
            }
    }
}