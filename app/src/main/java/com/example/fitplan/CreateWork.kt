package com.example.fitplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.R

class CreateWork : Fragment() {
    private lateinit var exercisesContainer: LinearLayout
    private lateinit var workoutNameEditText: EditText
    private lateinit var addExerciseButton: Button
    private lateinit var saveWorkoutButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_work, container, false)

        exercisesContainer = view.findViewById(R.id.exercisesContainer)
        workoutNameEditText = view.findViewById(R.id.workoutNameEditText)
        addExerciseButton = view.findViewById(R.id.addExerciseButton)
        saveWorkoutButton = view.findViewById(R.id.saveWorkoutButton)

        setupListeners()

        return view
    }

    private fun setupListeners() {
        addExerciseButton.setOnClickListener {
            addExercise()
        }

        saveWorkoutButton.setOnClickListener {
            saveWorkout()
        }
    }

    private fun addExercise() {
        try {
            Toast.makeText(requireContext(), "Упражнение добавлено", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWorkout() {
        val workoutName = workoutNameEditText.text.toString().trim()

        if (workoutName.isEmpty()) {
            Toast.makeText(requireContext(), "Введите название тренировки", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(
            requireContext(),
            "Тренировка '$workoutName' сохранена!",
            Toast.LENGTH_LONG
        ).show()

        parentFragmentManager.popBackStack()
    }
}