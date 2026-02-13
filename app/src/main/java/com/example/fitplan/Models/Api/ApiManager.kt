package com.example.fitplan.Models.Api

import android.util.Log
import com.example.fitplan.Models.Product
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

object ApiManager {

    private val foodApi = ApiClient.api

    // Кэши
    private val searchCache = ConcurrentHashMap<String, List<Product>>()
    private val productCache = ConcurrentHashMap<String, Product>()
    private val prefixCache = ConcurrentHashMap<String, List<Product>>() // Кэш по префиксам

    suspend fun searchProducts(query: String): List<Product> {
        val queryLower = query.trim().lowercase()
        if (queryLower.length < 2) return emptyList()

        // Проверяем полный кэш
        searchCache[queryLower]?.let {
            Log.d("ApiManager", "Полный кэш: '$query' (${it.size} продуктов)")
            return it
        }

        // Проверяем кэш по префиксу (для частичных совпадений)
        for (i in queryLower.length downTo 2) {
            val prefix = queryLower.substring(0, i)
            prefixCache[prefix]?.let { cached ->
                val filtered = cached.filter { it.name.lowercase().contains(queryLower) }
                if (filtered.isNotEmpty()) {
                    Log.d("ApiManager", "Префиксный кэш: '$prefix' -> '$query' (${filtered.size} продуктов)")
                    return filtered
                }
            }
        }

        try {
            Log.d("ApiManager", "Запрос к API: '$query'")

            val response = withTimeoutOrNull(2000) {
                foodApi.searchProducts(
                    query = query,
                    simple = 1,
                    action = "process",
                    json = 1,
                    pageSize = 15
                )
            }

            if (response != null) {
                val products = convertProducts(response.products)

                if (products.isNotEmpty()) {
                    // Сохраняем в кэши
                    searchCache[queryLower] = products

                    // Сохраняем по префиксам для быстрого поиска
                    for (i in 2..queryLower.length) {
                        val prefix = queryLower.substring(0, i)
                        if (!prefixCache.containsKey(prefix)) {
                            val prefixProducts = products.filter {
                                it.name.lowercase().contains(prefix)
                            }
                            if (prefixProducts.isNotEmpty()) {
                                prefixCache[prefix] = prefixProducts
                            }
                        }
                    }

                    // Сохраняем продукты по именам
                    products.forEach { product ->
                        val nameKey = product.name.lowercase()
                        if (!productCache.containsKey(nameKey)) {
                            productCache[nameKey] = product
                        }
                    }

                    // Очищаем старые записи
                    if (searchCache.size > 50) {
                        val oldest = searchCache.keys.firstOrNull()
                        oldest?.let { searchCache.remove(it) }
                    }

                    if (prefixCache.size > 100) {
                        val oldest = prefixCache.keys.firstOrNull()
                        oldest?.let { prefixCache.remove(it) }
                    }

                    Log.d("ApiManager", "Найдено: ${products.size} продуктов, сохранено в кэш")
                }

                return products
            }

            Log.d("ApiManager", "Таймаут API запроса")
            return emptyList()

        } catch (e: HttpException) {
            Log.e("ApiManager", "HTTP ошибка ${e.code()}: ${e.message()}")
            return emptyList()
        } catch (e: Exception) {
            Log.e("ApiManager", "Ошибка: ${e.message}")
            return emptyList()
        }
    }

    private fun convertProducts(apiProducts: List<ApiProduct>): List<Product> {
        return apiProducts
            .take(10) // Ограничиваем для скорости
            .mapNotNull { apiProduct ->
                try {
                    val name = apiProduct.productName?.trim() ?: return@mapNotNull null
                    if (name.isBlank()) return@mapNotNull null

                    val nutriments = apiProduct.nutriments ?: return@mapNotNull null

                    // Упрощаем название
                    val simpleName = name
                        .replace("\\s+".toRegex(), " ")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .trim()
                        .take(50)

                    Product(
                        id = 0,
                        name = simpleName,
                        calories = nutriments.energyKcal100g ?: 0f,
                        protein = nutriments.proteins ?: 0f,
                        fat = nutriments.fat ?: 0f,
                        carbs = nutriments.carbohydrates ?: 0f,
                        brand = apiProduct.brands ?: "",
                        barcode = apiProduct.code ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .distinctBy { it.name }
    }

    fun getCachedProductByName(name: String): Product? {
        return productCache[name.lowercase()]
    }

    fun getCachedResults(query: String): List<Product> {
        val queryLower = query.lowercase()

        // 1. Проверяем полный кэш
        searchCache[queryLower]?.let { return it }

        // 2. Собираем результаты из всех кэшей
        val results = mutableSetOf<Product>()

        // Ищем по префиксам
        prefixCache.forEach { (prefix, products) ->
            if (queryLower.contains(prefix) || prefix.contains(queryLower)) {
                val filtered = products.filter {
                    it.name.lowercase().contains(queryLower)
                }
                results.addAll(filtered)
            }
        }

        // Ищем по точным именам
        productCache.forEach { (name, product) ->
            if (name.contains(queryLower)) {
                results.add(product)
            }
        }

        return results.toList().take(15)
    }

    fun clearCache() {
        searchCache.clear()
        productCache.clear()
        prefixCache.clear()
        Log.d("ApiManager", "Все кэши очищены")
    }
}