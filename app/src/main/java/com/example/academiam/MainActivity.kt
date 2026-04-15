package com.example.academiam

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 3000)
        // Prueba rápida de lectura
        val db = FirebaseFirestore.getInstance()
        db.collection("students").limit(1).get()
            .addOnSuccessListener {
                Log.d("FIREBASE_TEST", "¡Conexión exitosa! Leído con éxito.")
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_TEST", "Error al conectar", e)
            }
    }

}