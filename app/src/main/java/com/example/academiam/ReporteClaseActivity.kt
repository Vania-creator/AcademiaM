package com.example.academiam

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import java.util.Calendar

class ReporteClaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte_clase)

        val txtErrorReporte = findViewById<TextView>(R.id.txtErrorReporte)
        val etAlumno = findViewById<EditText>(R.id.etAlumno)
        val etFecha = findViewById<EditText>(R.id.etFecha)
        val etTipoClase = findViewById<EditText>(R.id.etTipoClase)
        val etCuantoEstudio = findViewById<EditText>(R.id.etCuantoEstudio)
        val etNotasClase = findViewById<EditText>(R.id.etNotasClase)
        val cbAsistio = findViewById<CheckBox>(R.id.cbAsistio)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarReporte)
        val btnIngresar = findViewById<AppCompatButton>(R.id.btnIngresarReporte)

        txtErrorReporte.visibility = View.GONE

        etFecha.setOnClickListener {
            mostrarDatePicker(etFecha)
        }

        btnRegresar.setOnClickListener {
            finish()
        }

        btnIngresar.setOnClickListener {
            val alumno = etAlumno.text.toString().trim()
            val fecha = etFecha.text.toString().trim()
            val tipoClase = etTipoClase.text.toString().trim()
            val cuantoEstudio = etCuantoEstudio.text.toString().trim()
            val notasClase = etNotasClase.text.toString().trim()

            if (alumno.isEmpty() ||
                fecha.isEmpty() ||
                tipoClase.isEmpty() ||
                cuantoEstudio.isEmpty() ||
                notasClase.isEmpty()
            ) {
                txtErrorReporte.text = "Completa los campos"
                txtErrorReporte.visibility = View.VISIBLE
            } else {
                txtErrorReporte.visibility = View.GONE

                val mensajeAsistencia = if (cbAsistio.isChecked) {
                    "El alumno asistió"
                } else {
                    "El alumno no asistió"
                }

                Toast.makeText(
                    this,
                    "Reporte guardado. $mensajeAsistencia",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarDatePicker(etFecha: EditText) {
        val calendario = Calendar.getInstance()
        val year = calendario.get(Calendar.YEAR)
        val month = calendario.get(Calendar.MONTH)
        val day = calendario.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val fecha = "%02d/%02d/%04d".format(
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
                )
                etFecha.setText(fecha)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }
}