package com.example.veganism

import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WeekPlanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WeekPlanFragment : Fragment() {
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

    private lateinit var adapter: MealPlannerAdapter
    private val daysList = mutableListOf<DayMeals>()

    // Reload the week plan only if something changed in the recipe details page
    private val recipeDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            loadWeekPlan()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_week_plan, container, false)

        val recycler = view.findViewById<RecyclerView>(R.id.mealPlanner_days_rv)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // 7 days starting today
        if (daysList.isEmpty()) {
            val calendar = Calendar.getInstance()
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (i in 0 until 7) {
                val dateStr = formatter.format(calendar.time)
                daysList.add(DayMeals(dateStr, null, null, null))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        adapter = MealPlannerAdapter(
            daysList,
            onMealClick = { recipeId, recipeImageView ->
                openRecipeDetails(recipeId, recipeImageView)
            },
            onMealLongClick = { dayMeals, mealType ->
                confirmMealRemoval(dayMeals, mealType)
            }
        )

        recycler.adapter = adapter
        loadWeekPlan()

        return view
    }

    private fun loadWeekPlan() {
        val user = Firebase.auth.currentUser ?: return

        // Resetting the local list before fetching again so deleted meals won't stay on the screen
        for (day in daysList) {
            day.breakfastId = null
            day.lunchId = null
            day.dinnerId = null
        }

        Firebase.firestore.collection("users")
            .document(user.uid)
            .collection("mealPlans")
            .get()
            .addOnSuccessListener { result ->
                for (item in result) {
                    val dateKey = item.id
                    val day = daysList.find { it.dateTitle == dateKey } ?: continue

                    day.breakfastId = item.getString("breakfastId")
                    day.lunchId = item.getString("lunchId")
                    day.dinnerId = item.getString("dinnerId")
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load week plan.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openRecipeDetails(recipeId: String, recipeImageView: ImageView) {
        val intent = Intent(requireContext(), RecipeDetailsActivity::class.java)
        intent.putExtra("recipeId", recipeId)

        val pairImage = Pair.create<View, String>(
            recipeImageView, "recipe_image_transition"
        )

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            pairImage
        )

        recipeDetailsLauncher.launch(intent, options)
    }

    private fun confirmMealRemoval(
        dayMeals: DayMeals,
        mealType: MealType
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove planned meal?")
            .setMessage("Remove ${getMealLabel(mealType).lowercase()} from ${formatDayForMessage(dayMeals.dateTitle)}?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                removeMealFromWeekPlan(dayMeals, mealType)
            }
            .show()
    }

    private fun removeMealFromWeekPlan(
        dayMeals: DayMeals,
        mealType: MealType
    ) {
        val user = Firebase.auth.currentUser ?: return
        val fieldName = getMealFieldName(mealType)
        val mealPlanDocument = Firebase.firestore.collection("users")
            .document(user.uid)
            .collection("mealPlans")
            .document(dayMeals.dateTitle)

        mealPlanDocument.update(fieldName, FieldValue.delete())
            .addOnSuccessListener {
                when (mealType) {
                    MealType.BREAKFAST -> dayMeals.breakfastId = null
                    MealType.LUNCH -> dayMeals.lunchId = null
                    MealType.DINNER -> dayMeals.dinnerId = null
                    MealType.OTHER -> Unit
                }

                // If the day has no meals left, removing the whole document to keep Firestore clean
                if (dayMeals.breakfastId == null && dayMeals.lunchId == null && dayMeals.dinnerId == null) {
                    mealPlanDocument.delete()
                        .addOnSuccessListener {
                            adapter.notifyDataSetChanged()
                            Toast.makeText(requireContext(), "Meal removed from week plan.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Meal removed, but failed to clean up the empty day.", Toast.LENGTH_SHORT).show()
                            adapter.notifyDataSetChanged()
                        }
                } else {
                    adapter.notifyDataSetChanged()
                    Toast.makeText(requireContext(), "Meal removed from week plan.", Toast.LENGTH_SHORT).show()
                }

                MealPlanNotificationManager.cancelMealNotification(requireContext(), dayMeals.dateTitle, mealType.toString())
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to remove meal.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getMealFieldName(mealType: MealType): String {
        return when (mealType) {
            MealType.BREAKFAST -> "breakfastId"
            MealType.LUNCH -> "lunchId"
            MealType.DINNER -> "dinnerId"
            MealType.OTHER -> ""
        }
    }

    private fun getMealLabel(mealType: MealType): String {
        return when (mealType) {
            MealType.BREAKFAST -> "Breakfast"
            MealType.LUNCH -> "Lunch"
            MealType.DINNER -> "Dinner"
            MealType.OTHER -> "Meal"
        }
    }

    private fun formatDayForMessage(dateValue: String): String {
        val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val parsedDate = inputFormatter.parse(dateValue) ?: return dateValue
        return outputFormatter.format(parsedDate)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WeekPlanFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WeekPlanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
