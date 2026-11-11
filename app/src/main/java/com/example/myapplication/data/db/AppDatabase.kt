package com.example.myapplication.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        SubscriptionEntity::class,
        BudgetEntity::class,
        DebtEntity::class,
        SavingsGoalEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN account TEXT NOT NULL DEFAULT 'CARD'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscriptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `displayPrice` TEXT NOT NULL,
                        `billingCycle` TEXT NOT NULL,
                        `billingDay` INTEGER NOT NULL,
                        `account` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `lastPaidEpochMonth` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category` TEXT NOT NULL,
                        `limitAmount` INTEGER NOT NULL,
                        `periodMonth` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        `dueDateEpochMillis` INTEGER,
                        `note` TEXT NOT NULL,
                        `isSettled` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` INTEGER NOT NULL,
                        `currentAmount` INTEGER NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `targetDateEpochMillis` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "finance.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
