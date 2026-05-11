package com.example.academiam

import android.view.View

object ViewUtils {
    fun hacerPantallaCompleta(window: android.view.Window) {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // Oculta barra de abajo
                or View.SYSTEM_UI_FLAG_FULLSCREEN)    // Oculta barra de arriba

        decorView.systemUiVisibility = uiOptions
    }
}