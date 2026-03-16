package com.example.mineteh.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mineteh.R

/**
 * Extension functions for common UI operations
 */

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showConfirmDialog(
    title: String,
    message: String,
    positiveText: String = "OK",
    negativeText: String = "Cancel",
    onPositive: () -> Unit = {},
    onNegative: () -> Unit = {}
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveText) { _, _ -> onPositive() }
        .setNegativeButton(negativeText) { _, _ -> onNegative() }
        .show()
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}