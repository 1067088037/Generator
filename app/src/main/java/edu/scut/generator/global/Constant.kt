package edu.scut.generator.global

import edu.scut.generator.ui.main.GeneratorItem

object Constant {

    val defaultGeneratorList = arrayListOf(
        GeneratorItem(
            id = 0,
            iconId = 0,
            name = "1号发电机",
            state = GeneratorItem.stateToString(GeneratorState.Unknown),
            power = 0.0,
            temperatureDifference = 0.0,
            rev = 0
        ),
        GeneratorItem(
            id = 1,
            iconId = 0,
            name = "2233号发电机",
            state = GeneratorItem.stateToString(GeneratorState.Stopped),
            power = 0.0,
            temperatureDifference = 10.0,
            rev = 120
        ),
        GeneratorItem(
            id = 2,
            iconId = 0,
            name = "123456号发电机",
            state = GeneratorItem.stateToString(GeneratorState.Running),
            power = 0.0,
            temperatureDifference = 100.0,
            rev = 2400
        )
    )

}