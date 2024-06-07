package com.example.CustomWidgets.photoWidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
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
    val cornerRadius = prefs.getInt("corner_draw_key", 0)
    val isDateVisible = prefs.getBoolean("date_visibility", false)
    val dateText = prefs.getString("date_text", "")

    val remoteViews = RemoteViews(context.packageName, R.layout.new_app_widget)

    imageUri?.let {
        try {
            val inputStream = context.contentResolver.openInputStream(it)
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            remoteViews.setImageViewBitmap(R.id.widget_image_view, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            remoteViews.setTextViewText(R.id.widget_image_view, "Ошибка при загрузке изображения")
        }
    }

    val drawableRes = when (cornerRadius) {
        0 -> R.drawable.round0
        10 -> R.drawable.round10
        20 -> R.drawable.round20
        30 -> R.drawable.round30
        40 -> R.drawable.round40
        50 -> R.drawable.round50
        60 -> R.drawable.round60
        70 -> R.drawable.round70
        80 -> R.drawable.round80
        90 -> R.drawable.round90
        100 -> R.drawable.round100
        110 -> R.drawable.round110
        120 -> R.drawable.round120
        130 -> R.drawable.round130
        140 -> R.drawable.round140
        150 -> R.drawable.round150
        else -> R.drawable.round0
    }

    remoteViews.setInt(R.id.widget_frame, "setBackgroundResource", drawableRes)

    remoteViews.setTextViewText(R.id.widgetDateText, dateText ?: "")
    remoteViews.setViewVisibility(R.id.widgetDateText, if (isDateVisible) View.VISIBLE else View.INVISIBLE)

    appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
}