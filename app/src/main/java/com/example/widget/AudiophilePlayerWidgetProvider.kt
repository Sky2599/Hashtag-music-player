package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class AudiophilePlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdateHelper.refreshWidgetsFromState(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}
