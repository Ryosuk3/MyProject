package com.example.CustomWidgets.photoWidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import com.example.CustomWidgets.R

/**
 * Implementation of App Widget functionality.
 */
class NewAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }



    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    val imageUriString = prefs.getString("image_uri", null)
    val imageUri = imageUriString?.let { Uri.parse(it) }
    val cornerDraw = prefs.getInt("corner_draw_key", R.drawable.round0)
    val isDateVisible = prefs.getBoolean("date_visibility", false)
    val dateText = prefs.getString("date_text", "")

    val remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)


    imageUri?.let {
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
        remoteViews.setImageViewBitmap(R.id.widget_image_view, bitmap)
    }


    remoteViews.setInt(R.id.widget_frame, "setBackgroundResource", cornerDraw)


    remoteViews.setTextViewText(R.id.widgetDateText, dateText ?: "")
    remoteViews.setInt(R.id.widgetDateText,"setVisibility" ,if (isDateVisible) VISIBLE else INVISIBLE)

    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
}