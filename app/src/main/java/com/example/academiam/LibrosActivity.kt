package com.example.academiam

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class LibrosActivity : AppCompatActivity() {

    private var studentId: String = "" // 🔥 Variable para guardar el ID

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libros)

        // 🔥 RECUPERAMOS EL ID DEL ALUMNO DESDE EL MENÚ
        studentId = intent.getStringExtra("STUDENT_ID") ?: ""

        val itemLibro1 = findViewById<LinearLayout>(R.id.itemLibro1)
        val itemLibro2 = findViewById<LinearLayout>(R.id.itemLibro2)
        val itemLibro3 = findViewById<LinearLayout>(R.id.itemLibro3)
        val itemLibro4 = findViewById<LinearLayout>(R.id.itemLibro4)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarLibros)

        itemLibro1.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("C", "D", "E", "F", "G", "A", "B", "C"),
                "Actividad 1: Escala Completa",
                R.drawable.partirura1 // Cambia esto por el nombre de tu imagen
            )
        }

        itemLibro2.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("E", "E", "F", "G", "G", "F", "E", "D", "C", "C", "D", "E", "E", "D", "D"),
                "Actividad 2: Principiantes",
                R.drawable.partitura2 // Cambia esto por el nombre de tu imagen
            )
        }

        itemLibro3.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("C", "C", "G", "G", "A", "A", "G", "F", "F", "E", "E", "D", "D", "C"),
                "Actividad 3: Canción Sencilla",
                R.drawable.partitura3 // Cambia esto por el nombre de tu imagen
            )
        }

        itemLibro4.setOnClickListener {
            abrirSecuenciaDeAudio(
                arrayListOf("B", "B", "B", "B", "B", "G", "A", "B"),
                "Actividad 4: Rítmica Avanzada",
                R.drawable.partirura4 // Pon aquí el nombre de la imagen que me enviaste
            )
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun abrirSecuenciaDeAudio(secuencia: ArrayList<String>, titulo: String, imagenResId: Int) {
        try {
            val intent = Intent(this, PartituraInteractivaActivity::class.java)
            intent.putExtra("TITULO_CANCION", titulo)
            intent.putStringArrayListExtra("SECUENCIA_NOTAS", secuencia)
            intent.putExtra("IMAGEN_PARTITURA", imagenResId)
            intent.putExtra("STUDENT_ID", studentId) // 🔥 SE LO PASAMOS A LA PARTITURA
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastHelper.mostrarMensaje(this, "Error: Revisa que PartituraInteractivaActivity esté en el Manifest")
        }
    }
}