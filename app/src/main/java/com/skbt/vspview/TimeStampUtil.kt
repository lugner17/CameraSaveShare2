package com.skbt.vspview

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*

object TimeStampUtil {

    fun addTimestamp(context: Context, uri: Uri) {
        try {
            val originalBitmap =
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            val newBitmap =
                originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

            val canvas = Canvas(newBitmap)
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }

            val text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            canvas.drawText(text, 40f, newBitmap.height - 60f, paint)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            originalBitmap.recycle()
            newBitmap.recycle()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
