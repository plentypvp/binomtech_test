package com.pm.binomtech_test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.location.Location
import android.util.TypedValue
import androidx.core.content.ContextCompat


object Utils {
    fun locationByLatLng(lat: Double, lng: Double): Location {
        val res = Location("")
        res.latitude = lat
        res.longitude = lng
        return res
    }

    fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(bitmap.width / 2.0f, bitmap.height / 2.0f, bitmap.width / 2.0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    fun combineBitmaps(bg: Bitmap, fg: Bitmap, rectOffset: Rect): Bitmap {
        val output = Bitmap.createBitmap(bg.width, bg.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        canvas.drawBitmap(bg, Rect(0, 0, bg.width, bg.height), Rect(0, 0, bg.width, bg.height), paint)
        canvas.drawBitmap(fg, Rect(0, 0, fg.width, fg.height), rectOffset, paint)
        return output
    }

    fun getBitmap(vectorDrawable: VectorDrawable): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    fun getBitmap(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId)
        return if (drawable is BitmapDrawable) {
            BitmapFactory.decodeResource(context.resources, drawableId)
        } else if (drawable is VectorDrawable) {
            getBitmap(drawable)
        } else {
            throw IllegalArgumentException("unsupported drawable type")
        }
    }

    fun Context.toPx(dp: Int): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics)
}