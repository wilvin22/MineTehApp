package com.example.mineteh.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.bumptech.glide.Glide
import com.example.mineteh.R
import com.google.android.material.imageview.ShapeableImageView

object AvatarUtils {

    fun getInitials(firstName: String, lastName: String): String {
        val f = firstName.firstOrNull()?.uppercaseChar() ?: ""
        val l = lastName.firstOrNull()?.uppercaseChar() ?: ""
        return "$f$l"
    }

    fun getAvatarColor(context: Context, accountId: Int): Int {
        val colorRes = when (accountId % 6) {
            0 -> R.color.avatar_purple
            1 -> R.color.avatar_teal
            2 -> R.color.avatar_orange
            3 -> R.color.avatar_blue
            4 -> R.color.avatar_green
            else -> R.color.avatar_red
        }
        return context.getColor(colorRes)
    }

    fun bindAvatar(
        view: ShapeableImageView,
        firstName: String,
        lastName: String,
        accountId: Int,
        avatarUrl: String?,
        context: Context
    ) {
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(avatarUrl)
                .placeholder(R.drawable.dummyphoto)
                .error(R.drawable.dummyphoto)
                .into(view)
        } else {
            val initials = getInitials(firstName, lastName)
            val bgColor = getAvatarColor(context, accountId)
            val bitmap = drawInitialsBitmap(initials, bgColor)
            view.setImageBitmap(bitmap)
        }
    }

    private fun drawInitialsBitmap(initials: String, bgColor: Int): Bitmap {
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = bgColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE
        paint.textSize = size * 0.35f
        paint.textAlign = Paint.Align.CENTER
        val textBounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, textBounds)
        val y = size / 2f - textBounds.exactCenterY()
        canvas.drawText(initials, size / 2f, y, paint)

        return bitmap
    }
}
