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
import com.example.fitplan.Models.Api.ApiManager
import com.example.fitplan.Models.Product
import com.example.fitplan.R
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class AddMealFragment : Fragment() {

    private lateinit var mealName: String
    private val db by lazy { App.instance.db }

    private lateinit var productSearch: AutoCompleteTextView
    private lateinit var quantityEdit: EditText
    private lateinit var caloriesTv: TextView
    private lateinit var proteinTv: TextView
    private lateinit var fatTv: TextView
    private lateinit var carbsTv: TextView
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button

    private var selectedProduct: Product? = null
    private lateinit var adapter: ArrayAdapter<String>
    private var searchJob: Job? = null
    private var addMealJob: Job? = null
    private val searchResultsCache = mutableMapOf<String, List<Product>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mealName = requireArguments().getString("mealName") ?: ""
    }

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

    private fun setupListeners() {
        productSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Отменяем предыдущий поиск
                searchJob?.cancel()
                searchJob = null

                if (query.isEmpty()) {
                    adapter.clear()
                    adapter.notifyDataSetChanged()
                    selectedProduct = null
                    recalcNutrition()
                    return
                }

                // Запускаем мгновенный поиск без задержки
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    performInstantSearch(query)
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
                    // Ищем продукт в кэше
                    val cachedResults = searchResultsCache.values.flatten()
                    val product = cachedResults.find {
                        it.name.equals(name, ignoreCase = true)
                    }

                    if (product == null) {
                        // Если не нашли в кэше, ищем локально
                        val foundProduct = withContext(Dispatchers.IO) {
                            try {
                                db.getProductByName(name)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (isAdded && foundProduct != null) {
                            selectedProduct = foundProduct
                            recalcNutrition()
                        } else if (isAdded) {
                            Toast.makeText(
                                requireContext(),
                                "Не удалось загрузить данные продукта",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Сохраняем продукт в локальную БД
                        withContext(Dispatchers.IO) {
                            try {
                                db.insertOrGetProduct(product)
                            } catch (e: Exception) {
                                Log.e("AddMealFragment", "Error saving product", e)
                            }
                        }

                        if (isAdded) {
                            selectedProduct = product
                            recalcNutrition()
                        }
                    }
                } catch (e: CancellationException) {
                    return@launch
                } catch (e: Exception) {
                    if (isAdded) {
                        Log.e("AddMealFragment", "Error selecting product", e)
                    }
                }
            }
        }

        quantityEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = recalcNutrition()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

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

        btnCancel.setOnClickListener {
            searchJob?.cancel()
            addMealJob?.cancel()
            if (isAdded) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    // Мгновенный поиск через API без задержек
    private suspend fun performInstantSearch(query: String) = coroutineScope {
        if (query.length < 2) {
            // Для коротких запросов очищаем результаты
            withContext(Dispatchers.Main) {
                adapter.clear()
                adapter.notifyDataSetChanged()
            }
            return@coroutineScope
        }

        try {
            // Проверяем кэш
            val cached = searchResultsCache[query.lowercase()]
            if (cached != null) {
                Log.d("AddMealFragment", "Используем кэш для: $query")
                updateResults(cached, query)
                return@coroutineScope
            }

            // Проверяем интернет
            val hasInternet = isOnline()
            val products: List<Product>

            if (hasInternet) {
                Log.d("AddMealFragment", "API поиск: $query")

                // Используем async для параллельного поиска в API
                val apiJob = async {
                    try {
                        ApiManager.searchProducts(query)
                    } catch (e: Exception) {
                        Log.e("AddMealFragment", "API search error", e)
                        emptyList<Product>()
                    }
                }

                // Пока API ищет, показываем результаты из частичного кэша
                showPartialResults(query)

                // Ждем результаты API
                products = apiJob.await()

                // Сохраняем в кэш
                searchResultsCache[query.lowercase()] = products

            } else {
                Log.d("AddMealFragment", "Оффлайн поиск: $query")
                products = withContext(Dispatchers.IO) {
                    try {
                        // Ищем в локальной БД
                        val method = db::class.java.getMethod("getAllProductsMatching", String::class.java)
                        method.invoke(db, query) as List<Product>
                    } catch (e: Exception) {
                        emptyList<Product>()
                    }
                }
            }

            if (isAdded) {
                updateResults(products, query)
            }

        } catch (e: CancellationException) {
            Log.d("AddMealFragment", "Поиск отменен для: $query")
        } catch (e: Exception) {
            if (isAdded) {
                Log.e("AddMealFragment", "Ошибка поиска для: $query", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка поиска. Проверьте подключение к интернету",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Показываем результаты из частичного кэша (первые буквы)
    private fun showPartialResults(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val queryLower = query.lowercase()
                val partialResults = mutableListOf<Product>()

                // Ищем в кэше по частичному совпадению
                searchResultsCache.forEach { (cachedQuery, products) ->
                    if (cachedQuery.contains(queryLower) || queryLower.contains(cachedQuery)) {
                        // Фильтруем по текущему запросу
                        val filtered = products.filter {
                            it.name.lowercase().contains(queryLower)
                        }
                        partialResults.addAll(filtered)
                    }
                }

                if (partialResults.isNotEmpty() && isAdded) {
                    withContext(Dispatchers.Main) {
                        val uniqueResults = partialResults.distinctBy { it.name }.take(10)
                        adapter.clear()
                        adapter.addAll(uniqueResults.map { it.name })
                        adapter.notifyDataSetChanged()

                        if (!productSearch.isPopupShowing) {
                            productSearch.showDropDown()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("AddMealFragment", "Ошибка частичного поиска", e)
            }
        }
    }

    private fun updateResults(products: List<Product>, query: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (!isAdded) return@launch

            val queryLower = query.lowercase()
            val filtered = products.filter {
                it.name.lowercase().contains(queryLower)
            }.distinctBy { it.name }.take(20)

            adapter.clear()
            if (filtered.isNotEmpty()) {
                adapter.addAll(filtered.map { it.name })
                adapter.notifyDataSetChanged()

                if (!productSearch.isPopupShowing) {
                    productSearch.showDropDown()
                }
            } else if (query.length >= 3) {
                // Только для длинных запросов показываем сообщение
                Toast.makeText(
                    requireContext(),
                    "Ничего не найдено для '$query'",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun isOnline(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val timeoutMs = 2000
                val socket = Socket()
                val socketAddress = InetSocketAddress("8.8.8.8", 53)
                socket.connect(socketAddress, timeoutMs)
                socket.close()
                true
            } catch (e: IOException) {
                false
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun recalcNutrition() {
        val product = selectedProduct
        val quantity = quantityEdit.text.toString().toIntOrNull() ?: 100
        val factor = if (quantity > 0) quantity / 100f else 0f

        caloriesTv.text = ((product?.calories ?: 0f) * factor).toInt().toString()
        proteinTv.text = "${((product?.protein ?: 0f) * factor).toInt()} г"
        fatTv.text = "${((product?.fat ?: 0f) * factor).toInt()} г"
        carbsTv.text = "${((product?.carbs ?: 0f) * factor).toInt()} г"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        addMealJob?.cancel()
        searchJob = null
        addMealJob = null
        searchResultsCache.clear()
    }

    companion object {
        fun newInstance(mealName: String) =
            AddMealFragment().apply {
                arguments = Bundle().apply {
                    putString("mealName", mealName)
                }
            }
    }
}