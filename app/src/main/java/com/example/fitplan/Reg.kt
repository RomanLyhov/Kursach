package com.example.fitplan

import android.content.ContentValues
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper

class Reg : Fragment() {

    private lateinit var activityLevelSpinner: Spinner
    private lateinit var goalSpinner: Spinner
    private lateinit var genderRadioGroup: RadioGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reg, container, false)

        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = view.findViewById<EditText>(R.id.confirmPasswordEditText)
        val nameEditText = view.findViewById<EditText>(R.id.nameEditText)
        val ageEditText = view.findViewById<EditText>(R.id.ageEditText)
        val heightEditText = view.findViewById<EditText>(R.id.heightEditText)
        val currentWeightEditText = view.findViewById<EditText>(R.id.currentWeightEditText)
        val targetWeightEditText = view.findViewById<EditText>(R.id.targetWeightEditText)
        activityLevelSpinner = view.findViewById(R.id.activityLevelSpinner)
        genderRadioGroup = view.findViewById(R.id.genderRadioGroup)
        goalSpinner = view.findViewById(R.id.goalSpinner)
        val registerButton = view.findViewById<Button>(R.id.registerButton)
        val loginTextView = view.findViewById<TextView>(R.id.loginTextView)
        val passwordToggle = view.findViewById<TextView>(R.id.passwordToggle)

        setupSpinners()

        passwordToggle.setOnClickListener {
            val currentInputType = passwordEditText.inputType
            if (currentInputType == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD or
                android.text.InputType.TYPE_CLASS_TEXT) {
                passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
                passwordToggle.text = "🙈"
            } else {
                passwordEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD or
                        android.text.InputType.TYPE_CLASS_TEXT
                passwordToggle.text = "👁️"
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        registerButton.setOnClickListener {
            saveUser(
                emailEditText.text.toString(),
                passwordEditText.text.toString(),
                confirmPasswordEditText.text.toString(),
                nameEditText.text.toString(),
                ageEditText.text.toString(),
                heightEditText.text.toString(),
                currentWeightEditText.text.toString(),
                targetWeightEditText.text.toString()
            )
        }

        loginTextView.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        return view
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.activity_levels,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            activityLevelSpinner.adapter = adapter
        }
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.goals,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            goalSpinner.adapter = adapter
        }
    }

    private fun saveUser(
        email: String,
        password: String,
        confirmPassword: String,
        name: String,
        age: String,
        height: String,
        currentWeight: String,
        targetWeight: String
    ) {
        try {
            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email, пароль и имя", Toast.LENGTH_SHORT).show()
                return
            }

            val dbHelper = DatabaseHelper(requireContext())
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_EMAIL, email)
                put(DatabaseHelper.COLUMN_PASSWORD, password)
                put(DatabaseHelper.COLUMN_NAME, name)
                put(DatabaseHelper.COLUMN_AGE, age.toIntOrNull() ?: 0)
                put(DatabaseHelper.COLUMN_HEIGHT, height.toIntOrNull() ?: 0)
                put(DatabaseHelper.COLUMN_CURRENT_WEIGHT, currentWeight.toDoubleOrNull() ?: 0.0)
                put(DatabaseHelper.COLUMN_TARGET_WEIGHT, targetWeight.toDoubleOrNull() ?: 0.0)
                put(DatabaseHelper.COLUMN_GENDER, getGender())
                put(DatabaseHelper.COLUMN_ACTIVITY_LEVEL, activityLevelSpinner.selectedItem.toString())
                put(DatabaseHelper.COLUMN_GOAL, goalSpinner.selectedItem.toString())
                put(DatabaseHelper.COLUMN_REGISTER_DATE, System.currentTimeMillis())
            }

            val result = db.insert(DatabaseHelper.TABLE_USERS, null, values)

            if (result != -1L) {
                Toast.makeText(requireContext(), "Регистрация успешна! ID: $result", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "Ошибка регистрации", Toast.LENGTH_SHORT).show()
            }

            db.close()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun getGender(): String {
        return when (genderRadioGroup.checkedRadioButtonId) {
            R.id.maleRadioButton -> "Мужской"
            R.id.femaleRadioButton -> "Женский"
            else -> "Не указано"
        }
    }
}