package com.example.veganism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RecipeAdapter(
    private val list: MutableList<Recipe>,
    private val mode: RecipeAdapterMode,
    private val onItemClick: (Recipe, View, ImageView) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.recipeItem_recipeName_tv)
        val desc = itemView.findViewById<TextView>(R.id.recipeItem_recipeDescription_tv)
        val chef = itemView.findViewById<TextView>(R.id.recipeItem_chefName_tv)
        val image = itemView.findViewById<ImageView>(R.id.recipeItem_recipeImage_iv)
        val bookmark = itemView.findViewById<ImageView>(R.id.recipeItem_bookmarkIcon_v)
        val savesCount = itemView.findViewById<TextView>(R.id.recipeItem_savesCount_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recipe_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = list[position]

        holder.name.text = recipe.name
        holder.desc.text = recipe.description
        holder.chef.text = "Chef: ${recipe.chefUsername}"

        storage.getReference("recipes_images/${recipe.recipeImage}").downloadUrl
            .addOnSuccessListener { uri ->
                if (!holder.itemView.isAttachedToWindow) return@addOnSuccessListener

                val radiusDp = 16
                val radiusPx = (radiusDp * holder.image.resources.displayMetrics.density).toInt()

                Glide.with(holder.image)
                    .load(uri)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(radiusPx)))
                    .into(holder.image)
            }
            .addOnFailureListener {
                if (!holder.itemView.isAttachedToWindow) return@addOnFailureListener
                holder.image.setImageResource(R.drawable.img_recipe_item_example)
            }

        holder.itemView.setOnClickListener {
            onItemClick(recipe, holder.itemView, holder.image)
        }

        holder.savesCount.text = recipe.savesCount.toString()

        // If the user isn't signed in then finishing the function
        val user = auth.currentUser
        if (user == null) {
            updateBookmarkIcon(holder, false)
        } else {
            val recipeDocument = db.collection("users").document(user.uid)
                .collection("savedRecipes")
                .document(recipe.id)

            // If the user is signed in then checking if the recipe is saved
            recipeDocument.get()
                .addOnSuccessListener { document ->
                    recipe.isSaved = document.exists()
                    updateBookmarkIcon(holder, recipe.isSaved)
                }
        }

        holder.bookmark.setOnClickListener {
            if(auth.currentUser == null)
            {
                // If the user isn't signed in and clicks on the bookmark icon
                Toast.makeText(holder.itemView.context, "You need to sign in to save recipes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            recipe.isSaved = !recipe.isSaved
            val recipeDocument = db.collection("users").document(user!!.uid)
                .collection("savedRecipes")
                .document(recipe.id)
            if (recipe.isSaved) {
                // Save recipe to Firestore
                val recipeData = mapOf(
                    "name" to recipe.name,
                    "recipeImage" to recipe.recipeImage
                )
                recipeDocument.set(recipeData)
                    .addOnSuccessListener {
                        updateBookmarkIcon(holder, true)
                        recipe.savesCount++
                        holder.savesCount.text = recipe.savesCount.toString()
                        val db = FirebaseFirestore.getInstance()
                        db.collection("recipes").document(recipe.id)
                            .update("savesCount", recipe.savesCount)
                    }
                    .addOnFailureListener {
                        Toast.makeText(holder.itemView.context, "Failed to save.", Toast.LENGTH_SHORT).show()
                        recipe.isSaved = false
                        updateBookmarkIcon(holder, false)
                    }
            } else {
                // Remove recipe from Firestore
                recipeDocument.delete()
                    .addOnSuccessListener {
                        updateBookmarkIcon(holder, false)

                        recipe.savesCount--
                        val db = FirebaseFirestore.getInstance()
                        db.collection("recipes").document(recipe.id)
                            .update("savesCount", recipe.savesCount)

                        // If the list is in the saved recipes mode then remove the recipe from the list
                        if (mode == RecipeAdapterMode.SAVED_RECIPES) {
                            val position = holder.adapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                list.removeAt(position)
                                notifyItemRemoved(position)
                            }
                        } else {
                            holder.savesCount.text = recipe.savesCount.toString()
                            notifyItemChanged(holder.adapterPosition)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(holder.itemView.context, "Failed to remove", Toast.LENGTH_SHORT).show()
                        recipe.isSaved = true
                        updateBookmarkIcon(holder, true)
                    }
            }
        }
    }
    private fun updateBookmarkIcon(holder: ViewHolder, isSaved: Boolean) {
        if (isSaved) {
            holder.bookmark.setImageResource(R.drawable.ic_bookmark_saved)
        } else {
            holder.bookmark.setImageResource(R.drawable.ic_bookmark_unsaved)
        }
    }


    override fun getItemCount() = list.size
}
