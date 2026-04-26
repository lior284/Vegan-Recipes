package com.example.veganism

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    private lateinit var filterAllBtn: Button
    private lateinit var filterBreakfastBtn: Button
    private lateinit var filterLunchBtn: Button
    private lateinit var filterDinnerBtn: Button
    private lateinit var filterOtherBtn: Button

    private lateinit var sbMinutesFilter: SeekBar

    private var currentSearchQuery: String = ""
    private var currentMealType: MealType? = null // Null means ALL
    private var currentMaxRecipeMinutes: Int = 60 // The max minutes in the seek bar

    private val recipesList: MutableList<Recipe> = mutableListOf()
    private val filteredRecipes: MutableList<Recipe> = mutableListOf()
    private lateinit var adapter: RecipeAdapter

    private lateinit var tvMessage: TextView
    private var isLoadingRecipes = true

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val searchBar = view.findViewById<EditText>(R.id.homeFragment_searchBar_et)

        filterAllBtn = view.findViewById(R.id.homeFragment_filterAll_btn)
        filterBreakfastBtn = view.findViewById(R.id.homeFragment_filterBreakfast_btn)
        filterLunchBtn = view.findViewById(R.id.homeFragment_filterLunch_btn)
        filterDinnerBtn = view.findViewById(R.id.homeFragment_filterDinner_btn)
        filterOtherBtn = view.findViewById(R.id.homeFragment_filterOther_btn)

        sbMinutesFilter = view.findViewById<SeekBar>(R.id.homeFragment_minutesFilter_sb)

        tvMessage = view.findViewById(R.id.homeFragment_message_tv)

        val recycler = view.findViewById<RecyclerView>(R.id.homeFragment_recipes_rv)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val db = Firebase.firestore

        adapter = RecipeAdapter(
            filteredRecipes,
            RecipeAdapterMode.HOME,
            onItemClick = { clickedRecipe, recipeBackground, recipeImageView ->
                val intent = Intent(requireContext(), RecipeDetailsActivity::class.java)
                intent.putExtra("recipeId", clickedRecipe.id)

                // Create a pair of the View and its Transition Name
                val pairImage = androidx.core.util.Pair.create<View, String>(recipeImageView, "recipe_image_transition")

                // Pass the shared image into the animation options
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), pairImage)

                startActivity(intent, options.toBundle())
            }
        )
        recycler.adapter = adapter

        db.collection("recipes").get()
            .addOnSuccessListener { result ->
                for (item in result) {
                    val recipe = item.toObject(Recipe::class.java)
                    recipesList.add(recipe)
                }
                isLoadingRecipes = false
                applyFilters() // Apply filters after recipes are loaded in case the user touches the filters before the recipes are done loading
            }
            .addOnFailureListener {
                isLoadingRecipes = false
                tvMessage.text = "Failed to load recipes, please try again later."
            }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        filterAllBtn.setOnClickListener {
            currentMealType = null
            applyFilters()
            updateFilterBtn(filterAllBtn)
        }

        filterBreakfastBtn.setOnClickListener {
            currentMealType = MealType.BREAKFAST
            applyFilters()
            updateFilterBtn(filterBreakfastBtn)
        }

        filterLunchBtn.setOnClickListener {
            currentMealType = MealType.LUNCH
            applyFilters()
            updateFilterBtn(filterLunchBtn)
        }

        filterDinnerBtn.setOnClickListener {
            currentMealType = MealType.DINNER
            applyFilters()
            updateFilterBtn(filterDinnerBtn)
        }

        filterOtherBtn.setOnClickListener {
            currentMealType = MealType.OTHER
            applyFilters()
            updateFilterBtn(filterOtherBtn)
        }

        sbMinutesFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentMaxRecipeMinutes = progress // Progress representing the number of minutes
                applyFilters()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        return view
    }

    private fun applyFilters()
    {
        // I don't want to accidentally show a different message if the recipes are still loading.
        // But for the case where the user touches the filters before the recipes are done loading, I call this function after the recipes are loaded.
        if (isLoadingRecipes) return

        filteredRecipes.clear()
        filteredRecipes.addAll(
            recipesList.filter { recipe ->
                val mealTypeMatches = currentMealType == null || currentMealType?.name == recipe.mealType
                val timeMatches = recipe.cookingTimeMinutes <= currentMaxRecipeMinutes || sbMinutesFilter.max == currentMaxRecipeMinutes // If the bar is at 60 meaning showing recipes with no time limit
                val searchMatches = currentSearchQuery.isBlank() ||
                            recipe.name.contains(currentSearchQuery, ignoreCase = true) ||
                            recipe.description.contains(currentSearchQuery, ignoreCase = true) ||
                            recipe.ingredients.contains(currentSearchQuery, ignoreCase = true)

                mealTypeMatches && timeMatches && searchMatches
            }
        )

        tvMessage.visibility = if (filteredRecipes.isEmpty()) View.VISIBLE else View.GONE
        tvMessage.text = "Sorry, but no recipes match your filters."

        adapter.notifyDataSetChanged()
    }

    private fun updateFilterBtn(clickedBtn: Button) {
        val lst = listOf(filterAllBtn, filterBreakfastBtn, filterLunchBtn, filterDinnerBtn, filterOtherBtn)
        for (button in lst)
            button.setBackgroundResource(R.drawable.bg_filter_btn)
        clickedBtn.setBackgroundResource(R.drawable.bg_filter_btn_checked)
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
