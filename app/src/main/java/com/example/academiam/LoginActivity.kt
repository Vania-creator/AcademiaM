package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtError = findViewById<TextView>(R.id.txtError)

        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                txtError.text = "Completa todos los campos"
                txtError.visibility = View.VISIBLE
            } else if (usuario != "admin" || contrasena != "1234") {
                txtError.text = "Usuario o contraseña incorrectos"
                txtError.visibility = View.VISIBLE
            } else {
                txtError.visibility = View.GONE
                startActivity(Intent(this, MenuMaestroActivity::class.java))
                finish()
            }
        }
    }
}