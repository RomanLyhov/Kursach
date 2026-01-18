package com.example.fitplan.ui

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.Exercise
import com.example.fitplan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutProcessFragment : Fragment() {

    private lateinit var db: DatabaseHelper
    private var workoutId: Long = -1L
    private lateinit var containerExercises: LinearLayout
    private val activeTimers = mutableListOf<CountDownTimer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DatabaseHelper(requireContext())
        workoutId = arguments?.getLong("WORKOUT_ID", -1L) ?: -1L
        Log.d("WorkoutProcess", "Received workoutId: $workoutId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_workout_process, container, false)
        containerExercises = view.findViewById(R.id.containerExercises)
        loadExercises()
        return view
    }

    private fun loadExercises() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val exercises = db.getExercises(workoutId)
                Log.d("WorkoutProcess", "Loaded ${exercises.size} exercises for workout $workoutId")

                withContext(Dispatchers.Main) {
                    if (exercises.isEmpty()) {
                        val noExercisesText = TextView(requireContext()).apply {
                            text = "В этой тренировке нет упражнений"
                            textSize = 16f
                            setTextColor(Color.GRAY)
                            gravity = Gravity.CENTER
                            setPadding(0, 32, 0, 32)
                        }
                        containerExercises.addView(noExercisesText)
                        return@withContext
                    }
                    exercises.forEach { exercise ->
                        addExerciseCard(exercise)
                    }
                }
            } catch (e: Exception) {
                Log.e("PROCESS_CRASH", "Ошибка загрузки упражнений: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки упражнений", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeTimers.forEach { it.cancel() }
        activeTimers.clear()
    }

    private fun addExerciseCard(exercise: Exercise) {
        Log.d("WorkoutProcess", "Adding exercise: ${exercise.name} (sets: ${exercise.sets}, reps: ${exercise.reps})")

        val card = layoutInflater.inflate(
            R.layout.item_exercise_workout,
            containerExercises,
            false
        )

        val root = card.findViewById<LinearLayout>(R.id.rootCard)
        val tvName = card.findViewById<TextView>(R.id.tvExerciseName)
        val tvSets = card.findViewById<TextView>(R.id.tvSets)
        val tvReps = card.findViewById<TextView>(R.id.tvReps)
        val tvWeight = card.findViewById<TextView>(R.id.tvWeight)
        val tvRest = card.findViewById<TextView>(R.id.tvRest)
        val containerSets = card.findViewById<LinearLayout>(R.id.containerSets)
        val btnRest = card.findViewById<Button>(R.id.btnRest)
        val btnPause = card.findViewById<Button>(R.id.btnPause)
        val tvTimer = card.findViewById<TextView>(R.id.tvTimer)

        tvName.text = exercise.name
        tvSets.text = exercise.sets.toString()
        tvReps.text = exercise.reps.toString()
        tvWeight.text = "${exercise.weight} кг"
        tvRest.text = "${exercise.rest} сек"
        containerSets.removeAllViews()
        tvTimer.text = "${exercise.rest} сек"
        tvTimer.setTextColor(Color.parseColor("#4CAF50"))

        val setsDone = BooleanArray(exercise.sets)

        repeat(exercise.sets) { index ->
            val setView = TextView(requireContext()).apply {
                text = (index + 1).toString()
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER

                val size = 36 // dp
                val params = LinearLayout.LayoutParams(
                    (size * resources.displayMetrics.density).toInt(),
                    (size * resources.displayMetrics.density).toInt()
                )
                if (index < exercise.sets - 1) {
                    params.marginEnd = 8
                }
                layoutParams = params
                try {
                    val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.set_circle)
                    background = drawable ?: createCircleDrawable(Color.parseColor("#2196F3"))
                } catch (e: Exception) {
                    background = createCircleDrawable(Color.parseColor("#2196F3"))
                }
            }

            var clickCount = 0
            var locked = false

            setView.setOnClickListener {
                if (locked) return@setOnClickListener

                clickCount++

                if (clickCount == 2) {
                    locked = true
                    setsDone[index] = true

                    try {
                        val doneDrawable = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.set_circle_done
                        )
                        setView.background = doneDrawable ?: createCircleDrawable(Color.parseColor("#4CAF50"))
                    } catch (e: Exception) {
                        setView.background = createCircleDrawable(Color.parseColor("#4CAF50"))
                    }
                    setView.setTextColor(Color.WHITE)

                    val completedCount = setsDone.count { it }
                    if (completedCount == exercise.sets) {
                        root.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    }
                }
            }

            containerSets.addView(setView)
        }

        var currentTimer: CountDownTimer? = null
        var isTimerRunning = false
        var remainingTime = exercise.rest * 1000L
        var isPaused = false

        fun startTimer(totalTime: Long = exercise.rest * 1000L) {
            currentTimer?.cancel()

            currentTimer?.let { activeTimers.remove(it) }

            currentTimer = object : CountDownTimer(totalTime, 1000) {
                override fun onTick(ms: Long) {
                    remainingTime = ms
                    val seconds = ms / 1000

                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    tvTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)

                    when {
                        seconds <= 5 -> {
                            tvTimer.setTextColor(Color.RED)
                            btnRest.setBackgroundColor(Color.parseColor("#F44336"))
                        }
                        seconds <= 15 -> {
                            tvTimer.setTextColor(Color.parseColor("#FF9800"))
                            btnRest.setBackgroundColor(Color.parseColor("#FF9800"))
                        }
                        else -> {
                            tvTimer.setTextColor(Color.parseColor("#4CAF50"))
                            btnRest.setBackgroundColor(Color.parseColor("#4CAF50"))
                        }
                    }
                }

                override fun onFinish() {
                    tvTimer.text = "ГОТОВО! 💪"
                    tvTimer.setTextColor(Color.parseColor("#4CAF50"))
                    btnRest.text = "СТАРТ"
                    btnRest.setBackgroundColor(Color.parseColor("#4CAF50"))
                    btnPause.visibility = View.GONE
                    btnPause.text = "ПАУЗА"
                    btnPause.setBackgroundColor(Color.parseColor("#FF9800"))

                    isTimerRunning = false
                    isPaused = false
                    remainingTime = exercise.rest * 1000L

                    activeTimers.remove(this)
                }
            }.start()

            currentTimer?.let { activeTimers.add(it) }
            isTimerRunning = true
            isPaused = false
            btnRest.text = "СТОП"
            btnRest.setBackgroundColor(Color.parseColor("#F44336"))
            btnPause.visibility = View.VISIBLE
            btnPause.text = "ПАУЗА"
            btnPause.setBackgroundColor(Color.parseColor("#FF9800"))
        }

        btnRest.setOnClickListener {
            if (!isTimerRunning) {
                startTimer()
            } else {
                currentTimer?.cancel()
                currentTimer?.let { activeTimers.remove(it) }
                tvTimer.text = "${exercise.rest} сек"
                tvTimer.setTextColor(Color.parseColor("#4CAF50"))
                btnRest.text = "СТАРТ"
                btnRest.setBackgroundColor(Color.parseColor("#4CAF50"))
                btnPause.visibility = View.GONE

                isTimerRunning = false
                isPaused = false
                remainingTime = exercise.rest * 1000L
            }
        }

        btnPause.setOnClickListener {
            if (!isPaused) {
                currentTimer?.cancel()
                btnPause.text = "ПРОДОЛЖИТЬ"
                btnPause.setBackgroundColor(Color.parseColor("#4CAF50"))
                btnPause.setTextColor(Color.WHITE)

                val seconds = remainingTime / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                tvTimer.text = String.format("⏸ %02d:%02d", minutes, remainingSeconds)
                tvTimer.setTextColor(Color.GRAY)

                isPaused = true
            } else {
                btnPause.text = "ПАУЗА"
                btnPause.setBackgroundColor(Color.parseColor("#FF9800"))
                btnPause.setTextColor(Color.WHITE)
                startTimer(remainingTime)
            }
        }

        containerExercises.addView(card)
        Log.d("WorkoutProcess", "Exercise card added: ${exercise.name}")
    }

    private fun createCircleDrawable(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setSize(48, 48)
        }
    }
}