package com.example.vegan_recipes

import com.google.firebase.firestore.PropertyName

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
    var mealType: String = "OTHER",
    var timerMinutes: Int = 0,

    // I have this get and set because firebase saves 'isSomething' as 'something' and I want to save it as 'isSomething'
    @get:PropertyName("isVegan")
    @set:PropertyName("isVegan")
    var isSaved: Boolean = false,
    var savesCount: Int = 0
)