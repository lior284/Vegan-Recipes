package com.example.veganism

data class DayMeals(
    val dateTitle: String,
    var breakfastId: String? = null,
    var lunchId: String? = null,
    var dinnerId: String? = null
)