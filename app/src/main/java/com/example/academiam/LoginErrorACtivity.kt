package com.example.academiam

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LoginErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ViewUtils.hacerPantallaCompleta(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_error)
    }
}