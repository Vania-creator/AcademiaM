package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1. Ocultar la barra de estado para pantalla completa pro
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val layoutLogo = findViewById<LinearLayout>(R.id.layoutLogo)

        // 2. Cargar y ejecutar la animación "bonita"
        val animacion = AnimationUtils.loadAnimation(this, R.anim.fade_in_splash)
        layoutLogo.startAnimation(animacion)

        // 3. Esperar 3 segundos y pasar al Login
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Transición suave entre actividades
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // Cerramos el Splash para que no puedan regresar con el botón atrás
        }, 3000)
    }
}