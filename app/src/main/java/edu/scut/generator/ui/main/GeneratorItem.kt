package edu.scut.generator.ui.main

import edu.scut.generator.global.GeneratorState

data class GeneratorItem(
    var id: Int = -1,
    var iconId: Int = 0,
    var name: String = "Generator",
    var state: GeneratorState = GeneratorState.Unknown,
    var power: Double = 0.0,
    var temperatureDifference: Double = 0.0,
    var rev: Int = 0
) {

    companion object {
        fun stateToString(state: GeneratorState): String {
            return when (state) {
                GeneratorState.Running -> "正在运行"
                GeneratorState.Stopped -> "停止运行"
                GeneratorState.Disabled -> "失去功能"
                GeneratorState.Disconnected -> "连接断开"
                GeneratorState.Unknown -> "未知状态"
            }
        }
    }

    override fun toString(): String {
        return "[#$id]$name: 状态=$state, 功率=$power, 温度差=$temperatureDifference, 转速=$rev"
    }

}