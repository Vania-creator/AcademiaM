package com.example.academiam

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class TareaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarea)

        val txtError = findViewById<TextView>(R.id.txtErrorTarea)
        val etAlumno = findViewById<EditText>(R.id.etAlumnoTarea)
        val etTitulo = findViewById<EditText>(R.id.etTitulo)
        val etNotas = findViewById<EditText>(R.id.etNotasTarea)

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarTarea)
        val btnIngresar = findViewById<AppCompatButton>(R.id.btnIngresarTarea)

        txtError.visibility = View.GONE

        btnRegresar.setOnClickListener {
            finish()
        }

        btnIngresar.setOnClickListener {
            val alumno = etAlumno.text.toString().trim()
            val titulo = etTitulo.text.toString().trim()
            val notas = etNotas.text.toString().trim()

            if (alumno.isEmpty() || titulo.isEmpty() || notas.isEmpty()) {
                txtError.visibility = View.VISIBLE
            } else {
                txtError.visibility = View.GONE
                Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}