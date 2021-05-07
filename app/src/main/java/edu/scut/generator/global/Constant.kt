package edu.scut.generator.global

import edu.scut.generator.ui.main.GeneratorItem

object Constant {
    const val requestPermissionCode = 0x0
    const val startBluetoothCode = 0x1

    //需要申请的权限
    val needPermissions = arrayOf(
        "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION"
    )
    val defaultGeneratorList = arrayListOf(
        GeneratorItem(
            name = "1号发电机",
            state = GeneratorState.Running,
            power = 18.0,
            temperatureDifference = 8.5,
            rev = 600
        ),
        GeneratorItem(
            name = "2号发电机",
            state = GeneratorState.Paused,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            name = "3号发电机",
            state = GeneratorState.Disconnected,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            name = "4号发电机",
            state = GeneratorState.Disabled,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            name = "5号发电机",
            state = GeneratorState.Unknown,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        )
    )

}