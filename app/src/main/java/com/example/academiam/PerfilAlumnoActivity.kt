package com.example.academiam

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class PerfilAlumnoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_alumno)

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarPerfil)
        val btnGrabaciones = findViewById<AppCompatButton>(R.id.btnGrabaciones)

        btnRegresar.setOnClickListener {
            finish()
        }

        btnGrabaciones.setOnClickListener {
            val intent = Intent(this, GrabacionesActivity::class.java)
            startActivity(intent)
        }
    }
}