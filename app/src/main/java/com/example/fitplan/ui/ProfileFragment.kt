package com.example.fitplan.ui

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
import com.example.fitplan.ui.login.Login
import kotlinx.coroutines.*

class ProfileFragment : Fragment() {

    private lateinit var db: DatabaseHelper
    private var userId: Long = -1L

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvTargetWeight: TextView
    private lateinit var tvActivity: TextView
    private lateinit var tvGoal: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button

    private lateinit var tvDailyCalories: TextView
    private lateinit var tvProteinGoal: TextView
    private lateinit var tvFatGoal: TextView
    private lateinit var tvCarbsGoal: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        db = DatabaseHelper(requireContext())

        val prefs = requireContext().getSharedPreferences("session", Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1L)

        tvName = view.findViewById(R.id.userNameTextView)
        tvEmail = view.findViewById(R.id.userEmailTextView)
        tvAge = view.findViewById(R.id.ageTextView)
        tvHeight = view.findViewById(R.id.heightTextView)
        tvWeight = view.findViewById(R.id.weightTextView)
        tvTargetWeight = view.findViewById(R.id.targetWeightTextView)
        tvActivity = view.findViewById(R.id.activityLevelTextView)
        tvGoal = view.findViewById(R.id.goalTextView)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)

        tvDailyCalories = view.findViewById(R.id.dailyCaloriesTextView)
        tvProteinGoal = view.findViewById(R.id.proteinGoalTextView)
        tvFatGoal = view.findViewById(R.id.fatGoalTextView)
        tvCarbsGoal = view.findViewById(R.id.carbsGoalTextView)

        btnLogout.setOnClickListener { logout() }
        btnEditProfile.setOnClickListener {
            val mainActivity = activity as? MainActivity3
            mainActivity?.currentUser?.let { user ->
                val dialog = EditProfileDialog(user, db) {
                    refreshUserData()
                }
                dialog.show(parentFragmentManager, "edit_profile")
            } ?: run {
                Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            }
        }

        loadUser()

        return view
    }

    private fun loadUser() {
        if (userId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val user = db.getUserById(userId)
            withContext(Dispatchers.Main) {
                if (user != null) {
                    (activity as? MainActivity3)?.currentUser = user
                    updateUI(user)
                } else {
                    Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun refreshUserData() {
        if (userId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val user = db.getUserById(userId)
            withContext(Dispatchers.Main) {
                user?.let {
                    (activity as? MainActivity3)?.currentUser = it
                    updateUI(it)
                    Toast.makeText(context, "Данные обновлены", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(user: User) {
        tvName.text = user.name
        tvEmail.text = user.email
        tvAge.text = user.age?.toString() ?: "-"
        tvHeight.text = "${user.height?.toString() ?: "-"} см"
        tvWeight.text = "${user.weight?.toString() ?: "-"} кг"
        tvTargetWeight.text = "${user.targetWeight?.toString() ?: "-"} кг"
        tvActivity.text = translateActivityLevel(user.activity)
        tvGoal.text = translateGoal(user.goal)
        val caloriesGoal = user.dailyCaloriesGoal?.toString() ?: "-"
        val proteinGoal = user.dailyProteinGoal?.toString() ?: "-"
        val fatGoal = user.dailyFatGoal?.toString() ?: "-"
        val carbsGoal = user.dailyCarbsGoal?.toString() ?: "-"
        tvDailyCalories.text = "Калории в день: $caloriesGoal ккал"
        tvProteinGoal.text = "Белки: $proteinGoal г"
        tvFatGoal.text = "Жиры: $fatGoal г"
        tvCarbsGoal.text = "Углеводы: $carbsGoal г"
    }

    private fun translateActivityLevel(activityLevel: String?): String {
        return when (activityLevel?.toUpperCase()) {
            "LIGHT" -> "Малая активность"
            "MODERATE", "ACTIVE" -> "Средняя активность"
            "VERY_ACTIVE" -> "Высокая активность"
            else -> activityLevel ?: "-"
        }
    }

    private fun translateGoal(goal: String?): String {
        return when (goal?.toUpperCase()) {
            "WEIGHT_LOSS" -> "Снижение веса"
            "WEIGHT_GAIN" -> "Набор массы"
            "MAINTENANCE" -> "Поддержание веса"
            else -> goal ?: "-"
        }
    }

    private fun logout() {
        requireContext().getSharedPreferences("session", Context.MODE_PRIVATE)
            .edit().clear().apply()
        (activity as? MainActivity3)?.currentUser = null
        (activity as? MainActivity3)?.findViewById<LinearLayout>(R.id.bottom_navigation)
            ?.visibility = View.GONE
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, Login())
            .commit()
    }
}