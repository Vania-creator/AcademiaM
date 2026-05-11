package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Modelo actualizado con instrumento y notas
data class ClaseEvento(
    val studentId: String,
    val nombre: String,
    val hora: String,
    val tipo: String,
    val instrumento: String,
    val nota: String
)

class HorarioActivity : AppCompatActivity() {

    private lateinit var txtFechaHorario: TextView
    private lateinit var containerClases: LinearLayout
    private val db = FirebaseFirestore.getInstance()
    private var teacherId: String = ""
    private val listaEventosDia = mutableListOf<ClaseEvento>()

    private val calendarioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val fechaSeleccionada = result.data?.getStringExtra("fechaSeleccionada")
                if (!fechaSeleccionada.isNullOrEmpty()) {
                    txtFechaHorario.text = fechaSeleccionada
                    procesarCargaDeHorario(fechaSeleccionada)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horario)

        teacherId = intent.getStringExtra("TEACHER_ID") ?: ""
        txtFechaHorario = findViewById(R.id.txtFechaHorario)
        containerClases = findViewById(R.id.containerClasesDia)
        val btnRegresar = findViewById<AppCompatButton>(R.id.btnRegresarHorario)
        val btnCalendario = findViewById<AppCompatButton>(R.id.btnCalendario)

        btnRegresar.setOnClickListener { finish() }
        btnCalendario.setOnClickListener {
            val intent = Intent(this, CalendarioActivity::class.java)
            calendarioLauncher.launch(intent)
        }

        val hoy = SimpleDateFormat("EEEE d MMMM yyyy", Locale("es", "MX")).format(Date())
        txtFechaHorario.text = hoy
        procesarCargaDeHorario(hoy)
    }

    private fun procesarCargaDeHorario(fechaTexto: String) {
        listaEventosDia.clear()
        containerClases.removeAllViews()

        val diaSemana = obtenerDiaAbreviado(fechaTexto)
        val fechaFormatoDB = convertirFechaADB(fechaTexto)
        var consultasFinalizadas = 0

        val checkFinalizar = {
            consultasFinalizadas++
            if (consultasFinalizadas == 2) {
                listaEventosDia.sortBy { it.hora }
                dibujarClasesEnPantalla()
            }
        }

        // Consultar clases fijas y eventos especiales
        val queryFijas = db.collection("classes").whereEqualTo("teacherId", teacherId).whereEqualTo("dayOfWeek", diaSemana).whereEqualTo("type", "fija")
        val queryEspeciales = db.collection("classes").whereEqualTo("teacherId", teacherId).whereEqualTo("date", fechaFormatoDB)

        queryFijas.get().addOnSuccessListener { query ->
            for (doc in query) {
                listaEventosDia.add(crearEventoDesdeDoc(doc))
            }
            checkFinalizar()
        }

        queryEspeciales.get().addOnSuccessListener { query ->
            for (doc in query) {
                if (doc.getString("type") != "fija") {
                    listaEventosDia.add(crearEventoDesdeDoc(doc))
                }
            }
            checkFinalizar()
        }
    }

    private fun crearEventoDesdeDoc(doc: com.google.firebase.firestore.DocumentSnapshot): ClaseEvento {
        return ClaseEvento(
            doc.getString("studentId") ?: "",
            doc.getString("studentName") ?: "Sin nombre",
            doc.getString("time") ?: "00:00",
            doc.getString("type") ?: "fija",
            doc.getString("instrument") ?: "N/A",
            doc.getString("note") ?: ""
        )
    }

    private fun dibujarClasesEnPantalla() {
        containerClases.removeAllViews()
        for (clase in listaEventosDia) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_clase_horario, containerClases, false)

            view.findViewById<TextView>(R.id.txtNombreAlumnoH).text = clase.nombre
            view.findViewById<TextView>(R.id.txtInstrumentoH).text = "(${clase.instrumento})"
            view.findViewById<TextView>(R.id.txtHoraH).text = clase.hora

            val txtNotas = view.findViewById<TextView>(R.id.txtNotasH)
            if (clase.nota.isNotEmpty()) {
                txtNotas.text = "Nota: ${clase.nota}"
            } else {
                txtNotas.text = "Sin notas de clase"
            }

            val txtTipo = view.findViewById<TextView>(R.id.txtTipoClaseH)
            txtTipo.text = clase.tipo.replaceFirstChar { it.uppercase() }
            if (clase.tipo.lowercase() == "reposición") txtTipo.setTextColor(android.graphics.Color.parseColor("#D32F2F"))

            // 🔥 CARGAMOS EL AVATAR DINÁMICO 🔥
            val imgAvatar = view.findViewById<ImageView>(R.id.imgAvatarHorario)
            if (imgAvatar != null) {
                // Hacemos una consulta rápida a Firebase para sacar su avatar
                db.collection("students").document(clase.studentId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val avatarName = doc.getString("avatar") ?: "logo"
                            val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                            if (resId != 0) {
                                imgAvatar.setImageResource(resId)
                            } else {
                                imgAvatar.setImageResource(R.drawable.logo)
                            }
                        }
                    }
                    .addOnFailureListener {
                        imgAvatar.setImageResource(R.drawable.logo)
                    }
            }

            view.setOnClickListener {
                val intent = Intent(this, PerfilAlumnoActivity::class.java)
                intent.putExtra("STUDENT_ID", clase.studentId)
                startActivity(intent)
            }
            containerClases.addView(view)
        }
    }

    private fun obtenerDiaAbreviado(fechaTexto: String): String {
        val f = fechaTexto.lowercase()
        return when {
            f.contains("lunes") -> "Lun"
            f.contains("martes") -> "Mar"
            f.contains("miércoles") || f.contains("miercoles") -> "Mie"
            f.contains("jueves") -> "Jue"
            f.contains("viernes") -> "Vie"
            f.contains("sábado") || f.contains("sabado") -> "Sab"
            else -> "Dom"
        }
    }

    private fun convertirFechaADB(fechaTexto: String): String {
        return try {
            val formatEntrada = SimpleDateFormat("EEEE d MMMM yyyy", Locale("es", "MX"))
            val date = formatEntrada.parse(fechaTexto)
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date!!)
        } catch (e: Exception) { "" }
    }
}