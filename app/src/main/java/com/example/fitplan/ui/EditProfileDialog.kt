package com.example.fitplan.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.User
import com.example.fitplan.R
import com.example.fitplan.Utils.MacroCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileDialog(
    private var user: User,
    private val db: DatabaseHelper,
    private val onSave: () -> Unit
) : DialogFragment() {

    private lateinit var tvCalculatedGoals: TextView
    private lateinit var spGender: Spinner
    private lateinit var spGoal: Spinner
    private lateinit var spActivity: Spinner

    private val genderMapping = mapOf(
        "Мужской" to "male",
        "Женский" to "female"
    )

    private val activityMapping = mapOf(
        "Малая активность" to "LIGHT",
        "Средняя активность" to "MODERATE",
        "Высокая активность" to "VERY_ACTIVE",
    )

    private val goalMapping = mapOf(
        "Снижение веса" to "WEIGHT_LOSS",
        "Набор массы" to "WEIGHT_GAIN",
        "Поддержание веса" to "MAINTENANCE",
    )

    private var lastCalculatedGoals: MacroCalculator.MacroGoals? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_edit_profile_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etAge = view.findViewById<EditText>(R.id.etAge)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)
        val etHeight = view.findViewById<EditText>(R.id.etHeight)
        val etTargetWeight = view.findViewById<EditText>(R.id.etTargetWeight)
        spGender = view.findViewById<Spinner>(R.id.spGender)
        spGoal = view.findViewById<Spinner>(R.id.spGoal)
        spActivity = view.findViewById<Spinner>(R.id.spActivity)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        tvCalculatedGoals = view.findViewById(R.id.tvCalculatedGoals)
        etName.setText(user.name)
        etEmail.setText(user.email)
        etAge.setText(user.age?.toString() ?: "")
        etWeight.setText(user.weight?.toString() ?: "")
        etHeight.setText(user.height?.toString() ?: "")
        etTargetWeight.setText(user.targetWeight?.toString() ?: "")
        setupSpinners()

        @SuppressLint("SetTextI18n")
        fun calculateAndDisplayGoals() {
            val age = etAge.text.toString().toIntOrNull()
            val weight = etWeight.text.toString().toIntOrNull()
            val height = etHeight.text.toString().toIntOrNull()
            val gender = getSelectedGender()
            val activity = getSelectedActivityLevel()
            val goal = getSelectedGoal()

            if (age != null && weight != null && height != null &&
                gender != null && activity != null && goal != null) {

                val tempUser = User(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    password = user.password,
                    age = age,
                    weight = weight,
                    height = height,
                    gender = gender,
                    activity = activity,
                    goal = goal,
                    targetWeight = user.targetWeight,
                    registerDate = user.registerDate,
                    profileImage = user.profileImage,
                    dailyCaloriesGoal = user.dailyCaloriesGoal,
                    dailyProteinGoal = user.dailyProteinGoal,
                    dailyFatGoal = user.dailyFatGoal,
                    dailyCarbsGoal = user.dailyCarbsGoal
                )

                lastCalculatedGoals = MacroCalculator.calculateDailyGoals(tempUser)

                lastCalculatedGoals?.let { calculatedGoals ->
                    tvCalculatedGoals.text = """
                        Дневные цели (после сохранения):
                        Калории: ${calculatedGoals.calories} ккал
                        Белки: ${calculatedGoals.protein} г
                        Жиры: ${calculatedGoals.fat} г
                        Углеводы: ${calculatedGoals.carbs} г
                    """.trimIndent()
                } ?: run {
                    tvCalculatedGoals.text = """
                        Текущие цели:
                        Калории: ${user.dailyCaloriesGoal ?: "не задано"} ккал
                        Белки: ${user.dailyProteinGoal ?: "не задано"} г
                        Жиры: ${user.dailyFatGoal ?: "не задано"} г
                        Углеводы: ${user.dailyCarbsGoal ?: "не задано"} г
                    """.trimIndent()
                }
            } else {
                tvCalculatedGoals.text = "Заполните все поля для расчета целей"
            }
        }

        val fieldsToWatch = listOf(etAge, etWeight, etHeight)
        fieldsToWatch.forEach { field ->
            field.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    calculateAndDisplayGoals()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        spGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateAndDisplayGoals()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spGoal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateAndDisplayGoals()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spActivity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateAndDisplayGoals()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        calculateAndDisplayGoals()

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val age = etAge.text.toString().toIntOrNull()
            val weight = etWeight.text.toString().toIntOrNull()
            val height = etHeight.text.toString().toIntOrNull()
            val targetWeight = etTargetWeight.text.toString().toIntOrNull()
            val genderKey = getSelectedGender()
            val goalKey = getSelectedGoal()
            val activityKey = getSelectedActivityLevel()

            if (name.isEmpty() || email.isEmpty() || age == null || weight == null || height == null) {
                Toast.makeText(requireContext(), "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Некорректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calculatedGoals = lastCalculatedGoals ?: if (age != null && weight != null && height != null &&
                genderKey != null && activityKey != null && goalKey != null) {

                val tempUser = User(
                    age = age,
                    weight = weight,
                    height = height,
                    gender = genderKey,
                    activity = activityKey,
                    goal = goalKey
                )
                MacroCalculator.calculateDailyGoals(tempUser)
            } else {
                null
            }
            Log.d("EditProfileDialog", "Calculated goals: $calculatedGoals")
            val updatedUser = user.copy(
                id = user.id,
                name = name,
                email = email,
                password = user.password,
                age = age,
                weight = weight,
                height = height,
                targetWeight = targetWeight,
                gender = genderKey ?: user.gender,
                goal = goalKey ?: user.goal,
                activity = activityKey ?: user.activity,
                registerDate = user.registerDate,
                profileImage = user.profileImage,
                dailyCaloriesGoal = calculatedGoals?.calories,
                dailyProteinGoal = calculatedGoals?.protein,
                dailyFatGoal = calculatedGoals?.fat,
                dailyCarbsGoal = calculatedGoals?.carbs
            )

            Log.d("EditProfileDialog", "Saving user with goals: calories=${updatedUser.dailyCaloriesGoal}, protein=${updatedUser.dailyProteinGoal}")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.updateUser(updatedUser)
                    Log.d("EditProfileDialog", "User saved to database")
                    val mainActivity = activity as? MainActivity3
                    mainActivity?.currentUser = updatedUser
                    Log.d("EditProfileDialog", "Activity user updated")

                    withContext(Dispatchers.Main) {
                        onSave()
                        if (calculatedGoals != null) {
                            Toast.makeText(
                                requireContext(),
                                "Профиль обновлен! Новые цели: ${calculatedGoals.calories} ккал",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(requireContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show()
                        }

                        dismiss()
                    }
                } catch (e: Exception) {
                    Log.e("EditProfileDialog", "Error saving user: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupSpinners() {
        val genders = listOf("Мужской", "Женский")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spGender.adapter = genderAdapter

        val currentGender = when (user.gender?.toLowerCase()) {
            "male" -> "Мужской"
            "female" -> "Женский"
            else -> "Мужской"
        }
        spGender.setSelection(genders.indexOf(currentGender))
        val activityNames = activityMapping.keys.toList()
        val activityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, activityNames)
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spActivity.adapter = activityAdapter

        val currentActivity = when (user.activity?.toUpperCase()) {
            "LIGHT" -> "Малая активность"
            "MODERATE", "ACTIVE" -> "Средняя активность"
            "VERY_ACTIVE" -> "Высокая активность"
            else -> "Средняя активность"
        }
        spActivity.setSelection(activityNames.indexOf(currentActivity))

        val goalNames = goalMapping.keys.toList()
        val goalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, goalNames)
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spGoal.adapter = goalAdapter
        val currentGoal = when (user.goal?.toUpperCase()) {
            "WEIGHT_LOSS" -> "Снижение веса"
            "WEIGHT_GAIN" -> "Набор массы"
            "MAINTENANCE" -> "Поддержание веса"
            else -> "Поддержание веса"
        }
        spGoal.setSelection(goalNames.indexOf(currentGoal))
    }

    private fun getSelectedGender(): String? {
        val genderText = spGender.selectedItem.toString()
        return genderMapping[genderText]
    }

    private fun getSelectedActivityLevel(): String? {
        val activityText = spActivity.selectedItem.toString()
        return activityMapping[activityText]
    }

    private fun getSelectedGoal(): String? {
        val goalText = spGoal.selectedItem.toString()
        return goalMapping[goalText]
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}