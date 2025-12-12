package com.example.fitplan.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.fitplan.CreateWork
import com.example.fitplan.R

class WorkoutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout, container, false)

        val createWorkoutButton = Button(requireContext()).apply {
            text = "Создать новую тренировку"
            setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
            setTextColor(resources.getColor(android.R.color.white))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val createWorkoutFragment = CreateWork()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, createWorkoutFragment)
                    .addToBackStack("workout")
                    .commit()
            }
        }

        val layout = view.findViewById<android.widget.LinearLayout>(android.R.id.content)
        layout?.addView(createWorkoutButton)

        return view
    }
}