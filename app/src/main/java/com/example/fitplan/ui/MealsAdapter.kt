package com.example.fitplan.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitplan.Models.Meal
import com.example.fitplan.R

class MealsAdapter(
    private var meals: List<Meal>,
    private val onAddClick: (mealType: String) -> Unit
) : RecyclerView.Adapter<MealsAdapter.MealViewHolder>() {

    inner class MealViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMealName: TextView = view.findViewById(R.id.tvMealName)
        val btnAddMeal: Button = view.findViewById(R.id.btnAddMeal)
        val tvCalories: TextView = view.findViewById(R.id.tvMealCalories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun getItemCount(): Int = meals.size

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.tvMealName.text = meal.mealType ?: meal.productName
        holder.tvCalories.text = "${meal.calories} kcal"

        holder.btnAddMeal.setOnClickListener {
            onAddClick(meal.mealType ?: "")
        }
    }

    fun updateData(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }
}