package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class HorarioActivity : AppCompatActivity() {

    private lateinit var txtFechaHorario: TextView

    private val calendarioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val fechaSeleccionada = result.data?.getStringExtra("fechaSeleccionada")
                if (!fechaSeleccionada.isNullOrEmpty()) {
                    txtFechaHorario.text = fechaSeleccionada
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horario)

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarHorario)
        val btnCalendario = findViewById<AppCompatButton>(R.id.btnCalendario)
        txtFechaHorario = findViewById(R.id.txtFechaHorario)

        btnRegresar.setOnClickListener {
            finish()
        }

        btnCalendario.setOnClickListener {
            val intent = Intent(this, CalendarioActivity::class.java)
            calendarioLauncher.launch(intent)
        }
    }
}