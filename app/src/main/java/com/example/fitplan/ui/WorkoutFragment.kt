package com.example.fitplan.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.Exercise
import com.example.fitplan.Models.Workout
import com.example.fitplan.R
import com.example.fitplan.ui.login.Login
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class WorkoutFragment : Fragment(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var db: DatabaseHelper
    private var userId: Long = -1L
    private lateinit var containerWorkouts: LinearLayout
    private lateinit var emptyStateCard: LinearLayout
    private lateinit var createWorkoutButton: Button
    private lateinit var btnRefresh: Button
    private val expandedWorkouts = mutableSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация базы данных и Job для корутин
        db = DatabaseHelper(requireContext())
        job = Job()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Инициализация основного layout фрагмента
        val view = inflater.inflate(R.layout.fragment_workout, container, false)
        containerWorkouts = view.findViewById(R.id.containerWorkouts)
        emptyStateCard = view.findViewById(R.id.emptyStateCard)
        createWorkoutButton = view.findViewById(R.id.createWorkoutButton)
        btnRefresh = view.findViewById(R.id.btnRefresh)

        // Обработчик нажатия на кнопку создания тренировки
        // Переход на фрагмент CreateWorkoutFragment
        createWorkoutButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CreateWorkoutFragment())
                .addToBackStack(null)
                .commit()
        }

        // Обработчик нажатия на кнопку обновления
        // Очищает список развернутых тренировок и загружает данные заново
        btnRefresh.setOnClickListener {
            expandedWorkouts.clear()
            loadWorkouts()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получение ID текущего пользователя из SharedPreferences
        userId = requireContext()
            .getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
            .getLong("user_id", -1L)

        Log.d("WORKOUT", "onViewCreated → UserId = $userId")

        // Если пользователь не авторизован, перенаправляем на экран логина
        if (userId == -1L) {
            Log.e("WORKOUT", "USER_ID = -1 → переход на Login")
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, Login())
                .commit()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        // При возобновлении фрагмента загружаем актуальный список тренировок
        loadWorkouts()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Отмена всех корутин при уничтожении фрагмента
        job.cancel()
    }


     // Загружает список тренировок пользователя из базы данных

    private fun loadWorkouts() {
        // Проверка авторизации пользователя
        if (userId == -1L) return

        launch {
            try {
                // Получение тренировок из базы данных в IO потоке
                val workouts = withContext(Dispatchers.IO) {
                    db.getWorkoutsByUser(userId)
                }

                // Очистка контейнера перед загрузкой новых данных
                containerWorkouts.removeAllViews()

                if (workouts.isEmpty()) {
                    // Если тренировок нет, показываем состояние "пусто"
                    emptyStateCard.visibility = View.VISIBLE
                } else {
                    // Скрываем состояние "пусто" и добавляем карточки тренировок
                    emptyStateCard.visibility = View.GONE
                    workouts.forEach { addWorkoutCard(it) }
                }
            } catch (e: Exception) {
                // Обработка ошибок при загрузке тренировок
                Log.e("WORKOUT_CRASH", "Ошибка загрузки тренировок", e)
                emptyStateCard.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Ошибка загрузки тренировок", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //Создает и добавляет карточку тренировки в интерфейс

    private fun addWorkoutCard(workout: Workout) {
        // Создание контейнера для карточки тренировки и упражнений
        val workoutContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        // Инфлейт макета карточки тренировки
        val workoutCard = layoutInflater.inflate(R.layout.workout_card, workoutContainer, false)

        // Получение ссылок на элементы карточки
        val cardRoot = workoutCard.findViewById<LinearLayout>(R.id.cardRoot)
        val tvWorkoutName = workoutCard.findViewById<TextView>(R.id.tvWorkoutName)
        val tvExerciseCount = workoutCard.findViewById<TextView>(R.id.tvExerciseCount)
        val actionsContainer = workoutCard.findViewById<LinearLayout>(R.id.actionsContainer)

        // Создание контейнера для упражнений (скрыт по умолчанию)
        val exercisesContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (expandedWorkouts.contains(workout.id)) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 0, 16, 16)
            }
        }

        val btnStart = workoutCard.findViewById<Button>(R.id.btnStartWorkout)
        val btnEdit = workoutCard.findViewById<Button>(R.id.btnEditWorkout)

        // Асинхронная загрузка упражнений для текущей тренировки
        launch {
            try {
                val exercises = withContext(Dispatchers.IO) {
                    db.getExercises(workout.id)
                }

                // Обновление UI с загруженными данными
                tvExerciseCount.text = "${exercises.size} упражнений"
                tvWorkoutName.text = if (expandedWorkouts.contains(workout.id))
                    "▼ ${workout.name}" else "▶ ${workout.name}"

                actionsContainer.visibility =
                    if (expandedWorkouts.contains(workout.id)) View.VISIBLE else View.GONE

                // Обработчик нажатия на карточку для разворачивания/сворачивания
                cardRoot.setOnClickListener {
                    toggleWorkout(workout, tvWorkoutName, actionsContainer, exercisesContainer, exercises)
                }

                // Если тренировка уже развернута, добавляем упражнения
                if (expandedWorkouts.contains(workout.id)) {
                    addExercisesToContainer(exercises, exercisesContainer)
                }

                // Обработчик нажатия на кнопку "Начать тренировку"
                btnStart.setOnClickListener {
                    val bundle = Bundle().apply { putLong("WORKOUT_ID", workout.id) }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, WorkoutProcessFragment().apply { arguments = bundle })
                        .addToBackStack(null)
                        .commit()
                }

                // Обработчик нажатия на кнопку "Редактировать"
                btnEdit.setOnClickListener {
                    val bundle = Bundle().apply { putLong("WORKOUT_ID", workout.id) }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CreateWorkoutFragment().apply { arguments = bundle })
                        .addToBackStack(null)
                        .commit()
                }
            } catch (e: Exception) {
                Log.e("WORKOUT", "Ошибка загрузки упражнений для тренировки ${workout.id}", e)
            }
        }

        // Добавление карточки в контейнер
        workoutContainer.addView(workoutCard)
        workoutContainer.addView(exercisesContainer)
        containerWorkouts.addView(workoutContainer)
    }


     // Разворачивает или сворачивает карточку тренировки
     //Показывает/скрывает список упражнений и кнопки действий

    private fun toggleWorkout(
        workout: Workout,
        tvTitle: TextView,
        actions: LinearLayout,
        exercisesContainer: LinearLayout,
        exercises: List<Exercise>
    ) {
        if (expandedWorkouts.contains(workout.id)) {
            // Сворачивание карточки
            expandedWorkouts.remove(workout.id)
            tvTitle.text = "▶ ${workout.name}"
            actions.visibility = View.GONE
            exercisesContainer.visibility = View.GONE
            exercisesContainer.removeAllViews()
        } else {
            // Разворачивание карточки
            expandedWorkouts.add(workout.id)
            tvTitle.text = "▼ ${workout.name}"
            actions.visibility = View.VISIBLE
            exercisesContainer.visibility = View.VISIBLE
            addExercisesToContainer(exercises, exercisesContainer)
        }
    }

    //* Добавляет карточки упражнений в контейнер
    private fun addExercisesToContainer(exercises: List<Exercise>, container: LinearLayout) {
        container.removeAllViews()

        // Если упражнений нет, показываем заглушку
        if (exercises.isEmpty()) {
            val textView = TextView(requireContext()).apply {
                text = "Нет упражнений"
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 14f
                setPadding(16, 8, 16, 8)
            }
            container.addView(textView)
            return
        }

        // Создание карточек для каждого упражнения
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