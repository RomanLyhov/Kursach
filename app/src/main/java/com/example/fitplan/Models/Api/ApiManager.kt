package com.example.fitplan.Models.Api

import android.util.Log
import com.example.fitplan.Models.Product
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

object ApiManager {

    private val foodApi = ApiClient.api
    private val productCache = ConcurrentHashMap<String, Deferred<List<Product>>>()
    private val searchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun searchProducts(query: String): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedQuery = query.trim().lowercase()

                if (normalizedQuery.isBlank() || normalizedQuery.length < 2) {
                    return@withContext emptyList()
                }

                // Проверяем, есть ли уже активный поиск для этого запроса
                val existingDeferred = productCache[normalizedQuery]
                if (existingDeferred != null && existingDeferred.isActive) {
                    Log.d("ApiManager", "Возвращаем активный поиск для: $normalizedQuery")
                    return@withContext existingDeferred.await()
                }

                Log.d("ApiManager", "Начинаем API поиск: '$query'")

                // Создаем новый отложенный результат
                val deferred = searchScope.async {
                    try {
                        val response = foodApi.searchProducts(
                            query = query.trim(),
                            simple = 1,
                            action = "process",
                            json = 1,
                            pageSize = 30 // Больше результатов для лучшего поиска
                        )

                        val convertedProducts = convertApiProducts(response.products)

                        if (convertedProducts.isNotEmpty()) {
                            Log.d("ApiManager", "Найдено ${convertedProducts.size} продуктов для '$query'")
                            convertedProducts
                        } else {
                            Log.d("ApiManager", "Нет результатов для '$query', используем mock")
                            searchMockProducts(query)
                        }
                    } catch (e: HttpException) {
                        Log.e("ApiManager", "HTTP ошибка ${e.code()} для '$query': ${e.message()}")
                        searchMockProducts(query)
                    } catch (e: Exception) {
                        Log.e("ApiManager", "Ошибка API для '$query': ${e.message}", e)
                        searchMockProducts(query)
                    }
                }

                // Сохраняем deferred в кэш
                productCache[normalizedQuery] = deferred

                // Ограничиваем размер кэша
                if (productCache.size > 100) {
                    val oldestKey = productCache.keys.firstOrNull()
                    oldestKey?.let { productCache.remove(it) }
                }

                deferred.await()

            } catch (e: CancellationException) {
                Log.d("ApiManager", "Поиск отменен для: $query")
                emptyList()
            } catch (e: Exception) {
                Log.e("ApiManager", "Общая ошибка для '$query': ${e.message}", e)
                searchMockProducts(query)
            }
        }
    }

    private fun convertApiProducts(apiProducts: List<ApiProduct>): List<Product> {
        val validProducts = mutableListOf<Product>()

        if (apiProducts.isEmpty()) {
            return validProducts
        }

        apiProducts.forEach { apiProduct ->
            try {
                val name = apiProduct.productName?.trim() ?: return@forEach
                if (name.isBlank()) return@forEach

                // Улучшенная нормализация названий
                val normalizedName = normalizeProductName(name)

                val nutriments = apiProduct.nutriments ?: return@forEach

                val calories = nutriments.energyKcal100g ?: 0f
                val protein = nutriments.proteins ?: 0f
                val fat = nutriments.fat ?: 0f
                val carbs = nutriments.carbohydrates ?: 0f

                // Пропускаем только если все значения 0
                if (calories == 0f && protein == 0f && fat == 0f && carbs == 0f) {
                    return@forEach
                }

                // Проверяем, есть ли уже такой продукт
                val existingProduct = validProducts.find {
                    it.name.equals(normalizedName, ignoreCase = true)
                }

                if (existingProduct == null) {
                    validProducts.add(
                        Product(
                            id = 0,
                            name = normalizedName,
                            calories = calories,
                            protein = protein,
                            fat = fat,
                            carbs = carbs,
                            brand = apiProduct.brands?.takeIf { it.isNotBlank() } ?: "",
                            barcode = apiProduct.code ?: ""
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e("ApiManager", "Ошибка конвертации продукта", e)
            }
        }

        return validProducts
    }

    private fun normalizeProductName(name: String): String {
        val cleanName = name
            .replace("\\s+".toRegex(), " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .trim()

        val lowerName = cleanName.lowercase()

        // Определяем язык и нормализуем
        val hasRussian = cleanName.any { it in 'а'..'я' || it in 'А'..'Я' }

        return when {
            hasRussian -> {
                // Русские названия - делаем первую букву заглавной
                cleanName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            // Английские названия переводим на русский
            lowerName.contains("bread") -> "Хлеб"
            lowerName.contains("white bread") -> "Хлеб белый"
            lowerName.contains("whole wheat") || lowerName.contains("wholemeal") -> "Хлеб цельнозерновой"
            lowerName.contains("chicken") && lowerName.contains("breast") -> "Куриная грудка"
            lowerName.contains("chicken") -> "Курица"
            lowerName.contains("apple") -> "Яблоко"
            lowerName.contains("milk") -> "Молоко"
            lowerName.contains("cheese") -> "Сыр"
            lowerName.contains("egg") -> "Яйцо"
            lowerName.contains("banana") -> "Банан"
            lowerName.contains("rice") -> "Рис"
            lowerName.contains("potato") -> "Картофель"
            lowerName.contains("tomato") -> "Помидор"
            lowerName.contains("cucumber") -> "Огурец"
            lowerName.contains("beef") -> "Говядина"
            lowerName.contains("pork") -> "Свинина"
            lowerName.contains("fish") -> "Рыба"
            lowerName.contains("yogurt") || lowerName.contains("yoghurt") -> "Йогурт"
            lowerName.contains("butter") -> "Масло сливочное"
            lowerName.contains("oil") && lowerName.contains("sunflower") -> "Масло подсолнечное"
            lowerName.contains("oil") -> "Масло растительное"
            lowerName.contains("cottage cheese") -> "Творог"
            lowerName.contains("curd") -> "Творог"
            lowerName.contains("oat") -> "Овсянка"
            lowerName.contains("buckwheat") -> "Гречка"
            lowerName.contains("pasta") || lowerName.contains("macaroni") -> "Макароны"
            lowerName.contains("sugar") -> "Сахар"
            lowerName.contains("sour cream") -> "Сметана"
            lowerName.contains("sausage") -> "Колбаса"
            lowerName.contains("coffee") -> "Кофе"
            lowerName.contains("tea") -> "Чай"
            lowerName.contains("water") -> "Вода"
            lowerName.contains("juice") -> "Сок"
            lowerName.contains("beer") -> "Пиво"
            lowerName.contains("wine") -> "Вино"
            lowerName.contains("salt") -> "Соль"
            lowerName.contains("pepper") -> "Перец"
            else -> cleanName
        }
    }

    // Минимальный mock для резервного варианта
    private fun searchMockProducts(query: String): List<Product> {
        val searchQuery = query.lowercase()

        val mockProducts = listOf(
            Product(0, "Хлеб белый", 265f, 8f, 3.2f, 49f),
            Product(0, "Куриная грудка", 165f, 31f, 3.6f, 0f),
            Product(0, "Яблоко", 52f, 0.3f, 0.2f, 14f),
            Product(0, "Молоко 3.2%", 60f, 3.2f, 3.2f, 4.8f),
            Product(0, "Яйцо куриное", 155f, 13f, 11f, 1.1f),
            Product(0, "Рис отварной", 130f, 2.7f, 0.3f, 28f),
            Product(0, "Картофель отварной", 82f, 2f, 0.1f, 17f),
            Product(0, "Помидор", 18f, 0.9f, 0.2f, 3.9f),
            Product(0, "Огурец", 15f, 0.8f, 0.1f, 2.8f),
            Product(0, "Сыр Российский", 360f, 23f, 29f, 0f)
        )

        val results = mockProducts.filter {
            it.name.lowercase().contains(searchQuery) ||
                    (searchQuery.contains("bread") && it.name.contains("Хлеб")) ||
                    (searchQuery.contains("apple") && it.name.contains("Яблоко")) ||
                    (searchQuery.contains("chicken") && it.name.contains("Кури")) ||
                    (searchQuery.contains("milk") && it.name.contains("Молоко")) ||
                    (searchQuery.contains("egg") && it.name.contains("Яйцо")) ||
                    (searchQuery.contains("rice") && it.name.contains("Рис")) ||
                    (searchQuery.contains("potato") && it.name.contains("Картофель")) ||
                    (searchQuery.contains("tomato") && it.name.contains("Помидор")) ||
                    (searchQuery.contains("cucumber") && it.name.contains("Огурец")) ||
                    (searchQuery.contains("cheese") && it.name.contains("Сыр"))
        }

        return results.take(10)
    }

    fun clearCache() {
        productCache.clear()
        searchScope.coroutineContext.cancelChildren()
        Log.d("ApiManager", "Кэш очищен")
    }
}