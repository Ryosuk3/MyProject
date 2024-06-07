package com.example.CustomWidgets.ui.main

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.CustomWidgets.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PhotoWidgetMainViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val imageUri: MutableLiveData<Uri?> = MutableLiveData(null)
    val cornerRadius: MutableLiveData<Int> = MutableLiveData(0)  // Сохранение фактического значения угла
    val isDateVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val dateText: MutableLiveData<String?> = MutableLiveData(null)

    init {
        val prefs = application.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        imageUri.value = prefs.getString("image_uri", null)?.let { Uri.parse(it) }
        cornerRadius.value = prefs.getInt("corner_draw_key", 0)  // Измените ключ на "corner_radius"
        isDateVisible.value = prefs.getBoolean("date_visibility", false)
        dateText.value = prefs.getString("date_text", null)
    }

    fun saveToPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("image_uri", imageUri.value?.toString())
        editor.putInt("corner_draw_key", cornerRadius.value ?: 0)  // Сохраняем целое значение угла
        editor.putBoolean("date_visibility", isDateVisible.value ?: false)
        editor.putString("date_text", dateText.value)
        editor.apply()
    }

    fun saveSettingsToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userSettingsRef = firestore.collection("userSettings").document(currentUser.uid)
            val settings = hashMapOf(
                "isVisible" to isDateVisible.value,
                "radius" to cornerRadius.value  // Сохранение фактического значения угла
            )
            userSettingsRef.set(settings)
                .addOnSuccessListener {
                    Log.d(TAG, "Settings successfully written to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing settings to Firestore", e)
                }
        }
    }

    fun loadSettingsFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userSettingsRef = firestore.collection("userSettings").document(currentUser.uid)
            userSettingsRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val isVisible = document.getBoolean("isVisible") ?: false
                        val radius = document.getLong("radius")?.toInt() ?: 0  // Загружайте фактическое значение угла
                        isDateVisible.value = isVisible
                        cornerRadius.value = radius
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }
    }
}