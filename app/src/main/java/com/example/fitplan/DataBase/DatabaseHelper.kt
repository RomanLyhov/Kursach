package com.example.fitplan.DataBase

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.fitplan.Models.*
import com.example.fitplan.Models.Api.ApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "FitPlanDB.db"
        private const val DATABASE_VERSION = 9

        const val TABLE_USERS = "users"
        const val TABLE_WORKOUTS = "workouts"
        const val TABLE_EXERCISES = "exercises"
        const val TABLE_WORKOUT_EXERCISES = "workout_exercises"
        const val TABLE_PRODUCTS = "products"
        const val TABLE_NUTRITION_LOG = "nutrition_log"
        const val TABLE_NUTRITION_ARCHIVE = "nutrition_archive"
        const val COL_ID = "_id"
        const val COL_USER_ID = "user_id"
        const val COL_PRODUCT_ID = "product_id"
        const val COL_NAME = "name"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DB", "Creating ALL tables from scratch...")
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        try {

            db.execSQL("""
                CREATE TABLE $TABLE_USERS (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_NAME TEXT,
                    $COL_EMAIL TEXT,
                    $COL_PASSWORD TEXT,
                    age INTEGER,
                    height INTEGER,
                    current_weight INTEGER,
                    target_weight INTEGER,
                    activity_level TEXT,
                    goal TEXT,
                    gender TEXT,
                    register_date INTEGER,
                    profile_image TEXT,
                    daily_calories_goal INTEGER,
                    daily_protein_goal INTEGER,
                    daily_fat_goal INTEGER,
                    daily_carbs_goal INTEGER
                )
            """)
            Log.d("DB", "✓ Table $TABLE_USERS created")

            db.execSQL("""
                CREATE TABLE $TABLE_WORKOUTS (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_USER_ID INTEGER,
                    $COL_NAME TEXT,
                    created_at INTEGER
                )
            """)
            Log.d("DB", "✓ Table $TABLE_WORKOUTS created")

            db.execSQL("""
                CREATE TABLE $TABLE_EXERCISES (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_NAME TEXT
                )
            """)
            Log.d("DB", "✓ Table $TABLE_EXERCISES created")

            db.execSQL("""
                CREATE TABLE $TABLE_WORKOUT_EXERCISES (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    workout_id INTEGER,
                    exercise_id INTEGER,
                    sets INTEGER,
                    reps INTEGER,
                    weight INTEGER,
                    rest INTEGER
                )
            """)
            Log.d("DB", "✓ Table $TABLE_WORKOUT_EXERCISES created")


            db.execSQL("""
                CREATE TABLE $TABLE_PRODUCTS (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_NAME TEXT UNIQUE,
                    calories REAL,
                    protein REAL,
                    fat REAL,
                    carbs REAL
                )
            """)
            Log.d("DB", "✓ Table $TABLE_PRODUCTS created")

            db.execSQL("""
                CREATE TABLE $TABLE_NUTRITION_LOG (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_USER_ID INTEGER,
                    $COL_PRODUCT_ID INTEGER,
                    meal_type TEXT,
                    quantity INTEGER,
                    calories REAL,
                    protein REAL,
                    fat REAL,
                    carbs REAL,
                    date INTEGER
                )
            """)
            Log.d("DB", "✓ Table $TABLE_NUTRITION_LOG created")

            db.execSQL("""
                CREATE TABLE $TABLE_NUTRITION_ARCHIVE (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_USER_ID INTEGER,
                    $COL_PRODUCT_ID INTEGER,
                    product_name TEXT,
                    meal_type TEXT,
                    quantity INTEGER,
                    calories REAL,
                    protein REAL,
                    fat REAL,
                    carbs REAL,
                    date INTEGER,
                    archive_date INTEGER
                )
            """)
            Log.d("DB", " Table $TABLE_NUTRITION_ARCHIVE created")

            db.execSQL("""
                CREATE INDEX idx_products_name ON $TABLE_PRODUCTS($COL_NAME COLLATE NOCASE)
            """)
            Log.d("DB", " Index idx_products_name created")

            checkAllTablesCreated(db)

        } catch (e: Exception) {
            Log.e("DB", " ERROR creating tables: ${e.message}")
            Log.e("DB", "Stack trace:", e)
            throw e
        }
    }

    private fun checkAllTablesCreated(db: SQLiteDatabase) {
        val expectedTables = listOf(
            TABLE_USERS, TABLE_WORKOUTS, TABLE_EXERCISES,
            TABLE_WORKOUT_EXERCISES, TABLE_PRODUCTS,
            TABLE_NUTRITION_LOG, TABLE_NUTRITION_ARCHIVE
        )

        Log.d("DB", "=== Проверка создания таблиц ===")
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'",
            null
        )

        val createdTables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val tableName = cursor.getString(0)
            createdTables.add(tableName)
            Log.d("DB", "Найдена таблица: $tableName")
        }
        cursor.close()

        expectedTables.forEach { table ->
            if (table in createdTables) {
                Log.d("DB", " Таблица '$table' создана")
            } else {
                Log.e("DB", " Таблица '$table' НЕ создана!")
            }
        }
        Log.d("DB", "=== Проверка завершена ===")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DB", "‼ FORCED UPGRADE: Dropping all tables and recreating...")
        dropAllTables(db)
        createTables(db)

        Log.d("DB", " Database upgrade completed successfully")
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        val tables = listOf(
            TABLE_USERS, TABLE_WORKOUTS, TABLE_EXERCISES,
            TABLE_WORKOUT_EXERCISES, TABLE_PRODUCTS,
            TABLE_NUTRITION_LOG, TABLE_NUTRITION_ARCHIVE
        )

        tables.forEach { table ->
            try {
                db.execSQL("DROP TABLE IF EXISTS $table")
                Log.d("DB", "Dropped table: $table")
            } catch (e: Exception) {
                Log.w("DB", "Could not drop table $table: ${e.message}")
            }
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        Log.d("DB", "Database opened, checking tables...")
        ensureAllTablesExist(db)
    }

    private fun ensureAllTablesExist(db: SQLiteDatabase) {
        val expectedTables = listOf(
            TABLE_USERS, TABLE_WORKOUTS, TABLE_EXERCISES,
            TABLE_WORKOUT_EXERCISES, TABLE_PRODUCTS,
            TABLE_NUTRITION_LOG, TABLE_NUTRITION_ARCHIVE
        )

        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table'",
            null
        )

        val existingTables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            existingTables.add(cursor.getString(0))
        }
        cursor.close()

        expectedTables.forEach { table ->
            if (table !in existingTables) {
                Log.e("DB", " Table $table is missing! Recreating...")
                createMissingTable(db, table)
            }
        }
    }

    private fun createMissingTable(db: SQLiteDatabase, tableName: String) {
        when (tableName) {
            TABLE_USERS -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_USERS (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT,
                        $COL_EMAIL TEXT,
                        $COL_PASSWORD TEXT,
                        age INTEGER,
                        height INTEGER,
                        current_weight INTEGER,
                        target_weight INTEGER,
                        activity_level TEXT,
                        goal TEXT,
                        gender TEXT,
                        register_date INTEGER,
                        profile_image TEXT,
                        daily_calories_goal INTEGER,
                        daily_protein_goal INTEGER,
                        daily_fat_goal INTEGER,
                        daily_carbs_goal INTEGER
                    )
                """)
            }
            TABLE_WORKOUTS -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_WORKOUTS (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_USER_ID INTEGER,
                        $COL_NAME TEXT,
                        created_at INTEGER
                    )
                """)
            }
            TABLE_EXERCISES -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_EXERCISES (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT
                    )
                """)
            }
            TABLE_WORKOUT_EXERCISES -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_WORKOUT_EXERCISES (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        workout_id INTEGER,
                        exercise_id INTEGER,
                        sets INTEGER,
                        reps INTEGER,
                        weight INTEGER,
                        rest INTEGER
                    )
                """)
            }
            TABLE_PRODUCTS -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_PRODUCTS (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT UNIQUE,
                        calories REAL,
                        protein REAL,
                        fat REAL,
                        carbs REAL
                    )
                """)
            }
            TABLE_NUTRITION_LOG -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_NUTRITION_LOG (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_USER_ID INTEGER,
                        $COL_PRODUCT_ID INTEGER,
                        meal_type TEXT,
                        quantity INTEGER,
                        calories REAL,
                        protein REAL,
                        fat REAL,
                        carbs REAL,
                        date INTEGER
                    )
                """)
            }
            TABLE_NUTRITION_ARCHIVE -> {
                db.execSQL("""
                    CREATE TABLE $TABLE_NUTRITION_ARCHIVE (
                        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_USER_ID INTEGER,
                        $COL_PRODUCT_ID INTEGER,
                        product_name TEXT,
                        meal_type TEXT,
                        quantity INTEGER,
                        calories REAL,
                        protein REAL,
                        fat REAL,
                        carbs REAL,
                        date INTEGER,
                        archive_date INTEGER
                    )
                """)
            }
        }
        Log.d("DB", " Created missing table: $tableName")
    }
    private fun getIntOrNull(cursor: android.database.Cursor, columnName: String): Int? {
        val index = cursor.getColumnIndex(columnName)
        return if (index != -1 && !cursor.isNull(index)) cursor.getInt(index) else null
    }

    private fun getLongOrNull(cursor: android.database.Cursor, columnName: String): Long? {
        val index = cursor.getColumnIndex(columnName)
        return if (index != -1 && !cursor.isNull(index)) cursor.getLong(index) else null
    }

    private fun getStringOrNull(cursor: android.database.Cursor, columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        return if (index != -1 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    private fun querySingle(
        table: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): ContentValues? {
        return readableDatabase.query(
            table, null, selection, selectionArgs, null, null, null, "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val contentValues = ContentValues()
                for (i in 0 until cursor.columnCount) {
                    when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> Unit
                        android.database.Cursor.FIELD_TYPE_INTEGER -> contentValues.put(cursor.getColumnName(i), cursor.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> contentValues.put(cursor.getColumnName(i), cursor.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> contentValues.put(cursor.getColumnName(i), cursor.getString(i))
                        android.database.Cursor.FIELD_TYPE_BLOB -> contentValues.put(cursor.getColumnName(i), cursor.getBlob(i))
                    }
                }
                contentValues
            } else {
                null
            }
        }
    }

    private fun queryMultiple(
        table: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
        orderBy: String? = null,
        limit: String? = null
    ): List<ContentValues> {
        return readableDatabase.query(
            table, null, selection, selectionArgs, null, null, orderBy, limit
        ).use { cursor ->
            val result = mutableListOf<ContentValues>()
            while (cursor.moveToNext()) {
                val contentValues = ContentValues()
                for (i in 0 until cursor.columnCount) {
                    when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> Unit
                        android.database.Cursor.FIELD_TYPE_INTEGER -> contentValues.put(cursor.getColumnName(i), cursor.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> contentValues.put(cursor.getColumnName(i), cursor.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> contentValues.put(cursor.getColumnName(i), cursor.getString(i))
                        android.database.Cursor.FIELD_TYPE_BLOB -> contentValues.put(cursor.getColumnName(i), cursor.getBlob(i))
                    }
                }
                result.add(contentValues)
            }
            result
        }
    }

    private fun queryCount(
        table: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): Int {
        return readableDatabase.query(
            table, arrayOf("COUNT(*)"), selection, selectionArgs, null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun updateTable(
        table: String,
        values: ContentValues,
        whereClause: String,
        whereArgs: Array<String>
    ): Int {
        return writableDatabase.update(table, values, whereClause, whereArgs)
    }

    private fun deleteRows(
        table: String,
        whereClause: String,
        whereArgs: Array<String>
    ): Int {
        return writableDatabase.delete(table, whereClause, whereArgs)
    }

    fun addUser(user: User): Long {
        return writableDatabase.insert(TABLE_USERS, null, user.toContentValues())
    }

    fun getUserById(userId: Long): User? {
        return querySingle(
            table = TABLE_USERS,
            selection = "$COL_ID = ?",
            selectionArgs = arrayOf(userId.toString())
        )?.toUser()
    }

    fun getUserByCredentials(email: String, password: String): User? {
        return querySingle(
            table = TABLE_USERS,
            selection = "$COL_EMAIL = ? AND $COL_PASSWORD = ?",
            selectionArgs = arrayOf(email, password)
        )?.toUser()
    }

    fun getUserByEmailAndPassword(email: String, password: String): User? {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COL_EMAIL = ? AND $COL_PASSWORD = ?",
            arrayOf(email, password)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                User(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)).orEmpty(),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)).orEmpty(),
                    password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)).orEmpty(),
                    age = getIntOrNull(cursor, "age"),
                    height = getIntOrNull(cursor, "height"),
                    weight = getIntOrNull(cursor, "current_weight"),
                    targetWeight = getIntOrNull(cursor, "target_weight"),
                    activity = cursor.getString(cursor.getColumnIndexOrThrow("activity_level")).orEmpty(),
                    goal = cursor.getString(cursor.getColumnIndexOrThrow("goal")).orEmpty(),
                    gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")).orEmpty(),
                    registerDate = getLongOrNull(cursor, "register_date"),
                    profileImage = getStringOrNull(cursor, "profile_image"),
                    dailyCaloriesGoal = getIntOrNull(cursor, "daily_calories_goal"),
                    dailyProteinGoal = getIntOrNull(cursor, "daily_protein_goal"),
                    dailyFatGoal = getIntOrNull(cursor, "daily_fat_goal"),
                    dailyCarbsGoal = getIntOrNull(cursor, "daily_carbs_goal")
                )
            } else {
                null
            }
        }
    }

    fun updateUser(user: User) {
        writableDatabase.update(
            TABLE_USERS,
            user.toContentValues(),
            "$COL_ID = ?",
            arrayOf(user.id.toString())
        )
    }

    fun addWorkout(userId: Long, name: String): Long {
        return writableDatabase.insert(TABLE_WORKOUTS, null, ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_NAME, name)
            put("created_at", System.currentTimeMillis())
        })
    }

    fun getWorkoutById(workoutId: Long): Workout? {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_WORKOUTS WHERE $COL_ID = ?",
            arrayOf(workoutId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                Workout(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME))
                )
            } else {
                null
            }
        }
    }

    fun updateWorkout(workoutId: Long, name: String) {
        updateTable(
            table = TABLE_WORKOUTS,
            values = ContentValues().apply { put(COL_NAME, name) },
            whereClause = "$COL_ID = ?",
            whereArgs = arrayOf(workoutId.toString())
        )
    }

    fun getWorkoutsByUser(userId: Long): List<Workout> {
        Log.d("DatabaseHelper", "Getting workouts for userId: $userId")
        val workouts = queryMultiple(
            table = TABLE_WORKOUTS,
            selection = "$COL_USER_ID = ?",
            selectionArgs = arrayOf(userId.toString())
        ).map { it.toWorkout() }
        Log.d("DatabaseHelper", "Found ${workouts.size} workouts")
        return workouts
    }

    fun addExerciseToWorkout(workoutId: Long, exercise: Exercise): Long {
        var exerciseId = -1L
        writableDatabase.use { db ->
            exerciseId = db.insert(TABLE_EXERCISES, null, ContentValues().apply {
                put(COL_NAME, exercise.name)
            })

            db.insert(TABLE_WORKOUT_EXERCISES, null, ContentValues().apply {
                put("workout_id", workoutId)
                put("exercise_id", exerciseId)
                put("sets", exercise.sets)
                put("reps", exercise.reps)
                put("weight", exercise.weight)
                put("rest", exercise.rest)
            })
        }
        return exerciseId
    }

    fun getExercises(workoutId: Long): List<Exercise> {
        return readableDatabase.rawQuery("""
            SELECT we.$COL_ID, e.$COL_NAME, we.sets, we.reps, we.weight, we.rest
            FROM $TABLE_WORKOUT_EXERCISES we
            JOIN $TABLE_EXERCISES e ON e.$COL_ID = we.exercise_id
            WHERE we.workout_id = ?
        """, arrayOf(workoutId.toString())).use { cursor ->
            val exercises = mutableListOf<Exercise>()
            while (cursor.moveToNext()) {
                exercises.add(Exercise(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    workoutId = workoutId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    sets = cursor.getInt(cursor.getColumnIndexOrThrow("sets")),
                    reps = cursor.getInt(cursor.getColumnIndexOrThrow("reps")),
                    weight = cursor.getInt(cursor.getColumnIndexOrThrow("weight")),
                    rest = cursor.getInt(cursor.getColumnIndexOrThrow("rest"))
                ))
            }
            exercises
        }
    }

    fun deleteExercisesByWorkout(workoutId: Long) {
        deleteRows(
            table = TABLE_WORKOUT_EXERCISES,
            whereClause = "workout_id = ?",
            whereArgs = arrayOf(workoutId.toString())
        )
    }

    suspend fun insertOrGetProduct(product: Product): Long = withContext(Dispatchers.IO) {
        getProductByName(product.name)?.id ?: run {
            writableDatabase.insert(TABLE_PRODUCTS, null, product.toContentValues())
        }
    }

    suspend fun getProductByName(name: String): Product? = withContext(Dispatchers.IO) {
        querySingle(
            table = TABLE_PRODUCTS,
            selection = "$COL_NAME = ?",
            selectionArgs = arrayOf(name)
        )?.toProduct()
    }

    suspend fun getProductById(productId: Long): Product? = withContext(Dispatchers.IO) {
        querySingle(
            table = TABLE_PRODUCTS,
            selection = "$COL_ID = ?",
            selectionArgs = arrayOf(productId.toString())
        )?.toProduct()
    }

    suspend fun getAllProductsMatching(query: String): List<Product> = withContext(Dispatchers.IO) {
        val localResults = searchInLocalDatabaseFast(query)
        if (localResults.size >= 5) {
            return@withContext localResults.take(20)
        }
        if (query.length >= 2) {
            try {
                val apiProducts = ApiManager.searchProducts(query)
                val newApiProducts = apiProducts.filter { apiProduct ->
                    localResults.none { it.name.equals(apiProduct.name, ignoreCase = true) }
                }

                saveProductsToDatabase(newApiProducts)
            } catch (e: Exception) {
                Log.e("DB", "API search error: ${e.message}")
            }
        }

        localResults.take(20)
    }

    private fun searchInLocalDatabaseFast(query: String): List<Product> {
        return queryMultiple(
            table = TABLE_PRODUCTS,
            selection = "$COL_NAME LIKE ? || '%' COLLATE NOCASE",
            selectionArgs = arrayOf(query),
            orderBy = "$COL_NAME ASC",
            limit = "15"
        ).map { it.toProduct() }
    }

    private fun saveProductsToDatabase(products: List<Product>) {
        products.forEach { product ->
            try {
                val exists = queryCount(
                    table = TABLE_PRODUCTS,
                    selection = "$COL_NAME = ? COLLATE NOCASE",
                    selectionArgs = arrayOf(product.name)
                ) > 0

                if (!exists) {
                    writableDatabase.insert(TABLE_PRODUCTS, null, product.toContentValues())
                }
            } catch (e: Exception) {
                Log.e("DB", "Error saving product: ${e.message}")
            }
        }
    }
    suspend fun addMeal(
        userId: Long,
        productId: Long,
        mealType: String,
        quantity: Int,
        calories: Float,
        protein: Float,
        fat: Float,
        carbs: Float
    ): Long = withContext(Dispatchers.IO) {
        writableDatabase.insert(TABLE_NUTRITION_LOG, null, ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_PRODUCT_ID, productId)
            put("meal_type", mealType)
            put("quantity", quantity)
            put("calories", calories)
            put("protein", protein)
            put("fat", fat)
            put("carbs", carbs)
            put("date", System.currentTimeMillis())
        })
    }

    suspend fun getMealsByUserAndType(userId: Long, mealType: String): List<Meal> =
        withContext(Dispatchers.IO) {
            readableDatabase.rawQuery("""
                SELECT nl.$COL_ID, p.$COL_NAME, nl.quantity, nl.calories, nl.protein, 
                       nl.fat, nl.carbs, nl.date, nl.meal_type
                FROM $TABLE_NUTRITION_LOG nl
                JOIN $TABLE_PRODUCTS p ON p.$COL_ID = nl.$COL_PRODUCT_ID
                WHERE nl.$COL_USER_ID = ? AND nl.meal_type = ? COLLATE NOCASE
                ORDER BY nl.date ASC
            """, arrayOf(userId.toString(), mealType)).use { cursor ->
                val meals = mutableListOf<Meal>()
                while (cursor.moveToNext()) {
                    meals.add(Meal(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        productName = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                        calories = cursor.getDouble(cursor.getColumnIndexOrThrow("calories")).toInt(),
                        protein = cursor.getDouble(cursor.getColumnIndexOrThrow("protein")).toInt(),
                        fat = cursor.getDouble(cursor.getColumnIndexOrThrow("fat")).toInt(),
                        carbs = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs")).toInt(),
                        date = cursor.getLong(cursor.getColumnIndexOrThrow("date")),
                        mealType = cursor.getString(cursor.getColumnIndexOrThrow("meal_type"))
                    ))
                }
                meals
            }
        }

    suspend fun getMealProductsByType(userId: Long, mealType: String): List<MealProductDisplay> =
        withContext(Dispatchers.IO) {
            readableDatabase.rawQuery("""
                SELECT nl.$COL_ID, nl.$COL_PRODUCT_ID, p.$COL_NAME, nl.quantity, 
                       nl.calories, nl.protein, nl.fat, nl.carbs
                FROM $TABLE_NUTRITION_LOG nl
                JOIN $TABLE_PRODUCTS p ON p.$COL_ID = nl.$COL_PRODUCT_ID
                WHERE nl.$COL_USER_ID = ? AND nl.meal_type = ? COLLATE NOCASE
                ORDER BY nl.date ASC
            """, arrayOf(userId.toString(), mealType)).use { cursor ->
                val mealProducts = mutableListOf<MealProductDisplay>()
                while (cursor.moveToNext()) {
                    mealProducts.add(MealProductDisplay(
                        mealItemId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        productId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID)),
                        productName = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                        calories = cursor.getDouble(cursor.getColumnIndexOrThrow("calories")).toFloat(),
                        protein = cursor.getDouble(cursor.getColumnIndexOrThrow("protein")).toFloat(),
                        fat = cursor.getDouble(cursor.getColumnIndexOrThrow("fat")).toFloat(),
                        carbs = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs")).toFloat()
                    ))
                }
                mealProducts
            }
        }

    suspend fun deleteMealItem(mealItemId: Long): Boolean = withContext(Dispatchers.IO) {
        deleteRows(
            table = TABLE_NUTRITION_LOG,
            whereClause = "$COL_ID = ?",
            whereArgs = arrayOf(mealItemId.toString())
        ) > 0
    }

    suspend fun updateMealItem(
        mealItemId: Long,
        quantity: Int,
        calories: Float,
        protein: Float,
        fat: Float,
        carbs: Float
    ): Boolean = withContext(Dispatchers.IO) {
        updateTable(
            table = TABLE_NUTRITION_LOG,
            values = ContentValues().apply {
                put("quantity", quantity)
                put("calories", calories)
                put("protein", protein)
                put("fat", fat)
                put("carbs", carbs)
            },
            whereClause = "$COL_ID = ?",
            whereArgs = arrayOf(mealItemId.toString())
        ) > 0
    }
    suspend fun getDailySummary(userId: Long): DailySummary? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("""
            SELECT DATE(datetime(date/1000, 'unixepoch', 'localtime')) as log_date,
                   SUM(calories) as total_calories,
                   SUM(protein) as total_protein,
                   SUM(fat) as total_fat,
                   SUM(carbs) as total_carbs
            FROM $TABLE_NUTRITION_LOG
            WHERE $COL_USER_ID = ? 
            AND DATE(datetime(date/1000, 'unixepoch', 'localtime')) = DATE('now', 'localtime')
            GROUP BY log_date
        """, arrayOf(userId.toString())).use { cursor ->
            if (cursor.moveToFirst()) DailySummary(
                date = cursor.getString(cursor.getColumnIndexOrThrow("log_date")),
                totalCalories = cursor.getDouble(cursor.getColumnIndexOrThrow("total_calories")).toInt(),
                totalProtein = cursor.getDouble(cursor.getColumnIndexOrThrow("total_protein")).toInt(),
                totalFat = cursor.getDouble(cursor.getColumnIndexOrThrow("total_fat")).toInt(),
                totalCarbs = cursor.getDouble(cursor.getColumnIndexOrThrow("total_carbs")).toInt()
            ) else null
        }
    }

    suspend fun getMealSummaryByType(userId: Long, mealType: String): MealSummary? =
        withContext(Dispatchers.IO) {
            readableDatabase.rawQuery("""
                SELECT meal_type,
                       COUNT($COL_ID) as product_count,
                       SUM(quantity) as total_quantity,
                       SUM(calories) as total_calories,
                       SUM(protein) as total_protein,
                       SUM(fat) as total_fat,
                       SUM(carbs) as total_carbs
                FROM $TABLE_NUTRITION_LOG
                WHERE $COL_USER_ID = ? AND meal_type = ? COLLATE NOCASE
                GROUP BY meal_type
            """, arrayOf(userId.toString(), mealType)).use { cursor ->
                if (cursor.moveToFirst()) MealSummary(
                    mealType = cursor.getString(cursor.getColumnIndexOrThrow("meal_type")),
                    productCount = cursor.getInt(cursor.getColumnIndexOrThrow("product_count")),
                    totalQuantity = cursor.getInt(cursor.getColumnIndexOrThrow("total_quantity")),
                    totalCalories = cursor.getDouble(cursor.getColumnIndexOrThrow("total_calories")).toInt(),
                    totalProtein = cursor.getDouble(cursor.getColumnIndexOrThrow("total_protein")).toInt(),
                    totalFat = cursor.getDouble(cursor.getColumnIndexOrThrow("total_fat")).toInt(),
                    totalCarbs = cursor.getDouble(cursor.getColumnIndexOrThrow("total_carbs")).toInt()
                ) else null
            }
        }

    suspend fun archiveOldMeals(userId: Long): Boolean = withContext(Dispatchers.IO) {
        val todayStart = getStartOfDay()

        val oldMeals = readableDatabase.rawQuery("""
            SELECT nl.*, p.$COL_NAME as product_name
            FROM $TABLE_NUTRITION_LOG nl
            JOIN $TABLE_PRODUCTS p ON p.$COL_ID = nl.$COL_PRODUCT_ID
            WHERE nl.$COL_USER_ID = ? AND nl.date < ?
        """, arrayOf(userId.toString(), todayStart.toString())).use { cursor ->
            val meals = mutableListOf<Pair<MealProductDisplay, Long>>()
            while (cursor.moveToNext()) {
                val meal = MealProductDisplay(
                    mealItemId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                    productId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID)),
                    productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                    quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
                    calories = cursor.getDouble(cursor.getColumnIndexOrThrow("calories")).toFloat(),
                    protein = cursor.getDouble(cursor.getColumnIndexOrThrow("protein")).toFloat(),
                    fat = cursor.getDouble(cursor.getColumnIndexOrThrow("fat")).toFloat(),
                    carbs = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs")).toFloat()
                )
                val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                meals.add(Pair(meal, date))
            }
            meals
        }

        if (oldMeals.isEmpty()) return@withContext true

        writableDatabase.use { db ->
            db.beginTransaction()
            try {
                oldMeals.forEach { (mealData, originalDate) ->
                    val values = ContentValues().apply {
                        put(COL_USER_ID, userId)
                        put(COL_PRODUCT_ID, mealData.productId)
                        put("product_name", mealData.productName)
                        put("meal_type", "Завтрак")
                        put("quantity", mealData.quantity)
                        put("calories", mealData.calories)
                        put("protein", mealData.protein)
                        put("fat", mealData.fat)
                        put("carbs", mealData.carbs)
                        put("date", originalDate)
                        put("archive_date", System.currentTimeMillis())
                    }
                    db.insert(TABLE_NUTRITION_ARCHIVE, null, values)
                }

                val rowsDeleted = db.delete(
                    TABLE_NUTRITION_LOG,
                    "$COL_USER_ID = ? AND date < ?",
                    arrayOf(userId.toString(), todayStart.toString())
                )

                db.setTransactionSuccessful()
                rowsDeleted > 0
            } catch (e: Exception) {
                Log.e("DB", "Archive error", e)
                false
            } finally {
                db.endTransaction()
            }
        }
    }

    suspend fun clearTodayMeals(userId: Long): Boolean = withContext(Dispatchers.IO) {
        val todayStart = getStartOfDay()
        deleteRows(
            table = TABLE_NUTRITION_LOG,
            whereClause = "$COL_USER_ID = ? AND date >= ?",
            whereArgs = arrayOf(userId.toString(), todayStart.toString())
        ) > 0
    }

    private fun getStartOfDay(): Long {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

private fun android.database.Cursor.getIntOrNull(columnName: String): Int? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getInt(index) else null
}

private fun android.database.Cursor.getLongOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getLong(index) else null
}

private fun android.database.Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getString(index) else null
}
private fun ContentValues.toUser(): User {
    return User(
        id = getAsLong("_id") ?: 0L,
        name = getAsString("name") ?: "",
        email = getAsString("email") ?: "",
        password = getAsString("password") ?: "",
        age = getAsInteger("age"),
        height = getAsInteger("height"),
        weight = getAsInteger("current_weight"),
        targetWeight = getAsInteger("target_weight"),
        activity = getAsString("activity_level") ?: "",
        goal = getAsString("goal") ?: "",
        gender = getAsString("gender") ?: "",
        registerDate = getAsLong("register_date"),
        profileImage = getAsString("profile_image"),
        dailyCaloriesGoal = getAsInteger("daily_calories_goal"),
        dailyProteinGoal = getAsInteger("daily_protein_goal"),
        dailyFatGoal = getAsInteger("daily_fat_goal"),
        dailyCarbsGoal = getAsInteger("daily_carbs_goal")
    )
}

private fun User.toContentValues(): ContentValues {
    return ContentValues().apply {
        put("name", name)
        put("email", email)
        put("password", password)
        put("age", age)
        put("height", height)
        put("current_weight", weight)
        put("target_weight", targetWeight)
        put("activity_level", activity)
        put("goal", goal)
        put("gender", gender)
        put("register_date", registerDate ?: System.currentTimeMillis())
        put("profile_image", profileImage)
        put("daily_calories_goal", dailyCaloriesGoal)
        put("daily_protein_goal", dailyProteinGoal)
        put("daily_fat_goal", dailyFatGoal)
        put("daily_carbs_goal", dailyCarbsGoal)
    }
}
private fun ContentValues.toWorkout(): Workout {
    return Workout(
        id = getAsLong("_id") ?: 0L,
        userId = getAsLong("user_id") ?: 0L,
        name = getAsString("name") ?: ""
    )
}
private fun ContentValues.toProduct(): Product {
    return Product(
        id = getAsLong("_id") ?: 0L,
        name = getAsString("name") ?: "",
        calories = (getAsDouble("calories") ?: 0.0).toFloat(),
        protein = (getAsDouble("protein") ?: 0.0).toFloat(),
        fat = (getAsDouble("fat") ?: 0.0).toFloat(),
        carbs = (getAsDouble("carbs") ?: 0.0).toFloat()
    )
}

private fun Product.toContentValues(): ContentValues {
    return ContentValues().apply {
        put("name", name)
        put("calories", calories)
        put("protein", protein)
        put("fat", fat)
        put("carbs", carbs)
    }
}