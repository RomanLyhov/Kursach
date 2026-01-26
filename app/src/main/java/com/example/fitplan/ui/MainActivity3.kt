package com.example.fitplan.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.User
import com.example.fitplan.R
import com.example.fitplan.ui.login.Login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Главная активность приложения с нижней навигацией
class MainActivity3 : AppCompatActivity() {

    // Переменные для элементов интерфейса
    private lateinit var bottomPanel: LinearLayout
    private var currentTab: String = ""
    var currentUser: User? = null
    private var isUserLoaded = false
    private var isInitialLaunch = true

    // Исполнители для работы с потоками
    private val handler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor()

    // SharedPreferences для хранения сессии пользователя
    private val sharedPref by lazy {
        getSharedPreferences("session", MODE_PRIVATE)
    }

    // Инициализация активности при создании
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        Log.d("MainActivity3", "onCreate вызван, isInitialLaunch: $isInitialLaunch")

        bottomPanel = findViewById(R.id.bottom_navigation)
        setupPanelClicks()

        bottomPanel.visibility = View.VISIBLE

        if (savedInstanceState == null) {
            isInitialLaunch = true
            // Используем задержку для уменьшения нагрузки при старте
            handler.postDelayed({
                checkAuthAndOpenFragment()
            }, 100)
        } else {
            savedInstanceState.getString("currentTab")?.let {
                currentTab = it
                setActive(currentTab)
            }
            isUserLoaded = savedInstanceState.getBoolean("isUserLoaded", false)
            isInitialLaunch = savedInstanceState.getBoolean("isInitialLaunch", false)
        }
    }

    // Очистка ресурсов при уничтожении активности
    override fun onDestroy() {
        super.onDestroy()
        // Очищаем handler для предотвращения утечек памяти
        handler.removeCallbacksAndMessages(null)
        ioExecutor.shutdown()
    }

    // Сохранение состояния при повороте экрана
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentTab", currentTab)
        outState.putBoolean("isUserLoaded", isUserLoaded)
        outState.putBoolean("isInitialLaunch", isInitialLaunch)
    }

    // Проверка авторизации и открытие соответствующего фрагмента
    private fun checkAuthAndOpenFragment() {
        val userId = sharedPref.getLong("user_id", -1L)
        Log.d("MainActivity3", "checkAuthAndOpenFragment, userId: $userId, isUserLoaded: $isUserLoaded")

        if (userId == -1L) {
            Log.d("MainActivity3", "Пользователь не авторизован, открываем логин")
            openLogin()
        } else {
            if (isUserLoaded && currentUser != null) {
                Log.d("MainActivity3", "Пользователь уже загружен, открываем вкладку питания")
                openTab("food", addToBackStack = false)
            } else {
                loadCurrentUser(userId) { user ->
                    if (user != null) {
                        currentUser = user
                        isUserLoaded = true
                        Log.d("MainActivity3", "Пользователь успешно загружен, isInitialLaunch: $isInitialLaunch")

                        if (isInitialLaunch) {
                            Log.d("MainActivity3", "Первый запуск, открываем вкладку питания")
                            openTab("food", addToBackStack = false)
                            isInitialLaunch = false
                        }
                    } else {
                        Log.d("MainActivity3", "Пользователь не найден, открываем логин")
                        openLogin()
                    }
                }
            }
        }
    }

    // Загрузка данных текущего пользователя из базы данных
    private fun loadCurrentUser(userId: Long, onLoaded: (User?) -> Unit) {
        // Используем отдельный executor для работы с БД
        ioExecutor.execute {
            try {
                val db = DatabaseHelper(this@MainActivity3)
                val user = db.getUserById(userId)
                db.close() // Закрываем соединение после использования

                handler.post {
                    onLoaded(user)
                }
            } catch (e: Exception) {
                Log.e("MainActivity3", "Ошибка загрузки пользователя", e)
                handler.post {
                    onLoaded(null)
                }
            }
        }
    }

    // Обработчик успешного входа в систему
    fun onLoginSuccess(userId: Long) {
        Log.d("MainActivity3", "onLoginSuccess вызван, userId: $userId")
        sharedPref.edit().putLong("user_id", userId).apply()

        loadCurrentUser(userId) { user ->
            if (user != null) {
                currentUser = user
                isUserLoaded = true
                bottomPanel.visibility = View.VISIBLE

                Log.d("MainActivity3", "Вход успешен, открываем вкладку питания")
                openTab("food", addToBackStack = false)
            }
        }
    }

    // Открытие экрана входа в систему
    private fun openLogin() {
        isUserLoaded = false
        currentUser = null

        handler.post {
            bottomPanel.visibility = View.GONE

            try {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, Login())
                    setReorderingAllowed(true)
                }
            } catch (e: Exception) {
                Log.e("MainActivity3", "Ошибка открытия фрагмента логина", e)
            }
        }
    }

    // Настройка обработчиков кликов по панели навигации
    private fun setupPanelClicks() {
        // Добавляем debounce для предотвращения множественных быстрых нажатий
        setupDebouncedClick(R.id.nav_food) {
            Log.d("MainActivity3", "Нажата вкладка питания, isUserLoaded: $isUserLoaded")
            openTab("food", addToBackStack = false)
        }

        setupDebouncedClick(R.id.nav_workout) {
            Log.d("MainActivity3", "Нажата вкладка тренировок, isUserLoaded: $isUserLoaded")
            openTab("workout", addToBackStack = false)
        }

        setupDebouncedClick(R.id.nav_profile) {
            Log.d("MainActivity3", "Нажата вкладка профиля, isUserLoaded: $isUserLoaded")
            openTab("profile", addToBackStack = false)
        }
    }

    // Настройка обработчика кликов с задержкой (debounce)
    private fun setupDebouncedClick(viewId: Int, onClick: () -> Unit) {
        var lastClickTime = 0L
        val debounceTime = 500L // 500ms задержка

        findViewById<LinearLayout>(viewId).setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceTime) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }

    // Открытие указанной вкладки приложения
    private fun openTab(tab: String, addToBackStack: Boolean = false) {
        Log.d("MainActivity3", "openTab вызван: $tab, currentTab: $currentTab")

        // Проверяем, не пытаемся ли открыть тот же таб
        if (currentTab == tab) {
            Log.d("MainActivity3", "Уже на вкладке $tab, пропускаем")
            return
        }

        // Для всех табов проверяем авторизацию
        val userId = sharedPref.getLong("user_id", -1L)
        if (userId == -1L) {
            Log.d("MainActivity3", "ID пользователя не найден, открываем логин")
            openLogin()
            return
        }

        // Загружаем пользователя если нужно
        if (!isUserLoaded || currentUser == null) {
            loadCurrentUser(userId) { user ->
                if (user != null) {
                    currentUser = user
                    isUserLoaded = true
                    Log.d("MainActivity3", "Пользователь загружен, переходим к открытию $tab")
                    performTabOpen(tab, addToBackStack)
                } else {
                    Log.d("MainActivity3", "Пользователь не найден, открываем логин")
                    openLogin()
                }
            }
        } else {
            performTabOpen(tab, addToBackStack)
        }
    }

    // Выполнение открытия вкладки с фрагментом
    private fun performTabOpen(tab: String, addToBackStack: Boolean) {
        currentTab = tab
        Log.d("MainActivity3", "Устанавливаем currentTab в: $tab")

        val fragment: Fragment = when (tab) {
            "food" -> {
                Log.d("MainActivity3", "Создаем NutritionFragment")
                NutritionFragment()
            }
            "workout" -> {
                Log.d("MainActivity3", "Создаем WorkoutFragment")
                WorkoutFragment()
            }
            "profile" -> {
                Log.d("MainActivity3", "Создаем ProfileFragment")
                ProfileFragment()
            }
            else -> {
                Log.e("MainActivity3", "Неизвестная вкладка: $tab")
                return
            }
        }

        // Используем post для безопасного обновления UI
        handler.post {
            try {
                replaceFragment(fragment, addToBackStack)
                setActive(tab)
            } catch (e: Exception) {
                Log.e("MainActivity3", "Ошибка открытия вкладки $tab", e)
            }
        }
    }

    // Замена текущего фрагмента на новый
    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        Log.d("MainActivity3", "Заменяем фрагмент: ${fragment.javaClass.simpleName}")

        try {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, fragment)
                if (addToBackStack) {
                    addToBackStack(null)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity3", "Ошибка замены фрагмента", e)
            // Показываем сообщение об ошибке пользователю
            showError("Не удалось загрузить содержимое")
        }
    }

    // Установка активной вкладки на панели навигации
    private fun setActive(item: String) {
        Log.d("MainActivity3", "Устанавливаем активную вкладку: $item")

        try {
            setColor(R.id.nav_food, R.color.bottom_nav_inactive)
            setColor(R.id.nav_workout, R.color.bottom_nav_inactive)
            setColor(R.id.nav_profile, R.color.bottom_nav_inactive)

            when (item) {
                "food" -> setColor(R.id.nav_food, R.color.bottom_nav_active)
                "workout" -> setColor(R.id.nav_workout, R.color.bottom_nav_active)
                "profile" -> setColor(R.id.nav_profile, R.color.bottom_nav_active)
            }
        } catch (e: Exception) {
            Log.e("MainActivity3", "Ошибка установки активной вкладки", e)
        }
    }

    // Установка цвета для элемента навигации
    private fun setColor(buttonId: Int, colorId: Int) {
        try {
            val button = findViewById<LinearLayout>(buttonId)
            val icon = button.getChildAt(0) as? ImageView
            val text = button.getChildAt(1) as? TextView

            if (icon != null && text != null) {
                val color = ContextCompat.getColor(this, colorId)
                icon.setColorFilter(color)
                text.setTextColor(color)
            }
        } catch (e: Exception) {
            Log.e("MainActivity3", "Ошибка установки цвета для кнопки $buttonId", e)
        }
    }

    // Показать сообщение об ошибке
    private fun showError(message: String) {
        // Можно добавить Toast или Snackbar для показа ошибок
        Log.e("MainActivity3", "UI Ошибка: $message")
    }
}