package com.example.academiam

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class GrabacionesActivity : AppCompatActivity() {

    private lateinit var txtErrorCarga: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grabaciones)

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarGrabaciones)
        txtErrorCarga = findViewById(R.id.txtErrorCarga)


        txtErrorCarga.visibility = View.GONE

        btnRegresar.setOnClickListener {
            finish()
        }


        simularCarga()
    }


    private fun mostrarError(mensaje: String) {
        txtErrorCarga.text = mensaje
        txtErrorCarga.visibility = View.VISIBLE
    }


    private fun ocultarError() {
        txtErrorCarga.visibility = View.GONE
    }


    private fun simularCarga() {

        val listaGrabaciones = listOf<String>()

        if (listaGrabaciones.isEmpty()) {
            mostrarError("No hay grabaciones disponibles")
        } else {
            ocultarError()
        }
    }
}