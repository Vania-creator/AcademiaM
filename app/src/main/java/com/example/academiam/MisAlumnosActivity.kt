package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class MisAlumnosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_alumnos)

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresar)
        val itemAlumno1 = findViewById<LinearLayout>(R.id.itemAlumno1)

        btnRegresar.setOnClickListener {
            finish()
        }

        itemAlumno1.setOnClickListener {
            val intent = Intent(this, PerfilAlumnoActivity::class.java)
            startActivity(intent)
        }
    }
}