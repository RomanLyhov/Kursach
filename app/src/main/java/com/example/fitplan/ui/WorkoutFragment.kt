package com.example.fitplan.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.Exercise
import com.example.fitplan.Models.Workout
import com.example.fitplan.R
import com.example.fitplan.ui.login.Login

class WorkoutFragment : Fragment() {

    private lateinit var db: DatabaseHelper
    private var userId: Long = -1L
    private lateinit var containerWorkouts: LinearLayout
    private val expandedWorkouts = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DatabaseHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_workout, container, false)
        containerWorkouts = view.findViewById(R.id.containerWorkouts)

        view.findViewById<TextView>(R.id.createWorkoutButton).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CreateWorkoutFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            expandedWorkouts.clear()
            loadWorkouts()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        userId = requireContext()
            .getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
            .getLong("user_id", -1L)

        Log.d("WORKOUT", "onResume → UserId = $userId")

        Handler(Looper.getMainLooper()).postDelayed({ loadWorkouts() }, 100)
    }

    private fun loadWorkouts() {
        containerWorkouts.removeAllViews()

        if (userId == -1L) {
            Log.e("WORKOUT", "USER_ID = -1 → переход на Login")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Login())
                .commit()
            return
        }

        Thread {
            try {
                val workouts = db.getWorkoutsByUser(userId)
                if (!isAdded) return@Thread

                requireActivity().runOnUiThread {
                    if (workouts.isEmpty()) {
                        containerWorkouts.addView(TextView(requireContext()).apply {
                            text = "У вас пока нет тренировок"
                            textSize = 16f
                            setPadding(16, 16, 16, 16)
                        })
                        return@runOnUiThread
                    }

                    workouts.forEach { addWorkoutCard(it) }
                }
            } catch (e: Exception) {
                Log.e("WORKOUT_CRASH", "Ошибка загрузки тренировок", e)
            }
        }.start()
    }

    private fun addWorkoutCard(workout: Workout) {
        val workoutContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val workoutCard = layoutInflater.inflate(R.layout.workout_card, workoutContainer, false)

        val cardRoot = workoutCard.findViewById<CardView>(R.id.cardRoot)
        val tvWorkoutName = workoutCard.findViewById<TextView>(R.id.tvWorkoutName)
        val tvExerciseCount = workoutCard.findViewById<TextView>(R.id.tvExerciseCount)
        val actionsContainer = workoutCard.findViewById<LinearLayout>(R.id.actionsContainer)

        val exercisesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (expandedWorkouts.contains(workout.id)) View.VISIBLE else View.GONE
        }

        val btnStart = workoutCard.findViewById<Button>(R.id.btnStartWorkout)
        val btnEdit = workoutCard.findViewById<Button>(R.id.btnEditWorkout)

        Thread {
            val exercises = db.getExercises(workout.id)
            if (!isAdded) return@Thread

            requireActivity().runOnUiThread {
                tvExerciseCount.text = "${exercises.size} упражнений"
                tvWorkoutName.text = if (expandedWorkouts.contains(workout.id))
                    "▼ ${workout.name}" else "▶ ${workout.name}"

                actionsContainer.visibility =
                    if (expandedWorkouts.contains(workout.id)) View.VISIBLE else View.GONE

                cardRoot.setOnClickListener {
                    toggleWorkout(workout, tvWorkoutName, actionsContainer, exercisesContainer, exercises)
                }

                if (expandedWorkouts.contains(workout.id)) {
                    addExercisesToContainer(exercises, exercisesContainer)
                }

                btnStart.setOnClickListener {
                    val bundle = Bundle().apply { putLong("WORKOUT_ID", workout.id) }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, WorkoutProcessFragment().apply { arguments = bundle })
                        .addToBackStack(null)
                        .commit()
                }

                btnEdit.setOnClickListener {
                    val bundle = Bundle().apply { putLong("WORKOUT_ID", workout.id) }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CreateWorkoutFragment().apply { arguments = bundle })
                        .addToBackStack(null)
                        .commit()
                }
            }
        }.start()

        workoutContainer.addView(workoutCard)
        workoutContainer.addView(exercisesContainer)
        containerWorkouts.addView(workoutContainer)
    }

    private fun toggleWorkout(
        workout: Workout,
        tvTitle: TextView,
        actions: LinearLayout,
        exercisesContainer: LinearLayout,
        exercises: List<Exercise>
    ) {
        if (expandedWorkouts.contains(workout.id)) {
            expandedWorkouts.remove(workout.id)
            tvTitle.text = "▶ ${workout.name}"
            actions.visibility = View.GONE
            exercisesContainer.visibility = View.GONE
        } else {
            expandedWorkouts.add(workout.id)
            tvTitle.text = "▼ ${workout.name}"
            actions.visibility = View.VISIBLE
            exercisesContainer.visibility = View.VISIBLE
            addExercisesToContainer(exercises, exercisesContainer)
        }
    }

    private fun addExercisesToContainer(exercises: List<Exercise>, container: LinearLayout) {
        container.removeAllViews()
        if (exercises.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "Нет упражнений"
                setPadding(16, 8, 16, 8)
            })
            return
        }

        exercises.forEach { exercise ->
            val card = layoutInflater.inflate(R.layout.exercise_card, container, false)
            card.findViewById<TextView>(R.id.tvExerciseName).text = exercise.name
            card.findViewById<TextView>(R.id.tvSets).text = exercise.sets.toString()
            card.findViewById<TextView>(R.id.tvReps).text = exercise.reps.toString()
            card.findViewById<TextView>(R.id.tvWeight).text = "${exercise.weight} кг"
            card.findViewById<TextView>(R.id.tvRest)?.text = "${exercise.rest} сек"
            container.addView(card)
        }
    }
}
