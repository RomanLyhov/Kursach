package com.example.fitplan.ui.login

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.User
import com.example.fitplan.R
import com.example.fitplan.ui.MainActivity3
import com.example.fitplan.ui.ProfileFragment
import com.example.fitplan.ui.Reg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val emailInput = view.findViewById<EditText>(R.id.loginEditText)
        val passwordInput = view.findViewById<EditText>(R.id.passwordEditText)
        val loginBtn = view.findViewById<Button>(R.id.loginButton)
        val registerBtn = view.findViewById<Button>(R.id.registerButton)
        val eyeBtn = view.findViewById<TextView>(R.id.passwordToggle)

        // переключение видимости пароля
        eyeBtn.setOnClickListener {
            if (passwordInput.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT
                eyeBtn.text = "🙈"
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                eyeBtn.text = "👁️"
            }
            passwordInput.setSelection(passwordInput.text.length)
        }

        // кнопка входа
        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните поля", Toast.LENGTH_SHORT).show()
            } else {
                checkLogin(email, password)
            }
        }

        // кнопка регистрации
        registerBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Reg())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun checkLogin(email: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DatabaseHelper(requireContext())
                val user = db.getUserByEmailAndPassword(email, password)

                withContext(Dispatchers.Main) {
                    if (user != null) {
                        // сохраняем user_id
                        val prefs = requireContext().getSharedPreferences("session", Context.MODE_PRIVATE)
                        prefs.edit().putLong("user_id", user.id).apply()

                        // обновляем currentUser
                        (activity as? MainActivity3)?.currentUser = user

                        Toast.makeText(requireContext(), "Добро пожаловать, ${user.name}!", Toast.LENGTH_SHORT).show()

                        // показываем панель и открываем профиль
                        (activity as? MainActivity3)?.onLoginSuccess(user.id)

                    } else {
                        Toast.makeText(requireContext(), "Неверный email или пароль", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
