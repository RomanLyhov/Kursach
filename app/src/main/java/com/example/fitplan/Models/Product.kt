package com.example.fitplan.Models

data class Product(
    val id: Long = 0,
    val name: String,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val carbs: Float = 0f
)
