package com.vika.quest.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    private val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), QuestDatabase::class.java)
    @Test fun preservesGoalAndQuest() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("INSERT INTO goals (id,name,description,priority,createdAt) VALUES ('g','目标','描述',1,1)")
            execSQL("INSERT INTO quests (id,goalId,skillId,chainId,title,instruction,estimatedMinutes,completionCriteria,difficulty,status,createdAt,completedAt) VALUES ('q','g',NULL,NULL,'旧任务','旧说明',15,'[\"完成\"]',2,'ACTIVE',2,NULL)")
            close()
        }
        helper.runMigrationsAndValidate("migration-test", 2, true, QuestDatabase.MIGRATION_1_2).use { db ->
            db.query("SELECT goalId,title,sourceAvailableMinutes,status FROM quests WHERE id='q'").use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals("g", cursor.getString(0)); assertEquals("旧任务", cursor.getString(1)); assertEquals(15, cursor.getInt(2)); assertEquals("ACTIVE", cursor.getString(3)) }
        }
    }
}
