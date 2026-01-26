package com.example.fitplan.ui

import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.User
import com.example.fitplan.R
import com.example.fitplan.ui.MainActivity3
import com.example.fitplan.ui.NutritionFragment

// Фрагмент для регистрации нового пользователя
class Reg : Fragment() {

    // Создание интерфейса фрагмента регистрации
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reg, container, false)

        // Инициализация элементов интерфейса для ввода данных
        val email = view.findViewById<EditText>(R.id.emailEditText)
        val password = view.findViewById<EditText>(R.id.passwordEditText)
        val password2 = view.findViewById<EditText>(R.id.confirmPasswordEditText)
        val name = view.findViewById<EditText>(R.id.nameEditText)
        val age = view.findViewById<EditText>(R.id.ageEditText)
        val height = view.findViewById<EditText>(R.id.heightEditText)
        val weight = view.findViewById<EditText>(R.id.currentWeightEditText)
        val targetWeight = view.findViewById<EditText>(R.id.targetWeightEditText)
        val activitySpinner = view.findViewById<Spinner>(R.id.activityLevelSpinner)
        val goalSpinner = view.findViewById<Spinner>(R.id.goalSpinner)
        val maleBtn = view.findViewById<RadioButton>(R.id.maleRadioButton)
        val registerBtn = view.findViewById<Button>(R.id.registerButton)
        val loginLink = view.findViewById<TextView>(R.id.loginTextView)

        // Кнопка для показа/скрытия пароля
        val eyeBtn = view.findViewById<TextView>(R.id.passwordToggle)
        eyeBtn.setOnClickListener {
            if (password.inputType == 129) {
                password.inputType = 1
                eyeBtn.text = "🙈"
            } else {
                password.inputType = 129
                eyeBtn.text = "👁️"
            }
        }

        setupSpinners(activitySpinner, goalSpinner)

        // Обработчик кнопки регистрации
        registerBtn.setOnClickListener {
            if (checkFields(email, password, password2, name)) {
                saveUser(
                    email.text.toString(),
                    password.text.toString(),
                    name.text.toString(),
                    age.text.toString(),
                    height.text.toString(),
                    weight.text.toString(),
                    targetWeight.text.toString(),
                    if (maleBtn.isChecked) "Мужской" else "Женский",
                    activitySpinner.selectedItem.toString(),
                    goalSpinner.selectedItem.toString()
                )
            }
        }

        // Обработчик ссылки для перехода к логину
        loginLink.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    // Настройка выпадающих списков (спиннеров)
    private fun setupSpinners(activitySpinner: Spinner, goalSpinner: Spinner) {
        activitySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf("Низкий", "Средний", "Высокий")
        )
        goalSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf("Похудеть", "Набрать массу", "Поддерживать вес")
        )
    }

    // Проверка заполненности обязательных полей
    private fun checkFields(email: EditText, password: EditText, password2: EditText, name: EditText): Boolean {
        when {
            email.text.isEmpty() -> { toast("Введите email"); return false }
            password.text.isEmpty() -> { toast("Введите пароль"); return false }
            password.text.toString() != password2.text.toString() -> { toast("Пароли не совпадают"); return false }
            name.text.isEmpty() -> { toast("Введите имя"); return false }
        }
        return true
    }

    // Сохранение данных пользователя в базу данных
    private fun saveUser(
        email: String,
        password: String,
        name: String,
        age: String,
        height: String,
        weight: String,
        targetWeight: String,
        gender: String,
        activity: String,
        goal: String
    ) {
        try {
            val user = User(
                id = 0,
                name = name,
                email = email,
                password = password,
                age = age.toIntOrNull(),
                height = height.toIntOrNull(),
                weight = weight.toIntOrNull(),
                targetWeight = targetWeight.toIntOrNull(),
                gender = gender,
                activity = activity,
                goal = goal,
                registerDate = System.currentTimeMillis(),
                profileImage = null
            )

            val dbHelper = DatabaseHelper(requireContext())
            val userId = dbHelper.addUser(user)

            if (userId != -1L) {
                // Сохранение ID пользователя в SharedPreferences для сессии
                val prefs = requireContext().getSharedPreferences("session", Context.MODE_PRIVATE)
                prefs.edit().putLong("user_id", userId).apply()
                toast("Регистрация успешна!")

                // Переход к фрагменту питания после успешной регистрации
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, NutritionFragment())
                    .commit()
            } else {
                toast("Ошибка регистрации")
            }

        } catch (e: Exception) {
            toast("Ошибка: ${e.message}")
        }
    }

    // Вспомогательная функция для показа всплывающих сообщений
    private fun toast(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}