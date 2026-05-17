package com.example.academiam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PartituraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Pinceles (Colores y grosores)
    private val paintLineas = Paint().apply { color = Color.BLACK; strokeWidth = 5f; isAntiAlias = true }
    private val paintNotaPendiente = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL; isAntiAlias = true }
    private val paintNotaAcierto = Paint().apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.FILL; isAntiAlias = true } // Verde
    private val paintNotaError = Paint().apply { color = Color.RED; style = Paint.Style.FILL; isAntiAlias = true } // Rojo
    private val paintNotaActual = Paint().apply { color = Color.parseColor("#FF9800"); style = Paint.Style.FILL; isAntiAlias = true } // Naranja

    private var notas = listOf<String>()
    private var estados = mutableListOf<Int>() // 0: Pendiente, 1: Acierto, 2: Error
    private var indiceActual = 0

    // Medidas para el dibujo
    private val separacionNotas = 180f
    private val paddingHorizontal = 300f // Espacio antes de empezar a dibujar notas

    fun setNotas(nuevasNotas: List<String>) {
        notas = nuevasNotas
        estados = MutableList(notas.size) { 0 }
        indiceActual = 0
        requestLayout() // Recalcula el ancho de la vista
        invalidate() // Obliga a redibujar
    }

    fun marcarAcierto(indice: Int) {
        if (indice in estados.indices) estados[indice] = 1
        indiceActual = indice + 1
        invalidate()
    }

    fun marcarError(indice: Int) {
        if (indice in estados.indices) estados[indice] = 2
        invalidate()
    }

    fun reiniciar() {
        estados = MutableList(notas.size) { 0 }
        indiceActual = 0
        invalidate()
    }

    // Calculamos el ancho de la pantalla dinámicamente según la cantidad de notas
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (paddingHorizontal * 2 + notas.size * separacionNotas).toInt()
        val desiredHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec), desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (notas.isEmpty()) return

        val alturaTotal = height.toFloat()
        val espaciadoLineas = alturaTotal / 12f
        val centroY = alturaTotal / 2f

        // 1. Dibujar 5 líneas del pentagrama
        val inicioPentagramaY = centroY - (espaciadoLineas * 2)
        for (i in 0..4) {
            val y = inicioPentagramaY + (i * espaciadoLineas)
            canvas.drawLine(0f, y, width.toFloat(), y, paintLineas)
        }

        // 2. Mapeo Matemático de Notas (Do=C4 hasta Do=C5)
        // La línea inferior del pentagrama es E4 (Mi)
        val yMi = inicioPentagramaY + (4 * espaciadoLineas)
        val pasoY = espaciadoLineas / 2f // Mitad de espacio entre cada nota (Línea-Espacio)

        // Posiciones relativas a "E": C=-2, D=-1, E=0, F=1, G=2, A=3, B=4
        val mapaPosiciones = mapOf("C" to -2, "D" to -1, "E" to 0, "F" to 1, "G" to 2, "A" to 3, "B" to 4)

        var esOctavaAlta = false

        for ((i, notaStr) in notas.withIndex()) {
            val x = paddingHorizontal + (i * separacionNotas)
            val notaLimpia = notaStr.replace("#", "") // Quitamos el sostenido temporalmente para la posición Y

            // Lógica sencilla para diferenciar el C grave del C agudo
            if (notaLimpia == "C" && i > 0 && (notas[i-1].contains("B") || notas[i-1].contains("A") || esOctavaAlta)) {
                esOctavaAlta = true
            }
            if (notaLimpia == "C" && i == 0) esOctavaAlta = false

            var posicion = mapaPosiciones[notaLimpia] ?: 0
            if (notaLimpia == "C" && esOctavaAlta) posicion = 5 // C5 (Do agudo) en el tercer espacio

            val y = yMi - (posicion * pasoY)

            // 3. Seleccionar el color de la nota actual
            val pincel = when {
                estados[i] == 1 -> paintNotaAcierto
                estados[i] == 2 -> paintNotaError
                i == indiceActual -> paintNotaActual
                else -> paintNotaPendiente
            }

            // 4. Dibujar la bolita (Nota)
            val radioNota = pasoY * 0.9f
            canvas.drawCircle(x, y, radioNota, pincel)

            // 5. Dibujar línea adicional si es C4 (Do central)
            if (posicion == -2) {
                canvas.drawLine(x - radioNota * 2, y, x + radioNota * 2, y, paintLineas)
            }

            // 6. Escribir el nombre de la nota adentro de la bolita
            val paintTexto = Paint().apply { color = Color.WHITE; textSize = radioNota; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText(notaLimpia, x, y + (radioNota / 3), paintTexto)
        }
    }
}