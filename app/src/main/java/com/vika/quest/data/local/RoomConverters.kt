package com.vika.quest.data.local

import androidx.room.TypeConverter
import com.vika.quest.model.QuestStatus
import com.vika.quest.model.DifficultyRating
import com.vika.quest.model.ProjectStatus
import com.vika.quest.model.RejectionReason
import org.json.JSONArray

class RoomConverters {
    @TypeConverter
    fun questStatusToString(status: QuestStatus): String = status.name

    @TypeConverter
    fun stringToQuestStatus(value: String): QuestStatus = QuestStatus.valueOf(value)

    @TypeConverter fun projectStatusToString(value: ProjectStatus): String = value.name
    @TypeConverter fun stringToProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)
    @TypeConverter fun difficultyRatingToString(value: DifficultyRating?): String? = value?.name
    @TypeConverter fun stringToDifficultyRating(value: String?): DifficultyRating? = value?.let(DifficultyRating::valueOf)
    @TypeConverter fun rejectionReasonToString(value: RejectionReason): String = value.storageValue
    @TypeConverter fun stringToRejectionReason(value: String): RejectionReason = RejectionReason.entries.first { it.storageValue == value }

    @TypeConverter
    fun stringListToJson(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { index -> array.getString(index) }
    }
}
