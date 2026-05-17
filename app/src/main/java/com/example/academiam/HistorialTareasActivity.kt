package com.example.academiam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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

    // 🔥 EL MISMO DICCIONARIO QUE USAMOS PARA ASIGNAR (Para calcular la XP a dar)
    private val insigniasTarea = mapOf(
        "Tiempo Invertido" to Pair("tiempoinvertido", listOf(160L, 360L, 480L)),
        "En Marcha" to Pair("enmarcha", listOf(80L, 160L, 240L)),
        "Teclas Maestras" to Pair("teclasmaestras", listOf(120L, 240L, 360L)),
        "Formación Sólida" to Pair("formacionsolida", listOf(180L, 360L, 450L))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
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
        // Buscamos el contenedor del estado vacío que agregamos en el XML
        val layoutTareasVacio = findViewById<LinearLayout>(R.id.layoutTareasVacio)

        db.collection("tasks")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { query ->
                container.removeAllViews()

                // VISUAL: Controlamos si se muestra el letrero bonito o la lista de tareas
                if (query.isEmpty) {
                    layoutTareasVacio?.visibility = View.VISIBLE
                } else {
                    layoutTareasVacio?.visibility = View.GONE
                }

                for (doc in query) {
                    val view = LayoutInflater.from(this).inflate(R.layout.item_tarea_historial, container, false)

                    val taskId = doc.id
                    val status = doc.getString("status") ?: "Pendiente"
                    val titulo = doc.getString("title") ?: "Sin título"
                    val desc = doc.getString("description") ?: "Sin descripción"

                    // 🔥 Rescatamos la llave y el nombre legible de la insignia prometida
                    val nombreInsignia = doc.getString("motivationInsigniaName") ?: "Ninguna"
                    val claveInsignia = doc.getString("motivationInsigniaKey") ?: "ninguna"

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

                    // Al presionar "Ver y Completar"
                    btnAbrir.setOnClickListener {
                        mostrarDialogoTarea(taskId, titulo, desc, nombreInsignia, claveInsignia)
                    }

                    container.addView(view)
                }
            }
    }

    private fun mostrarDialogoTarea(taskId: String, titulo: String, desc: String, nombreInsignia: String, claveInsignia: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_completar_tarea, null)
        val dialogBuilder = android.app.AlertDialog.Builder(this).setView(dialogView)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.dialogTituloTarea).text = titulo
        dialogView.findViewById<TextView>(R.id.dialogDescTarea).text = desc

        // 🔥 CALCULAMOS Y PINTAMOS LA RECOMPENSA (Lógica previa a completar)
        val imgInsigniaDialog = dialogView.findViewById<ImageView>(R.id.imgInsigniaDialog)
        val txtInfoRecompensa = dialogView.findViewById<TextView>(R.id.txtInfoRecompensaDialog)
        val btnCompletar = dialogView.findViewById<AppCompatButton>(R.id.btnMarcarCompletada)

        // Verificamos en qué nivel va el alumno para saber qué imagen enseñarle y cuánta XP prometerle
        db.collection("students").document(studentId).get().addOnSuccessListener { doc ->
            if (doc.exists() && claveInsignia != "ninguna") {
                val nivelActual = doc.getLong("insignias_progreso.$claveInsignia") ?: 0L
                val nivelSiguiente = nivelActual + 1

                if (nivelSiguiente <= 3) {
                    // Calculamos la XP que le toca
                    val arreglosXP = insigniasTarea[nombreInsignia]?.second
                    val xpPrometida = arreglosXP?.getOrNull((nivelSiguiente - 1).toInt()) ?: 0L

                    // Construimos el nombre del PNG (Ej: "enmarcha2")
                    val nombreArchivo = "${claveInsignia}${nivelSiguiente}"
                    val imageResId = resources.getIdentifier(nombreArchivo, "drawable", packageName)

                    if (imageResId != 0) {
                        imgInsigniaDialog.setImageResource(imageResId)
                    } else {
                        imgInsigniaDialog.setImageResource(R.drawable.logo)
                    }

                    txtInfoRecompensa.text = "Recompensa:\n$nombreInsignia (Nivel $nivelSiguiente)\n+$xpPrometida XP"

                } else {
                    // Si ya es nivel 3, solo le damos las gracias, ya no gana esa medalla
                    imgInsigniaDialog.setImageResource(R.drawable.logo)
                    txtInfoRecompensa.text = "¡Ya tienes el nivel máximo de esta insignia!"
                }
            } else {
                imgInsigniaDialog.visibility = View.GONE
                txtInfoRecompensa.text = "Esta tarea no tiene insignia asignada."
            }
        }

        dialogView.findViewById<AppCompatButton>(R.id.btnCerrarDialogTarea).setOnClickListener {
            alertDialog.dismiss()
        }

        btnCompletar.setOnClickListener {
            marcarTareaYDarRecompensa(taskId, nombreInsignia, claveInsignia, alertDialog)
        }

        alertDialog.show()
    }

    private fun marcarTareaYDarRecompensa(taskId: String, nombreInsignia: String, claveInsignia: String, dialog: android.app.AlertDialog) {
        val docRefStudent = db.collection("students").document(studentId)
        val docRefTask = db.collection("tasks").document(taskId)

        db.runTransaction { transaction ->
            // 🔥 PASO 1: TODAS LAS LECTURAS (READS) PRIMERO
            var expActual = 0L
            var nivelActualInsignia = 0L

            if (claveInsignia != "ninguna") {
                val snapshot = transaction.get(docRefStudent) // Leemos al alumno
                expActual = snapshot.getLong("expTotal") ?: 0L
                nivelActualInsignia = snapshot.getLong("insignias_progreso.$claveInsignia") ?: 0L
            }

            // 🔥 PASO 2: TODAS LAS ESCRITURAS (WRITES) AL FINAL
            // Actualizamos la tarea
            transaction.update(docRefTask, "status", "Hecha")

            // Actualizamos al alumno (si aplica la recompensa)
            if (claveInsignia != "ninguna" && nivelActualInsignia < 3) {
                val nuevoNivelInsignia = nivelActualInsignia + 1
                val arreglosXP = insigniasTarea[nombreInsignia]?.second
                val xpGanada = arreglosXP?.getOrNull((nuevoNivelInsignia - 1).toInt()) ?: 0L

                val nuevaExpTotal = expActual + xpGanada
                val nuevoNivelGeneral = (nuevaExpTotal / 500).toInt()

                transaction.update(docRefStudent, "expTotal", nuevaExpTotal)
                transaction.update(docRefStudent, "nivel", nuevoNivelGeneral)
                transaction.update(docRefStudent, "insignias_progreso.$claveInsignia", nuevoNivelInsignia)
            }
        }.addOnSuccessListener {
            ToastHelper.mostrarMensaje(this, "¡Tarea completada y recompensa asignada!")
            dialog.dismiss()
            cargarTareas()
        }.addOnFailureListener { e ->
            ToastHelper.mostrarMensaje(this, "Error al completar: ${e.message}")
        }
    }
}