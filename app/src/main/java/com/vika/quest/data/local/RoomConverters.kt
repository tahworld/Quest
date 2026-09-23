package com.vika.quest.data.local

import androidx.room.TypeConverter
import com.vika.quest.model.QuestStatus
import org.json.JSONArray

class RoomConverters {
    @TypeConverter
    fun questStatusToString(status: QuestStatus): String = status.name

    @TypeConverter
    fun stringToQuestStatus(value: String): QuestStatus = QuestStatus.valueOf(value)

    @TypeConverter
    fun stringListToJson(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getString(index) }
    }
}
