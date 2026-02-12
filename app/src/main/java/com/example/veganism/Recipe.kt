package com.example.veganism

class Recipe (
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var chefUsername: String = "",
    var recipeImage: String = "",
    var ingredients: String = "",
    var instructions: String = "",
    var notes: String = "",
    var cookingTimeMinutes: Int = 0,
    var timerMinutes: Int = 0,
    var isSaved: Boolean = false,
    var savesCount: Int = 0
)