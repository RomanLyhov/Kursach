package com.example.fitplan.Models.Api

import android.util.Log
import com.example.fitplan.Models.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object ApiManager {

    private val foodApi = ApiClient.api

    suspend fun searchProducts(query: String): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                ensureActive()

                Log.d("ApiManager", "Searching for: $query")

                val response = foodApi.searchProducts(
                    query = query,
                    simple = 1,
                    action = "process",
                    json = 1,
                    pageSize = 20
                )

                ensureActive()

                Log.d("ApiManager", "Got response: ${response.products.size} products")

                convertApiProducts(response.products)

            } catch (e: CancellationException) {
                emptyList()
            } catch (e: Exception) {
                if (e.cause is CancellationException) {
                    emptyList()
                } else {
                    Log.e("ApiManager", "API Error", e)
                    searchMockProducts(query)
                }
            }
        }
    }

    private fun convertApiProducts(apiProducts: List<ApiProduct>): List<Product> {
        return apiProducts.mapNotNull { apiProduct ->
            try {
                val name = apiProduct.productName ?: return@mapNotNull null
                val nutriments = apiProduct.nutriments ?: return@mapNotNull null

                val calories = nutriments.energyKcal100g ?: 0f
                val protein = nutriments.proteins ?: 0f
                val fat = nutriments.fat ?: 0f
                val carbs = nutriments.carbohydrates ?: 0f

                if (calories == 0f && protein == 0f && fat == 0f && carbs == 0f) {
                    return@mapNotNull null
                }

                Product(
                    id = 0,
                    name = name.trim(),
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs
                )
            } catch (e: Exception) {
                Log.e("ApiManager", "Error converting product", e)
                null
            }
        }
    }

    private val mockProducts = listOf(
        Product(0, "Куриная грудка", 165f, 31f, 3.6f, 0f),
        Product(0, "Яблоко", 52f, 0.3f, 0.2f, 14f),
        Product(0, "Творог 5%", 121f, 17f, 5f, 1.8f),
        Product(0, "Рис отварной", 130f, 2.7f, 0.3f, 28f),
        Product(0, "Банан", 89f, 1.1f, 0.3f, 23f),
        Product(0, "Яйцо куриное", 155f, 13f, 11f, 1.1f),
        Product(0, "Овсянка", 389f, 16.9f, 6.9f, 66.3f),
        Product(0, "Гречка", 343f, 13.3f, 3.4f, 71.5f)
    )

    private fun searchMockProducts(query: String): List<Product> {
        return if (query.isBlank()) {
            mockProducts.take(5)
        } else {
            mockProducts.filter {
                it.name.contains(query, ignoreCase = true)
            }.take(5)
        }
    }
}