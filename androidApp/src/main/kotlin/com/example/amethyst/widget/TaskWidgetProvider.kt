package com.example.amethyst.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.amethyst.MainActivity
import com.example.amethyst.R

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget instance created
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            println("TaskWidgetProvider.updateAppWidget: Updating widget $appWidgetId")
            val views = RemoteViews(context.packageName, R.layout.widget_task_list)
            println("TaskWidgetProvider.updateAppWidget: Created RemoteViews for ${context.packageName}")

            // Set up the intent to launch MainActivity when widget is clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
            println("TaskWidgetProvider.updateAppWidget: Set title click listener")

            // Set up the intent for the task list
            val listIntent = Intent(context, TaskWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setRemoteAdapter(R.id.widget_task_list, listIntent)
            println("TaskWidgetProvider.updateAppWidget: Set remote adapter")

            // Set empty view
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)
            println("TaskWidgetProvider.updateAppWidget: Set empty view")

            // Set up click listener for list items
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, clickPendingIntent)
            println("TaskWidgetProvider.updateAppWidget: Set pending intent template")

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            println("TaskWidgetProvider.updateAppWidget: Called updateAppWidget")

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
            println("TaskWidgetProvider.updateAppWidget: Called notifyAppWidgetViewDataChanged")
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TaskWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
