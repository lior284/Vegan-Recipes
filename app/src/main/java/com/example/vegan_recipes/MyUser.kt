package com.example.vegan_recipes
import com.google.firebase.firestore.PropertyName

data class MyUser (
    var firstName: String = "",
    var lastName: String = "",
    var username: String = "",
    var birthYear: Int = 0,

    // I have this get and set because firebase saves 'isSomething' as 'something' and I want to save it as 'isSomething'
    @get:PropertyName("isVegan")
    @set:PropertyName("isVegan")
    var isVegan: Boolean = false,
    var profilePicture: String = ""
)