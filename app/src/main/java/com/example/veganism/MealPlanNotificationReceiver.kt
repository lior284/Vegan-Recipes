package com.example.veganism

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class MealPlanNotificationReceiver : BroadcastReceiver() {
    // Show notification when the time arrives
    override fun onReceive(context: Context, intent: Intent) {
        val recipeId = intent.getStringExtra("recipeId") ?: return
        val mealType = intent.getStringExtra("mealType") ?: return

        val openRecipeIntent = Intent(context, RecipeDetailsActivity::class.java)
        openRecipeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        openRecipeIntent.putExtra("recipeId", recipeId)
        openRecipeIntent.putExtra("fromNotification", true)

        val pendingIntent = PendingIntent.getActivity(
            context,
            recipeId.hashCode(),
            openRecipeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mealTypeLabel = mealType.lowercase().replaceFirstChar { it.uppercase() }

        val notification = NotificationCompat.Builder(context, "WEEK_PLAN_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$mealTypeLabel reminder")
            .setContentText("You have a planned ${mealType.lowercase()} right now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(recipeId.hashCode() + mealType.hashCode(), notification)
    }

}
