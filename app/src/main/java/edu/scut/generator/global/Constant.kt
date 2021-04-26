package edu.scut.generator.global

import edu.scut.generator.ui.main.GeneratorItem

object Constant {

    val defaultGeneratorList = arrayListOf(
        GeneratorItem(
            id = 0,
            iconId = 0,
            name = "1号发电机",
            state = GeneratorState.Running,
            power = 18.0,
            temperatureDifference = 8.5,
            rev = 600
        ),
        GeneratorItem(
            id = 1,
            iconId = 0,
            name = "2号发电机",
            state = GeneratorState.Paused,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            id = 2,
            iconId = 0,
            name = "3号发电机",
            state = GeneratorState.Disconnected,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            id = 3,
            iconId = 0,
            name = "4号发电机",
            state = GeneratorState.Disabled,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            id = 4,
            iconId = 0,
            name = "5号发电机",
            state = GeneratorState.Unknown,
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
    )

}