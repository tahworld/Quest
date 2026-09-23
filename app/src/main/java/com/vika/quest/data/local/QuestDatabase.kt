package com.vika.quest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vika.quest.data.local.dao.GoalDao
import com.vika.quest.data.local.dao.MemoryDao
import com.vika.quest.data.local.dao.ProjectDao
import com.vika.quest.data.local.dao.QuestDao
import com.vika.quest.data.local.dao.QuestRejectionDao
import com.vika.quest.data.local.dao.QuestResultDao
import com.vika.quest.data.local.dao.UserPreferenceDao
import com.vika.quest.data.local.entity.GoalEntity
import com.vika.quest.data.local.entity.MemoryEntity
import com.vika.quest.data.local.entity.ProjectEntity
import com.vika.quest.data.local.entity.QuestEntity
import com.vika.quest.data.local.entity.QuestRejectionEntity
import com.vika.quest.data.local.entity.QuestResultEntity
import com.vika.quest.data.local.entity.UserPreferenceEntity

@Database(
    entities = [GoalEntity::class, ProjectEntity::class, MemoryEntity::class, QuestEntity::class, QuestResultEntity::class, QuestRejectionEntity::class, UserPreferenceEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao

    abstract fun questDao(): QuestDao
    abstract fun projectDao(): ProjectDao
    abstract fun memoryDao(): MemoryDao
    abstract fun questResultDao(): QuestResultDao
    abstract fun questRejectionDao(): QuestRejectionDao
    abstract fun userPreferenceDao(): UserPreferenceDao

    companion object {
        const val DATABASE_NAME = "quest.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `projects` (`id` TEXT NOT NULL, `goalId` TEXT, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `currentState` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_goalId` ON `projects` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_updatedAt` ON `projects` (`updatedAt`)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `quests_new` (`id` TEXT NOT NULL, `goalId` TEXT, `projectId` TEXT, `skillId` TEXT, `chainId` TEXT, `title` TEXT NOT NULL, `reason` TEXT NOT NULL, `steps` TEXT NOT NULL, `instruction` TEXT NOT NULL, `estimatedMinutes` INTEGER NOT NULL, `completionCriteria` TEXT NOT NULL, `expectedOutput` TEXT NOT NULL, `difficulty` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, `sourceIntention` TEXT NOT NULL, `sourceAvailableMinutes` INTEGER NOT NULL, `sourceEnergy` INTEGER NOT NULL, `sourceResources` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("""INSERT INTO `quests_new` (`id`,`goalId`,`projectId`,`skillId`,`chainId`,`title`,`reason`,`steps`,`instruction`,`estimatedMinutes`,`completionCriteria`,`expectedOutput`,`difficulty`,`status`,`createdAt`,`completedAt`,`sourceIntention`,`sourceAvailableMinutes`,`sourceEnergy`,`sourceResources`) SELECT `id`,`goalId`,NULL,`skillId`,`chainId`,`title`,'','[]',`instruction`,`estimatedMinutes`,`completionCriteria`,'',`difficulty`,`status`,`createdAt`,`completedAt`,'',`estimatedMinutes`,3,'[]' FROM `quests`""")
                db.execSQL("DROP TABLE `quests`")
                db.execSQL("ALTER TABLE `quests_new` RENAME TO `quests`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quests_goalId` ON `quests` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quests_projectId` ON `quests` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quests_createdAt` ON `quests` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quests_status` ON `quests` (`status`)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `memories` (`id` TEXT NOT NULL, `goalId` TEXT, `projectId` TEXT, `type` TEXT NOT NULL, `content` TEXT NOT NULL, `importance` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_goalId` ON `memories` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_projectId` ON `memories` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_importance` ON `memories` (`importance`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_createdAt` ON `memories` (`createdAt`)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `quest_results` (`id` TEXT NOT NULL, `questId` TEXT NOT NULL, `resultText` TEXT NOT NULL, `actualMinutes` INTEGER, `difficultyRating` TEXT, `usefulnessRating` INTEGER, `createdAt` INTEGER NOT NULL, `analysisSummary` TEXT, `evidence` TEXT NOT NULL, `insights` TEXT NOT NULL, `projectProgress` TEXT, `suggestedNextStep` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_results_questId` ON `quest_results` (`questId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_results_createdAt` ON `quest_results` (`createdAt`)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `quest_rejections` (`id` TEXT NOT NULL, `questId` TEXT NOT NULL, `reason` TEXT NOT NULL, `details` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_rejections_questId` ON `quest_rejections` (`questId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_rejections_createdAt` ON `quest_rejections` (`createdAt`)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `user_preferences` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))""")
            }
        }

        fun create(context: Context): QuestDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                QuestDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2).build()
    }
}
