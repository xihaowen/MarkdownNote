package com.mdnote.markdownnote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库类
 */
@Database(
    entities = [NoteEntity::class, TodoEntity::class, MemoryEntity::class, MemoryFts::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        /**
         * 版本 1 -> 2: 添加 isPinned 字段
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * 版本 2 -> 3: 添加回收站字段
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
            }
        }

        /**
         * 版本 3 -> 4: 添加待办事项表
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        content TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        dueDate INTEGER NOT NULL,
                        noteId INTEGER
                    )
                """)
            }
        }

        /**
         * 版本 4 -> 5: 添加外部文件夹 URI 字段（用于解析相对图片路径）
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN externalFolderUri TEXT")
            }
        }

        /**
         * 版本 5 -> 6: 添加 AI 记忆表及全文搜索虚拟表
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_memory` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `importance` REAL NOT NULL,
                        `sourceNoteId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `lastAccessedAt` INTEGER NOT NULL,
                        `accessCount` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_type` ON `ai_memory` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_createdAt` ON `ai_memory` (`createdAt`)")
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `ai_memory_fts` USING FTS4(
                        `content` TEXT,
                        content=`ai_memory`
                    )
                """)
                // Room 对 @Fts4 外部内容表会自动生成 room_fts_content_sync_* 触发器维护
                // 索引同步；这里手写的 ai_memory_fts_ai/ad/au 与之重复，会导致 UPDATE/DELETE
                // ai_memory 时 FTS 'delete' 命令找不到已被 Room BEFORE 触发器删除的 rowid，
                // 抛 SQL logic error。同步触发器交由 Room 自动管理即可（见 MIGRATION_6_7）
            }
        }

        /**
         * 版本 6 -> 7: 删除早期 5->6 误建的手写 FTS 同步触发器，修复 AI 功能闪退
         *
         * 线上已升级到 v6 的数据库同时存在手写触发器与 Room 自动触发器，两者冲突使
         * ai_memory 的 UPDATE/DELETE 必崩（AI 搜索记忆→更新访问记录会触发）。仅保留
         * Room 自动生成的那套触发器即可正常工作。
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TRIGGER IF EXISTS ai_memory_fts_au")
                db.execSQL("DROP TRIGGER IF EXISTS ai_memory_fts_ad")
                db.execSQL("DROP TRIGGER IF EXISTS ai_memory_fts_ai")
            }
        }
    }
}
