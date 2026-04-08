package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MenuMaestroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_maestro)

        val btnMisAlumnos = findViewById<Button>(R.id.btnMisAlumnos)
        val btnHorario = findViewById<Button>(R.id.btnHorario)
        val btnReporteClase = findViewById<Button>(R.id.btnReporteClase)
        val btnTarea = findViewById<Button>(R.id.btnTarea)
        val btnSeleccionarLibro = findViewById<Button>(R.id.btnSeleccionarLibro)

        btnMisAlumnos.setOnClickListener {
            startActivity(Intent(this, MisAlumnosActivity::class.java))
        }

        btnHorario.setOnClickListener {
            startActivity(Intent(this, HorarioActivity::class.java))
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
    }
}