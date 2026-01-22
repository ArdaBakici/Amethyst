package com.example.amethyst.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.amethyst.widget.TaskWidgetProvider

/**
 * Worker that observes changes to task files and refreshes the widget
 */
class TaskFileObserverWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        println("TaskFileObserverWorker: File change detected, refreshing widgets")
        
        try {
            // Get the URI that triggered this work
            val triggeredContentUris = inputData.getStringArray("triggered_content_uris")
            println("TaskFileObserverWorker: Triggered by URIs: ${triggeredContentUris?.joinToString()}")
            
            // Refresh all widgets
            TaskWidgetProvider.updateAllWidgets(applicationContext)
            
            // Send broadcast to notify the app to refresh
            val intent = android.content.Intent("com.example.amethyst.TASKS_CHANGED")
            applicationContext.sendBroadcast(intent)
            println("TaskFileObserverWorker: Sent TASKS_CHANGED broadcast")
            
            println("TaskFileObserverWorker: Widgets refreshed successfully")
            return Result.success()
        } catch (e: Exception) {
            println("TaskFileObserverWorker: Error refreshing widgets: ${e.message}")
            e.printStackTrace()
            return Result.failure()
        }
    }
}
