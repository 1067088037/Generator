package edu.scut.generator.global

import android.util.Log

fun debug(any: Any) = debug("调试", any)

fun debug(tag: String, any: Any) = Log.d(tag, any.toString())

fun prepareCommand(string: String): String {
    var command = string
    while (true) {
        var count = 0
        command.forEach { if (it == '\n') count++ }
        if (count <= Constant.MaxDebugCommandLine) break
        command = command.substringAfter('\n')
    }
    return command
}