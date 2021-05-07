package edu.scut.generator.global

enum class GeneratorState(val state: Int) {
    Running(0x0), //正在运行
    Paused(0x1), //停止运行
    Disabled(0x2), //失去功能
    Disconnected(0x3), //连接断开
    Unknown(0x4), //未知状态
}