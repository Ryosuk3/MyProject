package com.example.mysamsungproject.photoWidget

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.mysamsungproject.R

class PhotoWidgetMainViewModel(application: Application): AndroidViewModel(application) {
    val imageUri: MutableLiveData<Uri?> = MutableLiveData()
    val cornerRadius: MutableLiveData<Int> = MutableLiveData()
    val isDateVisible: MutableLiveData<Boolean> = MutableLiveData()
    val dateText: MutableLiveData<String?> = MutableLiveData()

    init {
        // Load saved data from SharedPreferences
        val prefs = application.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        imageUri.value = prefs.getString("image_uri", null)?.let { Uri.parse(it) }
        cornerRadius.value = prefs.getInt("corner_draw_key", R.drawable.round0)
        isDateVisible.value = prefs.getBoolean("date_visibility", false)
        dateText.value = prefs.getString("date_text", null)
    }

    fun saveToPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("image_uri", imageUri.value?.toString())
        editor.putInt("corner_draw_key", cornerRadius.value ?: R.drawable.round0)
        editor.putBoolean("date_visibility", isDateVisible.value ?: false)
        editor.putString("date_text", dateText.value)
        editor.apply()
    }
}