package com.example.veganism

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MealPlannerAdapter(
    private val list: List<DayMeals>,
    private val onMealClick: (String, ImageView) -> Unit,
    private val onMealLongClick: (DayMeals, MealType) -> Unit
) : RecyclerView.Adapter<MealPlannerAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTitle = itemView.findViewById<TextView>(R.id.mealItem_dayTitle_tv)

        val breakfastSlot = itemView.findViewById<LinearLayout>(R.id.mealItem_breakfastSlot_ll)
        val tvBreakfastTitle = itemView.findViewById<TextView>(R.id.mealItem_breakfastTitle_tv)
        val ivBreakfast = itemView.findViewById<ImageView>(R.id.mealItem_breakfastImage_iv)

        val lunchSlot = itemView.findViewById<LinearLayout>(R.id.mealItem_lunchSlot_ll)
        val tvLunchTitle = itemView.findViewById<TextView>(R.id.mealItem_lunchTitle_tv)
        val ivLunch = itemView.findViewById<ImageView>(R.id.mealItem_lunchImage_iv)

        val dinnerSlot = itemView.findViewById<LinearLayout>(R.id.mealItem_dinnerSlot_ll)
        val tvDinnerTitle = itemView.findViewById<TextView>(R.id.mealItem_dinnerTitle_tv)
        val ivDinner = itemView.findViewById<ImageView>(R.id.mealItem_dinnerImage_iv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.meal_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dayMeals = list[position]

        holder.dayTitle.text = formatDate(dayMeals.dateTitle)

        bindMealSlot(
            dayMeals = dayMeals,
            slotView = holder.breakfastSlot,
            titleView = holder.tvBreakfastTitle,
            imageView = holder.ivBreakfast,
            recipeId = dayMeals.breakfastId,
            mealType = MealType.BREAKFAST
        )
        bindMealSlot(
            dayMeals = dayMeals,
            slotView = holder.lunchSlot,
            titleView = holder.tvLunchTitle,
            imageView = holder.ivLunch,
            recipeId = dayMeals.lunchId,
            mealType = MealType.LUNCH
        )
        bindMealSlot(
            dayMeals = dayMeals,
            slotView = holder.dinnerSlot,
            titleView = holder.tvDinnerTitle,
            imageView = holder.ivDinner,
            recipeId = dayMeals.dinnerId,
            mealType = MealType.DINNER
        )
    }

    private fun bindMealSlot(
        dayMeals: DayMeals,
        slotView: View,
        titleView: TextView,
        imageView: ImageView,
        recipeId: String?,
        mealType: MealType
    ) {
        if (recipeId.isNullOrBlank()) {
            slotView.setOnClickListener(null)
            slotView.setOnLongClickListener(null)
            slotView.isClickable = false
            titleView.text = "Empty"
            imageView.visibility = View.GONE
            imageView.setImageDrawable(null)
            return
        }

        slotView.isClickable = true
        slotView.setOnClickListener {
            onMealClick(recipeId, imageView)
        }
        slotView.setOnLongClickListener {
            // Long press is only for deleting the meal from the week plan
            onMealLongClick(dayMeals, mealType)
            true
        }
        titleView.text = "Loading..."
        imageView.visibility = View.GONE
        imageView.setImageDrawable(null)

        db.collection("recipes")
            .document(recipeId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    titleView.text = "Recipe missing"
                    imageView.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val recipeName = document.getString("name")
                val recipeImage = document.getString("recipeImage")

                titleView.text = recipeName

                if (recipeImage.isNullOrBlank()) {
                    imageView.visibility = View.GONE
                    return@addOnSuccessListener
                }

                imageView.visibility = View.VISIBLE
                storage.getReference("recipes_images/$recipeImage").downloadUrl
                    .addOnSuccessListener { uri ->
                        // Converting dp to pixels because RoundedCorners works with pixels
                        val radiusDp = 16
                        val radiusPx = (radiusDp * imageView.resources.displayMetrics.density).toInt()

                        Glide.with(imageView)
                            .load(uri)
                            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(radiusPx)))
                            .into(imageView)
                    }
                    .addOnFailureListener {
                        imageView.setImageResource(R.drawable.img_recipe_item_example)
                    }
            }
            .addOnFailureListener {
                titleView.text = "Failed to load"
                imageView.visibility = View.GONE
            }
    }

    private fun formatDate(dateValue: String): String {
        val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputDateFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        val parsedDate = inputDateFormatter.parse(dateValue) ?: return dateValue
        val formattedDate = outputDateFormatter.format(parsedDate)

        // Writing Today / Tomorrow instead of the full date makes the week plan easier to scan
        if (isSameDay(parsedDate, Calendar.getInstance())) {
            return "Today"
        }

        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        if (isSameDay(parsedDate, tomorrow)) {
            return "Tomorrow"
        }

        return formattedDate
    }

    private fun isSameDay(date: Date, calendar: Calendar): Boolean {
        val dateCalendar = Calendar.getInstance().apply {
            time = date
        }

        return dateCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
            dateCalendar.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }
}
