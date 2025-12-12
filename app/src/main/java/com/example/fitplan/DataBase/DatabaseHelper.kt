package com.example.fitplan.DataBase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "FitPlanDB.db", null, 1) {

    companion object {
        const val TABLE_USERS = "users"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_AGE = "age"
        const val COLUMN_HEIGHT = "height"
        const val COLUMN_CURRENT_WEIGHT = "current_weight"
        const val COLUMN_TARGET_WEIGHT = "target_weight"
        const val COLUMN_GENDER = "gender"
        const val COLUMN_ACTIVITY_LEVEL = "activity_level"
        const val COLUMN_GOAL = "goal"
        const val COLUMN_REGISTER_DATE = "register_date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_EMAIL TEXT,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_AGE INTEGER,
                $COLUMN_HEIGHT INTEGER,
                $COLUMN_CURRENT_WEIGHT REAL,
                $COLUMN_TARGET_WEIGHT REAL,
                $COLUMN_GENDER TEXT,
                $COLUMN_ACTIVITY_LEVEL TEXT,
                $COLUMN_GOAL TEXT,
                $COLUMN_REGISTER_DATE INTEGER
            )
        """
        db.execSQL(createUsersTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
}