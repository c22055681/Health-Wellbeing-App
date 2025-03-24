package com.example.healthandwellbeingapp.api

data class FoodSearchResponse(
    val foods: Foods
)

data class Foods(
    val food: List<Food>
)

data class Food(
    val food_name: String,
    val food_description: String
)
