package com.example.veganism

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MealPlanNotificationManager { // schedule, cancel, reschedule, create notification
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "WEEK_PLAN_CHANNEL",
                "Week Plan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for planned meals"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleMealNotification(context: Context, recipeId: String, mealType: String, date: String, time: String) {
        createNotificationChannel(context)

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val scheduledDate = formatter.parse("$date $time") ?: return

        val calendar = Calendar.getInstance()
        calendar.time = scheduledDate

        // Checks if the time is in the past (for example if it's 15:00 and I set breakfast alarms for 9:00 and I try to schedule a new breakfast for today)
        if (calendar.before(Calendar.getInstance())) {
            return
        }

        val intent = Intent(context, MealPlanNotificationReceiver::class.java).apply {
            putExtra("recipeId", recipeId)
            putExtra("mealType", mealType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (date + mealType).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    fun cancelMealNotification(context: Context, date: String, mealType: String) {
        val intent = Intent(context, MealPlanNotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (date + mealType).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleWeekPlanNotifications(context: Context, uid: String) {
        var calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Canceling all the alarms planned for the next 7 days
        for (i in 0 until 7) {
            val dateStr = formatter.format(calendar.time)
            for(mealType in listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)) {
                cancelMealNotification(context, dateStr, mealType.toString())
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        calendar = Calendar.getInstance()

        val isWeekPlanNotificationsEnabled = SettingsManager.isWeekPlanNotificationsEnabled(context, uid)
        if (!isWeekPlanNotificationsEnabled) {
            return
        }

        val isBreakfastNotificationsEnabled = SettingsManager.isBreakfastNotificationsEnabled(context, uid)
        val isLunchNotificationsEnabled = SettingsManager.isLunchNotificationsEnabled(context, uid)
        val isDinnerNotificationsEnabled = SettingsManager.isDinnerNotificationsEnabled(context, uid)
        if (!isBreakfastNotificationsEnabled && !isLunchNotificationsEnabled && !isDinnerNotificationsEnabled) {
            return
        }

        val breakfastNotificationTime = SettingsManager.getBreakfastNotificationTime(context, uid)
        val lunchNotificationTime = SettingsManager.getLunchNotificationTime(context, uid)
        val dinnerNotificationTime = SettingsManager.getDinnerNotificationTime(context, uid)

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .collection("mealPlans")
            .get()
            .addOnSuccessListener {
                for(i in 0 until 7)
                {
                    val dateStr = formatter.format(calendar.time)
                    val day = it.documents.find { item -> item.id == dateStr }
                    if (day == null) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        continue
                    }

                    val breakfastId = day.getString("breakfastId")
                    if(breakfastId != null && isBreakfastNotificationsEnabled) {
                        scheduleMealNotification(context, breakfastId, MealType.BREAKFAST.toString(), dateStr, breakfastNotificationTime)
                    }

                    val lunchId = day.getString("lunchId")
                    if(lunchId != null && isLunchNotificationsEnabled) {
                        scheduleMealNotification(context, lunchId, MealType.LUNCH.toString(), dateStr, lunchNotificationTime)
                    }

                    val dinnerId = day.getString("dinnerId")
                    if(dinnerId != null && isDinnerNotificationsEnabled) {
                        scheduleMealNotification(context, dinnerId, MealType.DINNER.toString(), dateStr, dinnerNotificationTime)
                    }

                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            .addOnFailureListener {
                Log.e("MealPlanNotificationManager", "Failed to load meal plans", it)
            }

    }
}
