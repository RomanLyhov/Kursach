package com.example.fitplan.Models

import com.google.gson.annotations.SerializedName

data class Nutriments(

    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Float?,

    @SerializedName("proteins_100g")
    val proteins: Float?,

    @SerializedName("fat_100g")
    val fat: Float?,

    @SerializedName("carbohydrates_100g")
    val carbohydrates: Float?
)
