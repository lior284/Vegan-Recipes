package com.example.vegan_recipes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class TimerFinishedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recipeId = intent.getStringExtra("recipeId") ?: return
        val timerMinutes = intent.getIntExtra("timerMinutes", 0)

        clearSavedTimerState(context, recipeId)
        createNotificationChannel(context)

        val openRecipeIntent = Intent(context, RecipeDetailsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("recipeId", recipeId)
            putExtra("fromNotification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            recipeId.hashCode(),
            openRecipeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "TIMER_CHANNEL")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Timer Finished!")
            .setContentText("Your $timerMinutes minutes timer has finished!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(recipeId.hashCode(), notification)
    }

    private fun clearSavedTimerState(context: Context, recipeId: String) {
        val prefs = context.getSharedPreferences(RecipeDetailsActivity.TIMER_PREFS_NAME, Context.MODE_PRIVATE)
        val savedRecipeId = prefs.getString(RecipeDetailsActivity.TIMER_RECIPE_ID_KEY, null)

        if (savedRecipeId == recipeId) {
            prefs.edit()
                .remove(RecipeDetailsActivity.TIMER_RECIPE_ID_KEY)
                .remove(RecipeDetailsActivity.TIMER_END_TIME_KEY)
                .apply()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TIMER_CHANNEL",
                "Timer Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for recipe timer"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
