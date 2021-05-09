package edu.scut.generator.global

import edu.scut.generator.ui.main.GeneratorItem
import java.util.*

object Constant {
    const val requestPermissionCode = 0x0
    const val startBluetoothCode = 0x1

    const val MessageStartCode = '['.toByte()
    const val MessageStopCode = ']'.toByte()
    const val ObjectStartCode = '{'.toByte()
    const val ObjectStopCode = '}'.toByte()

    const val MaxPointNumberMeanwhile = 40
    const val MaxDebugCommandLine = 10

    const val DefaultDevice = "HC-05"

    //需要申请的权限
    val NeedPermissions = arrayOf(
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION"
    )
    val TempUUid: UUID = UUID.randomUUID()
    val DefaultGeneratorList = arrayListOf(
        GeneratorItem(
            name = "1号发电机",
            state = GeneratorState.Running,
            power = 18.0,
            temperatureDifference = 8.5,
            rev = 600.0
        ),
        GeneratorItem(
            name = "2号发电机",
            state = GeneratorState.Paused,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0.0
        ),
        GeneratorItem(
            name = "3号发电机",
            state = GeneratorState.Disconnected,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0.0
        )
    )

}