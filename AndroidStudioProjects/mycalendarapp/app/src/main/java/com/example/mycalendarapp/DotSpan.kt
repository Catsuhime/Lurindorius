package com.example.mycalendarapp
//DotSpan

import android.graphics.Canvas
import android.graphics.Paint
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class DotSpan(private val radius: Float, private val color: Int) : DotSpan(radius, color) {
    fun draw(canvas: Canvas, paint: Paint, x: Float, y: Float) {
        paint.color = color
        canvas.drawCircle(x, y, radius, paint)
    }
}
