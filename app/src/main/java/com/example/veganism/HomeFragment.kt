package com.example.veganism

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private var currentMealType: MealType? = null // Null means ALL
    private var currentMaxRecipeMinutes: Int = 60 // The max minutes in the seek bar

    private val recipesList: MutableList<Recipe> = mutableListOf()
    private val filteredRecipes: MutableList<Recipe> = mutableListOf()
    private lateinit var adapter: RecipeAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val filterAll = view.findViewById<Button>(R.id.homeFragment_filterAll_btn)
        val filterBreakfast = view.findViewById<Button>(R.id.homeFragment_filterBreakfast_btn)
        val filterLunch = view.findViewById<Button>(R.id.homeFragment_filterLunch_btn)
        val filterDinner = view.findViewById<Button>(R.id.homeFragment_filterDinner_btn)
        val filterOther = view.findViewById<Button>(R.id.homeFragment_filterOther_btn)

        val minutesFilter = view.findViewById<SeekBar>(R.id.homeFragment_minutesFilter_sb)

        val recycler = view.findViewById<RecyclerView>(R.id.homeFragment_recipes_rv)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val db = Firebase.firestore

        adapter = RecipeAdapter(
            filteredRecipes,
            RecipeAdapterMode.HOME,
            onItemClick = { clickedRecipe, recipeBackground, recipeImageView ->
                val intent = Intent(requireContext(), RecipeDetailsActivity::class.java)
                intent.putExtra("recipeId", clickedRecipe.id)

                // Create pairs of the View and its Transition Name
                val pairImage = androidx.core.util.Pair.create<View, String>(
                    recipeImageView, "recipe_image_transition"
                )
                val pairBackground = androidx.core.util.Pair.create<View, String>(
                    recipeBackground, "recipe_background_transition"
                )

                // Pass the pairs into the animation options
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    pairImage,
                    pairBackground
                )

                startActivity(intent, options.toBundle())
            }
        )

        db.collection("recipes").get()
            .addOnSuccessListener { result ->
                for (item in result) {
                    val recipe = item.toObject(Recipe::class.java)
                    recipesList.add(recipe)
                }

                // Initially show all recipes
                filteredRecipes.clear()
                filteredRecipes.addAll(recipesList)
                adapter.notifyDataSetChanged()

                recycler.adapter = adapter
            }

        filterAll.setOnClickListener {
            currentMealType = null
            applyFilters()
        }

        filterBreakfast.setOnClickListener {
            currentMealType = MealType.BREAKFAST
            applyFilters()
        }

        filterLunch.setOnClickListener {
            currentMealType = MealType.LUNCH
            applyFilters()
        }

        filterDinner.setOnClickListener {
            currentMealType = MealType.DINNER
            applyFilters()
        }

        filterOther.setOnClickListener {
            currentMealType = MealType.OTHER
            applyFilters()
        }

        minutesFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress
                currentMaxRecipeMinutes = minutes
                applyFilters()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        return view
    }

    private fun applyFilters()
    {
        filteredRecipes.clear()
        filteredRecipes.addAll(
            recipesList.filter { recipe ->
                val mealTypeMatches = currentMealType == null || currentMealType!!.name == recipe.mealType
                val timeMatches = recipe.cookingTimeMinutes <= currentMaxRecipeMinutes
                mealTypeMatches && timeMatches
            }
        )
        adapter.notifyDataSetChanged()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}