package com.example.fitplan.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitplan.App
import com.example.fitplan.Models.Product
import com.example.fitplan.R
import kotlinx.coroutines.*

class AddMealFragment : Fragment() {

    // Переменные для хранения данных
    private lateinit var mealName: String
    private val db by lazy { App.instance.db }

    // Элементы интерфейса
    private lateinit var productSearch: AutoCompleteTextView
    private lateinit var quantityEdit: EditText
    private lateinit var caloriesTv: TextView
    private lateinit var proteinTv: TextView
    private lateinit var fatTv: TextView
    private lateinit var carbsTv: TextView
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button

    // Данные и состояния
    private var selectedProduct: Product? = null
    private lateinit var adapter: ArrayAdapter<String>
    private var searchJob: Job? = null
    private var addMealJob: Job? = null

    // Инициализация фрагмента с получением параметров
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mealName = requireArguments().getString("mealName") ?: ""
    }

    // Создание интерфейса фрагмента
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_meal, container, false)

        productSearch = view.findViewById(R.id.productSearchAutoComplete)
        quantityEdit = view.findViewById(R.id.quantityEditText)
        caloriesTv = view.findViewById(R.id.caloriesValue)
        proteinTv = view.findViewById(R.id.proteinValue)
        fatTv = view.findViewById(R.id.fatValue)
        carbsTv = view.findViewById(R.id.carbsValue)
        btnAdd = view.findViewById(R.id.btnAddMeal)
        btnCancel = view.findViewById(R.id.btnCancel)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf())
        productSearch.setAdapter(adapter)
        productSearch.threshold = 1

        setupListeners()

        return view
    }

    // Настройка всех обработчиков событий
    private fun setupListeners() {
        productSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = null

                val query = s.toString().trim()

                if (query.isEmpty()) {
                    adapter.clear()
                    adapter.notifyDataSetChanged()
                    selectedProduct = null
                    recalcNutrition()
                    return
                }

                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)

                    if (!isActive) return@launch

                    try {
                        val products = withContext(Dispatchers.IO) {
                            try {
                                db.getAllProductsMatching(query)
                            } catch (e: Exception) {
                                emptyList<Product>()
                            }
                        }

                        if (!isActive) return@launch
                        if (!isAdded) return@launch

                        withContext(Dispatchers.Main) {
                            adapter.clear()
                            if (products.isNotEmpty()) {
                                adapter.addAll(products.map { it.name })
                                adapter.notifyDataSetChanged()
                                if (!productSearch.isPopupShowing) {
                                    productSearch.showDropDown()
                                }
                            } else {
                                selectedProduct = null
                                recalcNutrition()
                            }
                        }
                    } catch (e: CancellationException) {
                        return@launch
                    } catch (e: Exception) {
                        if (isAdded) {
                            Log.d("AddMealFragment", "Search error", e)
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        productSearch.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position) ?: return@setOnItemClickListener

            searchJob?.cancel()
            searchJob = null

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val product = withContext(Dispatchers.IO) {
                        try {
                            db.getProductByName(name)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (!isAdded) return@launch

                    selectedProduct = product
                    recalcNutrition()
                } catch (e: CancellationException) {
                    return@launch
                }
            }
        }

        quantityEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = recalcNutrition()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Обработчик кнопки добавления продукта
        btnAdd.setOnClickListener {
            if (addMealJob?.isActive == true) return@setOnClickListener

            btnAdd.isEnabled = false
            val product = selectedProduct
            if (product == null) {
                Toast.makeText(requireContext(), "Выберите продукт", Toast.LENGTH_SHORT).show()
                btnAdd.isEnabled = true
                return@setOnClickListener
            }

            val quantity = quantityEdit.text.toString().toIntOrNull() ?: 100
            if (quantity <= 0) {
                Toast.makeText(requireContext(), "Введите корректное количество", Toast.LENGTH_SHORT).show()
                btnAdd.isEnabled = true
                return@setOnClickListener
            }

            addMealJob = viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            val prefs = requireContext().getSharedPreferences("session", Context.MODE_PRIVATE)
                            val userId = prefs.getLong("user_id", -1L)
                            if (userId == -1L) return@withContext false

                            val factor = quantity / 100f
                            val productId = db.insertOrGetProduct(product)

                            db.addMeal(
                                userId, productId, mealName, quantity,
                                product.calories * factor,
                                product.protein * factor,
                                product.fat * factor,
                                product.carbs * factor
                            )
                            true
                        } catch (e: Exception) {
                            Log.e("AddMealFragment", "Database error", e)
                            false
                        }
                    }

                    if (isAdded) {
                        if (success) {
                            parentFragmentManager.setFragmentResult("meal_added", Bundle.EMPTY)
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(requireContext(), "Ошибка при добавлении продукта", Toast.LENGTH_SHORT).show()
                            btnAdd.isEnabled = true
                        }
                    }
                } catch (e: CancellationException) {
                    if (isAdded) {
                        btnAdd.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e("AddMealFragment", "Add meal error", e)
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnAdd.isEnabled = true
                    }
                }
            }
        }

        // Обработчик кнопки отмены
        btnCancel.setOnClickListener {
            searchJob?.cancel()
            addMealJob?.cancel()

            if (isAdded) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    // Пересчет пищевой ценности на основе выбранного продукта и количества
    private fun recalcNutrition() {
        val product = selectedProduct
        val quantity = quantityEdit.text.toString().toIntOrNull() ?: 100
        val factor = if (quantity > 0) quantity / 100f else 0f

        caloriesTv.text = ((product?.calories ?: 0f) * factor).toInt().toString()
        proteinTv.text = "${((product?.protein ?: 0f) * factor).toInt()} г"
        fatTv.text = "${((product?.fat ?: 0f) * factor).toInt()} г"
        carbsTv.text = "${((product?.carbs ?: 0f) * factor).toInt()} г"
    }

    // Очистка ресурсов при уничтожении вида
    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        addMealJob?.cancel()
        searchJob = null
        addMealJob = null
    }

    companion object {
        // Создание нового экземпляра фрагмента с передачей параметров
        fun newInstance(mealName: String) =
            AddMealFragment().apply {
                arguments = Bundle().apply {
                    putString("mealName", mealName)
                }
            }
    }
}