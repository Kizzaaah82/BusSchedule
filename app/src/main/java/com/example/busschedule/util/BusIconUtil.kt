package com.example.busschedule.util

import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import com.example.busschedule.R
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set

fun createBusMarkerIcon(context: Context, @ColorInt routeColor: Int, bearing: Float?): Bitmap {
    val rawFiller = BitmapFactory.decodeResource(context.resources, R.drawable.bus_marker_filler)
    val rawOverlay = BitmapFactory.decodeResource(context.resources, R.drawable.bus_marker_overlay)

    val targetSize = 115
    val filler = rawFiller.scale(targetSize, targetSize)
    val overlay = rawOverlay.scale(targetSize, targetSize)

    val width = filler.width
    val height = filler.height

    val flipFiller = bearing != null && bearing in 90f..270f

    val coloredFiller = createBitmap(width, height)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = filler[x, y]

            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val a = Color.alpha(pixel)

            val isBubble = r > 200 && g > 200 && b > 200 && a > 200

            coloredFiller[x, y] = if (isBubble) routeColor else Color.TRANSPARENT
        }
    }

    val flippedFiller = if (flipFiller) {
        val matrix = Matrix().apply { preScale(-1f, 1f, width / 2f, height / 2f) }
        Bitmap.createBitmap(coloredFiller, 0, 0, width, height, matrix, true)
    } else {
        coloredFiller
    }

    val finalBitmap = createBitmap(width, height)
    val finalCanvas = Canvas(finalBitmap)
    finalCanvas.drawBitmap(flippedFiller, 0f, 0f, null)
    finalCanvas.drawBitmap(overlay, 0f, 0f, null)

    return finalBitmap
}