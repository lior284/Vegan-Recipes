package com.example.veganism

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class SavedRecipesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_recipes)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val back = findViewById<TextView>(R.id.savedRecipes_backArrow_iv)
        back.setOnClickListener {
            supportFinishAfterTransition()
        }
        val tvMessage = findViewById<TextView>(R.id.savedRecipes_message_tv)

        val recycler = findViewById<RecyclerView>(R.id.savedRecipes_recipes_rv)
        recycler.layoutManager = LinearLayoutManager(this)
        val auth = FirebaseAuth.getInstance()
        val userUID = auth.currentUser!!.uid
        val db = Firebase.firestore
        val recipesList: MutableList<Recipe> = mutableListOf()

        val adapter = RecipeAdapter(
            recipesList,
            RecipeAdapterMode.SAVED_LIST,
            onItemClick = { clickedRecipe, recipeBackground, recipeImageView ->
                val intent = Intent(this, RecipeDetailsActivity::class.java)
                intent.putExtra("recipeId", clickedRecipe.id)

                // Creat a pair of the View and its Transition Name
                val pairImage = androidx.core.util.Pair.create<View, String>(recipeImageView, "recipe_image_transition")

                // Pass the shared image into the animation options
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(this, pairImage)

                startActivity(intent, options.toBundle())
            },
            onRecipeUnsaved = { _, position ->
                recipesList.removeAt(position)
                recycler.adapter?.notifyItemRemoved(position)
                if (recipesList.isEmpty()) {
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = "Your saved recipes will appear here!"
                }
            }
        )

        recycler.adapter = adapter

        db.collection("users")
            .document(userUID)
            .collection("savedRecipes")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = "Your saved recipes will appear here!"
                    return@addOnSuccessListener
                }
                for (item in result) {
                    val recipeId = item.id
                    db.collection("recipes").document(recipeId).get()
                        .addOnSuccessListener { recipeDoc ->
                            val recipe = recipeDoc.toObject(Recipe::class.java)
                            if (recipe != null) {
                                recipesList.add(recipe)
                                adapter.notifyItemInserted(recipesList.size - 1)
                                tvMessage.visibility = View.GONE
                            }
                        }
                }
            }
            .addOnFailureListener {
                tvMessage.text = "Failed to load recipes, please try again later."
            }
    }
}
