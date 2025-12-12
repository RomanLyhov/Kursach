package com.example.fitplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.ui.WorkoutFragment

class Login : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val emailEditText = view.findViewById<EditText>(R.id.loginEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                checkUser(email, password)
            } else {
                Toast.makeText(requireContext(), "Введите данные", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            val regFragment = Reg()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, regFragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun checkUser(email: String, password: String) {
        try {
            val dbHelper = DatabaseHelper(requireContext())
            val db = dbHelper.readableDatabase

            val cursor = db.rawQuery(
                "SELECT * FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_EMAIL} = ? AND ${DatabaseHelper.COLUMN_PASSWORD} = ?",
                arrayOf(email, password)
            )

            if (cursor.count > 0) {
                Toast.makeText(requireContext(), "ВХОД УСПЕШЕН!", Toast.LENGTH_SHORT).show()
                goToWorkouts()
            } else {
                Toast.makeText(requireContext(), "Неправильные данные", Toast.LENGTH_SHORT).show()
            }

            cursor.close()
            db.close()
            dbHelper.close()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "ОШИБКА: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun goToWorkouts() {
        try {
            val workoutFragment = CreateWork()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, workoutFragment)
                .addToBackStack("workouts")
                .commit()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка перехода: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}