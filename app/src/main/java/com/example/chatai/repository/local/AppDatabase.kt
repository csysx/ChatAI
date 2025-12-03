package com.example.chatai.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chatai.model.data.ChatMessage
import com.example.chatai.model.data.Session

/**
 * 应用数据库（单例模式）：管理所有本地存储表
 */


@Database(entities = [ChatMessage::class,Session::class],//  指定数据库包含 ChatMessage 这张表
    version = 2,          //数据库版本号，后续升级数据库时需要递增
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // 提供 DAO 实例（用于操作消息表）
    abstract fun chatDao(): ChatDao
    abstract fun sessionDao(): SessionDao // 新增会话 DAO

    companion object {
        // 单例模式：避免重复创建数据库实例
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // 使用应用上下文，避免内存泄漏
                    AppDatabase::class.java,
                    "chat_database" // 数据库文件的名称
                )
                    .addMigrations(MIGRATION_1_2)
                    // .allowMainThreadQueries() // 不推荐在主线程执行数据库操作，所以注释掉
                    .build()
                INSTANCE = instance
                instance
            }
        }
        // 数据库迁移（从版本1到版本2，新增sessions表）
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE sessions (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        createTime INTEGER NOT NULL,
                        lastMessage TEXT NOT NULL,
                        lastMessageTime INTEGER NOT NULL,
                        userId TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}