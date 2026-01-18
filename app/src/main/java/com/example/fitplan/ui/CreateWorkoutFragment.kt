package com.example.fitplan.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.Exercise
import com.example.fitplan.R
import com.example.fitplan.ui.login.Login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateWorkoutFragment : Fragment() {

    private lateinit var db: DatabaseHelper
    private var userId = -1L
    private var workoutId = -1L
    private var isEditMode = false
    private var editingExerciseIndex: Int? = null
    private val exercises = mutableListOf<Exercise>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DatabaseHelper(requireContext())

        // Получаем userId из SharedPreferences
        val prefs = requireContext().getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        userId = prefs.getLong("user_id", -1L)

        Log.d("CreateWorkoutFragment", "User ID from SharedPreferences: $userId")

        workoutId = arguments?.getLong("WORKOUT_ID", -1) ?: -1
        isEditMode = workoutId != -1L

        Log.d("CreateWorkoutFragment", "isEditMode: $isEditMode, workoutId: $workoutId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create_workout, container, false)

        val etWorkoutName = view.findViewById<EditText>(R.id.etWorkoutName)
        val etExerciseName = view.findViewById<EditText>(R.id.etExerciseName)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)
        val etRest = view.findViewById<EditText>(R.id.etRestTime)
        val tvSets = view.findViewById<TextView>(R.id.tvSetsCount)
        val tvReps = view.findViewById<TextView>(R.id.tvRepsCount)
        val tvExercisesCount = view.findViewById<TextView>(R.id.tvExercisesCount)
        val containerExercises = view.findViewById<LinearLayout>(R.id.containerExercises)
        val btnAddExercise = view.findViewById<TextView>(R.id.createWorkoutButton)
        val btnSaveWorkout = view.findViewById<TextView>(R.id.btnCompleteWorkout)

        if (isEditMode) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("CreateWorkoutFragment", "Loading workout with ID: $workoutId")
                    val workout = db.getWorkoutById(workoutId)
                    val list = db.getExercises(workoutId)

                    Log.d("CreateWorkoutFragment", "Workout loaded: ${workout != null}")
                    Log.d("CreateWorkoutFragment", "Exercises loaded: ${list.size}")

                    withContext(Dispatchers.Main) {
                        if (workout == null) {
                            Toast.makeText(requireContext(), "Тренировка не найдена", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            return@withContext
                        }

                        etWorkoutName.setText(workout.name)
                        exercises.clear()
                        exercises.addAll(list)
                        updateExerciseList(containerExercises, tvExercisesCount)
                        btnSaveWorkout.text = "Сохранить изменения"
                        Toast.makeText(requireContext(), "Загружено ${list.size} упражнений", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("CREATE_CRASH", "edit load error", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка загрузки тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.btnIncreaseSets).setOnClickListener {
            tvSets.text = (tvSets.text.toString().toInt() + 1).toString()
        }
        view.findViewById<TextView>(R.id.btnDecreaseSets).setOnClickListener {
            val v = tvSets.text.toString().toInt()
            if (v > 1) tvSets.text = (v - 1).toString()
        }

        view.findViewById<TextView>(R.id.btnIncreaseReps).setOnClickListener {
            tvReps.text = (tvReps.text.toString().toInt() + 1).toString()
        }
        view.findViewById<TextView>(R.id.btnDecreaseReps).setOnClickListener {
            val v = tvReps.text.toString().toInt()
            if (v > 1) tvReps.text = (v - 1).toString()
        }

        btnAddExercise.setOnClickListener {
            val name = etExerciseName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название упражнения", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val exercise = Exercise(
                id = 0,
                workoutId = workoutId,
                name = name,
                sets = tvSets.text.toString().toInt(),
                reps = tvReps.text.toString().toInt(),
                weight = etWeight.text.toString().toIntOrNull() ?: 0,
                rest = etRest.text.toString().toIntOrNull() ?: 90
            )

            if (editingExerciseIndex == null) {
                exercises.add(exercise)
            } else {
                exercises[editingExerciseIndex!!] = exercise
                editingExerciseIndex = null
                btnAddExercise.text = "Добавить упражнение"
            }

            updateExerciseList(containerExercises, tvExercisesCount)

            etExerciseName.text.clear()
            etWeight.text.clear()
            etRest.setText("90")
            tvSets.text = "3"
            tvReps.text = "10"
        }

        btnSaveWorkout.setOnClickListener {
            if (userId == -1L) {
                Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, Login())
                    .commit()
                return@setOnClickListener
            }

            val workoutName = etWorkoutName.text.toString().trim()
            if (workoutName.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название тренировки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (exercises.isEmpty()) {
                Toast.makeText(requireContext(), "Добавьте хотя бы одно упражнение", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (isEditMode) {
                        val updated = db.updateWorkout(workoutId, workoutName)
                        Log.d("CreateWorkoutFragment", "Updated workout: $updated")

                        db.deleteExercisesByWorkout(workoutId)
                        Log.d("CreateWorkoutFragment", "Deleted old exercises")

                        exercises.forEach { exercise ->
                            val exerciseId = db.addExerciseToWorkout(workoutId, exercise)
                            Log.d("CreateWorkoutFragment", "Added exercise: $exerciseId")
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Тренировка сохранена", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    } else {
                        val newWorkoutId = db.addWorkout(userId, workoutName)
                        Log.d("CreateWorkoutFragment", "Created new workout ID: $newWorkoutId")

                        if (newWorkoutId == -1L) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Ошибка создания тренировки", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        exercises.forEach { exercise ->
                            val exerciseId = db.addExerciseToWorkout(newWorkoutId, exercise)
                            Log.d("CreateWorkoutFragment", "Added exercise $exerciseId to workout $newWorkoutId")
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Тренировка создана", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CreateWorkoutFragment", "Error saving workout", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return view
    }

    private fun updateExerciseList(container: LinearLayout, countTextView: TextView? = null) {
        container.removeAllViews()
        container.visibility = if (exercises.isEmpty()) View.GONE else View.VISIBLE

        exercises.forEachIndexed { index, e ->
            val card = layoutInflater.inflate(R.layout.exercise_card, container, false)
            card.findViewById<TextView>(R.id.tvExerciseName).text = e.name
            card.findViewById<TextView>(R.id.tvSets).text = e.sets.toString()
            card.findViewById<TextView>(R.id.tvReps).text = e.reps.toString()
            card.findViewById<TextView>(R.id.tvWeight).text = "${e.weight} кг"
            card.findViewById<TextView>(R.id.tvRest).text = "${e.rest} сек"

            card.findViewById<TextView>(R.id.btnRemoveExercise).setOnClickListener {
                exercises.removeAt(index)
                updateExerciseList(container, countTextView)
            }

            card.setOnClickListener {
                editingExerciseIndex = index
                view?.apply {
                    findViewById<EditText>(R.id.etExerciseName).setText(e.name)
                    findViewById<TextView>(R.id.tvSetsCount).text = e.sets.toString()
                    findViewById<TextView>(R.id.tvRepsCount).text = e.reps.toString()
                    findViewById<EditText>(R.id.etWeight).setText(e.weight.toString())
                    findViewById<EditText>(R.id.etRestTime).setText(e.rest.toString())
                    findViewById<TextView>(R.id.createWorkoutButton).text = "Сохранить изменения"
                }
            }

            container.addView(card)
        }
        countTextView?.text = "${exercises.size} упражнений"
    }
}