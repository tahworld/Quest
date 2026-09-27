package com.vika.quest.model

enum class DirectionChoice(val label: String, val hint: String) {
    MOVEMENT("身体活动", "例如：下班后恢复轻量运动"),
    READING("阅读/学习", "例如：读完手头的《刻意练习》并做笔记"),
    PROJECT("推进项目", "例如：验证 Quest 的行动生成体验"),
    CUSTOM("自定义方向", "写下你真正想推进的事情"),
}
