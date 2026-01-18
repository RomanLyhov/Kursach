// MealsAdapter.kt в com.example.fitplan.ui.adapters
package com.example.fitplan.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.fitplan.Models.MealProductDisplay
import com.example.fitplan.R
import com.example.fitplan.ui.models.DisplayMeal

class MealsAdapter(
    private val meals: MutableList<DisplayMeal>,
    private val onAddClick: (String) -> Unit,
    private val onMealClick: (String) -> Unit,
    private val onEditProduct: (MealProductDisplay, String) -> Unit,
    private val onDeleteProduct: (MealProductDisplay, String) -> Unit
) : RecyclerView.Adapter<MealsAdapter.MealVH>() {

    private val expandedMeals = mutableSetOf<String>()
    private val mealProductsMap = mutableMapOf<String, List<MealProductDisplay>>()

    inner class MealVH(v: View) : RecyclerView.ViewHolder(v) {
        private val cardRoot: View = v.findViewById(R.id.mealCardRoot)
        private val tvMealName: TextView = v.findViewById(R.id.tvMealName)
        private val tvMealCalories: TextView = v.findViewById(R.id.tvMealCalories)
        private val tvMealDescription: TextView = v.findViewById(R.id.tvMealDescription)
        private val btnAddMealItem: Button = v.findViewById(R.id.btnAddMealItem)
        private val productsContainer: LinearLayout = v.findViewById(R.id.productsContainer)
        private val arrowIcon: TextView = v.findViewById(R.id.arrowIcon)

        fun bind(meal: DisplayMeal) {
            arrowIcon.text = if (meal.isExpanded) "▼" else "▶"
            tvMealName.text = meal.mealType
            tvMealCalories.text = "${meal.totalCalories} ккал"
            tvMealDescription.text = meal.description

            productsContainer.visibility = if (meal.isExpanded) View.VISIBLE else View.GONE

            if (meal.isExpanded) {
                val products = mealProductsMap[meal.mealType] ?: emptyList()
                fillProductsContainer(products, meal.mealType)
            }

            cardRoot.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMealClick(meal.mealType)
                }
            }

            btnAddMealItem.setOnClickListener {
                onAddClick(meal.mealType)
            }

            if (meal.productCount > 0) {
                tvMealCalories.setTextColor(itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
                tvMealDescription.setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null))
            } else {
                tvMealCalories.setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null))
                tvMealDescription.setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null))
            }
        }

        private fun fillProductsContainer(products: List<MealProductDisplay>, mealType: String) {
            productsContainer.removeAllViews()

            if (products.isEmpty()) {
                val textView = TextView(itemView.context).apply {
                    text = "Нет продуктов"
                    setPadding(16, 8, 16, 8)
                    textSize = 14f
                    setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null))
                }
                productsContainer.addView(textView)
                return
            }

            products.forEach { product ->
                val productView = createProductView(product, mealType)
                productsContainer.addView(productView)
            }
        }

        private fun createProductView(product: MealProductDisplay, mealType: String): View {
            val linearLayout = LinearLayout(itemView.context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFF5F5F5.toInt())
                setPadding(16, 12, 16, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            }

            val nameTextView = TextView(itemView.context).apply {
                text = product.productName
                textSize = 16f
                setTextColor(itemView.context.resources.getColor(android.R.color.black, null))
            }

            val detailsTextView = TextView(itemView.context).apply {
                text = "${product.quantity} г • ${product.calories.toInt()} ккал"
                textSize = 14f
                setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null))
            }

            val macrosTextView = TextView(itemView.context).apply {
                text = "Б: ${product.protein.toInt()}г Ж: ${product.fat.toInt()}г У: ${product.carbs.toInt()}г"
                textSize = 12f
                setTextColor(itemView.context.resources.getColor(android.R.color.darker_gray, null))
            }

            val buttonsLayout = LinearLayout(itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
            }

            val editButton = Button(itemView.context).apply {
                text = "✏"
                setOnClickListener { onEditProduct(product, mealType) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(0x00000000)
                setTextColor(itemView.context.resources.getColor(android.R.color.holo_blue_dark, null))
            }

            val deleteButton = Button(itemView.context).apply {
                text = "✕"
                setOnClickListener { onDeleteProduct(product, mealType) }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    leftMargin = 16
                }
                setBackgroundColor(0x00000000)
                setTextColor(itemView.context.resources.getColor(android.R.color.holo_red_dark, null))
            }

            buttonsLayout.addView(editButton)
            buttonsLayout.addView(deleteButton)

            linearLayout.addView(nameTextView)
            linearLayout.addView(detailsTextView)
            linearLayout.addView(macrosTextView)
            linearLayout.addView(buttonsLayout)

            return linearLayout
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealVH(view)
    }

    override fun getItemCount() = meals.size

    override fun onBindViewHolder(holder: MealVH, position: Int) {
        holder.bind(meals[position])
    }

    fun updateData(newMeals: List<DisplayMeal>) {
        meals.clear()
        meals.addAll(newMeals)
        notifyDataSetChanged()
    }

    fun updateMealProducts(mealType: String, products: List<MealProductDisplay>) {
        if (expandedMeals.contains(mealType)) {
            expandedMeals.remove(mealType)
        } else {
            expandedMeals.add(mealType)
            mealProductsMap[mealType] = products
        }

        val position = meals.indexOfFirst { it.mealType == mealType }
        if (position != -1) {
            val meal = meals[position]
            meals[position] = meal.copy(isExpanded = expandedMeals.contains(mealType))
            notifyItemChanged(position)
        }
    }

    fun isMealExpanded(mealType: String): Boolean {
        return expandedMeals.contains(mealType)
    }
}