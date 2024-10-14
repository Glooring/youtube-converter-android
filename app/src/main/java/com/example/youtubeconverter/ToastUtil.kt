package com.example.youtubeconverter

import android.content.Context
import android.widget.Toast

object ToastUtil {
    private var toast: Toast? = null

    fun showToast(context: Context, message: String) {
        toast?.cancel() // Cancel any existing Toast message
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG).apply {
            show()
        }
    }
}
