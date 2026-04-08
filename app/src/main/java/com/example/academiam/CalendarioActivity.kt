package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarioActivity : AppCompatActivity() {

    private var fechaParaMostrar = ""
    private var fechaCorta = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario)

        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarCalendario)
        val btnSeleccionarFecha = findViewById<AppCompatButton>(R.id.btnSeleccionarFecha)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val txtFechaSeleccionada = findViewById<TextView>(R.id.txtFechaSeleccionada)
        val txtFechaConfirmada = findViewById<TextView>(R.id.txtFechaConfirmada)

        val calendario = Calendar.getInstance()

        fechaCorta = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(calendario.time)
        fechaParaMostrar = formatearFechaLarga(calendario)

        txtFechaSeleccionada.text = "Fecha elegida: $fechaCorta"
        txtFechaConfirmada.text = "Fecha confirmada: ninguna"

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val fecha = Calendar.getInstance()
            fecha.set(year, month, dayOfMonth)

            fechaCorta = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX")).format(fecha.time)
            fechaParaMostrar = formatearFechaLarga(fecha)

            txtFechaSeleccionada.text = "Fecha elegida: $fechaCorta"
        }

        btnSeleccionarFecha.setOnClickListener {
            txtFechaConfirmada.text = "Fecha confirmada: $fechaCorta"
            Toast.makeText(this, "Fecha seleccionada: $fechaCorta", Toast.LENGTH_SHORT).show()

            val intent = Intent()
            intent.putExtra("fechaSeleccionada", fechaParaMostrar)
            setResult(RESULT_OK, intent)
            finish()
        }

        btnRegresar.setOnClickListener {
            finish()
        }
    }

    private fun formatearFechaLarga(calendar: Calendar): String {
        val dias = arrayOf(
            "Domingo", "Lunes", "Martes", "Miércoles",
            "Jueves", "Viernes", "Sábado"
        )

        val meses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        val diaSemana = dias[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val dia = calendar.get(Calendar.DAY_OF_MONTH)
        val mes = meses[calendar.get(Calendar.MONTH)]
        val anio = calendar.get(Calendar.YEAR)

        return "$diaSemana $dia $mes $anio"
    }
}