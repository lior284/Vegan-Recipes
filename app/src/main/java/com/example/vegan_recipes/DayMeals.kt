package com.example.vegan_recipes

data class DayMeals(
    val dateTitle: String,
    var breakfastId: String? = null,
    var lunchId: String? = null,
    var dinnerId: String? = null
)