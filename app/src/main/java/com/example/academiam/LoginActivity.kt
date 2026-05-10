package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    // Inicializamos la instancia de Firestore
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtError = findViewById<TextView>(R.id.txtError)

        btnLogin.setOnClickListener {
            val usuarioInput = etUsuario.text.toString().trim()
            val contrasenaInput = etContrasena.text.toString().trim()

            if (usuarioInput.isEmpty() || contrasenaInput.isEmpty()) {
                txtError.text = "Completa todos los campos"
                txtError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            txtError.visibility = View.GONE

            // 1. Intentar loguear como Maestro
            buscarEnColeccion("teachers", usuarioInput, contrasenaInput, MenuMaestroActivity::class.java, txtError)
        }
    }

    private fun <T> buscarEnColeccion(
        coleccion: String,
        usuario: String,
        pass: String,
        destino: Class<T>,
        txtError: TextView
    ) {
        db.collection(coleccion)
            .whereEqualTo("usuario", usuario)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val passDB = doc.getString("password")
                    val nombreReal = doc.getString("nombre") ?: "Alumno" // Sacamos el nombre de Firestore

                    if (passDB == pass) {
                        // --- LOGICA DE LOGIN EXITOSO ---
                        if (coleccion == "teachers") {
                            val intent = Intent(this, MenuMaestroActivity::class.java)
                            intent.putExtra("TEACHER_ID", doc.id)
                            startActivity(intent)
                            finish()
                        } else {
                            // --- LOGIN ALUMNO ---
                            val intent = Intent(this, MenuAlumnoActivity::class.java)
                            // Pasamos el ID del alumno
                            intent.putExtra("STUDENT_ID", doc.id)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        txtError.text = "Contraseña incorrecta"
                        txtError.visibility = View.VISIBLE
                    }
                } else {
                    // Si no lo encuentra en maestros, busca en alumnos
                    if (coleccion == "teachers") {
                        buscarEnColeccion("students", usuario, pass, PerfilAlumnoActivity::class.java, txtError)
                    } else {
                        txtError.text = "El usuario no existe"
                        txtError.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener {
                ToastHelper.mostrarMensaje(this, "Error de conexión con la base de datos")
            }
    }
}