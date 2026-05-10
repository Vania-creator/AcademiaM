package com.example.academiam

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast

object ToastHelper {

    fun mostrarMensaje(context: Context, mensaje: String) {
        // Inflamos el diseño que ya creamos
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.layout_toast_personalizado, null)

        // Seteamos el texto
        val txt = layout.findViewById<TextView>(R.id.txtMensajeToast)
        txt.text = mensaje

        // Configuramos el Toast
        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout

        // Lo centramos abajo
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)

        toast.show()
    }
}