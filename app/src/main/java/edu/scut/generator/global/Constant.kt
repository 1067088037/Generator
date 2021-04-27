package edu.scut.generator.global

import edu.scut.generator.ui.main.GeneratorItem

object Constant {

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
        ),
    )

}