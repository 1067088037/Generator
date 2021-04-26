package edu.scut.generator.global

enum class GeneratorState {
    Running, //正在运行
    Paused, //停止运行
    Disabled, //失去功能
    Disconnected, //连接断开
    Unknown, //未知状态
}