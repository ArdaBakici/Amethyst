package com.example.amethyst.worker

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.example.amethyst.data.Preferences
import java.util.concurrent.TimeUnit

/**
 * Manages file observation using WorkManager to detect changes to task files
 */
object FileObserverManager {
    private const val WORK_TAG = "task_file_observer"
    private const val WORK_NAME = "task_file_observer_work"
    
    /**
     * Sets up content URI observation for the tasks folder
     */
    fun setupFileObserver(context: Context) {
        try {
            if (!Preferences.isInitialized) {
                println("FileObserverManager: Preferences not initialized, cannot setup observer")
                return
            }
            
            val fullTasksPath = Preferences.instance.getFullTasksPath()
            println("FileObserverManager: Setting up file observer for path: $fullTasksPath")
            
            if (fullTasksPath.isBlank()) {
                println("FileObserverManager: Tasks path is blank, skipping observer setup")
                return
            }
            
            // Get content URI for the tasks directory
            // Note: This works with Storage Access Framework URIs
            val vaultPath = Preferences.instance.vaultRootPath.value
            if (vaultPath.startsWith("content://")) {
                val contentUri = Uri.parse(vaultPath)
                println("FileObserverManager: Observing content URI: $contentUri")
                
                // Create constraints to trigger on content URI changes
                val constraints = Constraints.Builder()
                    .addContentUriTrigger(contentUri, true) // true = trigger on descendants too
                    .setTriggerContentUpdateDelay(1, TimeUnit.SECONDS) // Debounce rapid changes
                    .build()
                
                val workRequest = OneTimeWorkRequestBuilder<TaskFileObserverWorker>()
                    .setConstraints(constraints)
                    .addTag(WORK_TAG)
                    .build()
                
                // Enqueue the work request with KEEP policy to avoid canceling existing observers
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
                
                println("FileObserverManager: File observer work enqueued successfully")
            } else {
                println("FileObserverManager: Path is not a content URI, using periodic refresh fallback")
                setupPeriodicRefresh(context)
            }
        } catch (e: Exception) {
            println("FileObserverManager: Error setting up file observer: ${e.message}")
            e.printStackTrace()
            // Fallback to periodic refresh
            setupPeriodicRefresh(context)
        }
    }
    
    /**
     * Sets up a periodic refresh as a fallback when content URI observation is not available
     */
    private fun setupPeriodicRefresh(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<TaskFileObserverWorker>(
            15, TimeUnit.MINUTES
        ).addTag(WORK_TAG).build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "${WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        println("FileObserverManager: Periodic refresh set up (every 15 minutes)")
    }
    
    /**
     * Cancels all file observation work
     */
    fun cancelFileObserver(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        println("FileObserverManager: File observer cancelled")
    }
}
