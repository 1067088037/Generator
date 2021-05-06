package edu.scut.generator.global

import android.util.Log

fun debug(any: Any) = debug("调试", any)

fun debug(tag: String, any: Any) = Log.d(tag, any.toString())