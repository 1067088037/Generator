package edu.scut.generator.ui.main

import android.graphics.Color
import edu.scut.generator.global.GeneratorState
import java.util.*

data class GeneratorItem(
    var id: UUID = UUID.randomUUID(),
    var iconId: Int = 0,
    var name: String = "Generator",
    var state: GeneratorState = GeneratorState.Unknown,
    var power: Double = 0.0,
    var temperatureDifference: Double = 0.0,
    var rev: Double = 0.0
) {

    companion object {
        fun stateToString(state: GeneratorState): String {
            return when (state) {
                GeneratorState.Running -> "正在运行"
                GeneratorState.Paused -> "暂停运行"
                GeneratorState.Disabled -> "失去功能"
                GeneratorState.Disconnected -> "连接断开"
                GeneratorState.Unknown -> "未知状态"
            }
        }

        fun getStateTextColor(state: GeneratorState): Int {
            return when (state) {
                GeneratorState.Running -> Color.rgb(48, 175, 56)
                GeneratorState.Paused -> Color.rgb(136, 136, 136)
                GeneratorState.Disabled -> Color.rgb(233, 30, 99)
                GeneratorState.Disconnected -> Color.rgb(232, 138, 0)
                GeneratorState.Unknown -> Color.rgb(3, 169, 244)
            }
        }

        /**
         * 编码发电机数组
         */
        fun encodeGeneratorArray(generators: Array<GeneratorItem>): String {
            var res = "["
            generators.forEach { res += encodeGenerator(it) }
            return "$res]"
        }

        /**
         * 解码发电机组
         */
        fun decodeGeneratorArray(string: String): Array<GeneratorItem> {
            val res = arrayListOf<GeneratorItem>()
            var content = string.substringAfter('[').substringBefore(']')
            while (content.isNotEmpty()) {
                res.add(decodeGenerator(content.substringBefore('}') + '}'))
                content = content.substringAfter('}')
            }
            return res.toTypedArray()
        }

        /**
         * 以字符串的形式编码发电机
         */
        private fun encodeGenerator(generatorItem: GeneratorItem): String {
            return "{${generatorItem.id},${"%.2f".format(generatorItem.power)}," +
                    "${"%.2f".format(generatorItem.temperatureDifference)}," +
                    "${"%.2f".format(generatorItem.rev)}}"
        }

        /**
         * 将字符串编码的发电机信息解码
         */
        private fun decodeGenerator(string: String): GeneratorItem {
            var content = string.substringAfter('{').substringBefore('}')
            val uuid = content.substringBefore(',')
            content = content.substringAfter(',')
            val power = content.substringBefore(',')
            content = content.substringAfter(',')
            val differT = content.substringBefore(',')
            content = content.substringAfter(',')
            val rev = content.substringBefore(',')
            return GeneratorItem(
                id = UUID.fromString(uuid),
                power = power.toDouble(),
                temperatureDifference = differT.toDouble(),
                rev = rev.toDouble()
            )
        }
    }

    override fun toString(): String {
        return "[#$id] $name: 状态=$state, 功率=$power, 温度差=$temperatureDifference, 转速=$rev"
    }

}