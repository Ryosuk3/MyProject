package com.example.mysamsungproject.photoWidget

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import android.widget.RemoteViews
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.mysamsungproject.photoWidget.utils.CornersDialog
import com.example.mysamsungproject.photoWidget.utils.CropImageActivity
import com.example.mysamsungproject.photoWidget.utils.CropImageActivity.Companion.CROP_IMAGE_INTENT_KEY
import com.example.mysamsungproject.photoWidget.utils.CropImageActivity.Companion.CROP_IMAGE_URI_KEY
import com.example.mysamsungproject.R
import com.example.mysamsungproject.databinding.PhotoWidgetActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoWidgetMainActivity : AppCompatActivity() {

    private lateinit var binding: PhotoWidgetActivityMainBinding
    private lateinit var viewModel: PhotoWidgetMainViewModel
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    private var chosenPhotoDate: String? = null
    private var isDateVisibleInWidget = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PhotoWidgetActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация ViewModel
        viewModel = ViewModelProvider(this).get(PhotoWidgetMainViewModel::class.java)

        // Наблюдение за изменениями данных в ViewModel и обновление UI
        viewModel.imageUri.observe(this) { uri ->
            binding.cropIv.setImageURI(uri)
            //sendWidgetUpdateBroadcast(this)
            // Убедитесь, что изображение корректно отображается
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val inputStream = contentResolver.openInputStream(uri)
                    val drawable = Drawable.createFromStream(inputStream, uri.toString())
                    binding.cropIv.setImageDrawable(drawable)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                saveImageUriInPrefs(uri)
                //viewModel.saveToPrefs()
                //updateWidgetWithImage(uri)
            }

        }


        viewModel.cornerRadius.observe(this) { radius ->
            setDrawableResRadius(binding.relLay, radius)
            //sendWidgetUpdateBroadcast(this)
            //viewModel.saveToPrefs()
            saveCornerRadiusInPrefs(radius)
            //updateWidgetWithCornersDebounced(radius)
        }

        viewModel.isDateVisible.observe(this) { isVisible ->
            binding.dateText.visibility = if (isVisible) VISIBLE else INVISIBLE
            //sendWidgetUpdateBroadcast(this)
            //viewModel.saveToPrefs()
            saveDateVisibilityInPrefs(isVisible)
            //updateWidgetWithDateTextDebounced(viewModel.dateText.value, isVisible)
        }

        viewModel.dateText.observe(this) { text ->
            binding.dateText.text = text
            saveDateTextInPrefs(text!!)
            //updateWidgetWithDateTextDebounced(text, viewModel.isDateVisible.value==true)
        }

        val rounds = mapOf(0 to R.drawable.round0, 10 to R.drawable.round10, 20 to R.drawable.round20,
            30 to R.drawable.round30, 40 to R.drawable.round40, 50 to R.drawable.round50, 60 to R.drawable.round60,
            70 to R.drawable.round70, 80 to R.drawable.round80, 90 to R.drawable.round90, 100 to R.drawable.round100,
            110 to R.drawable.round110, 120 to R.drawable.round120, 130 to R.drawable.round130,
            140 to R.drawable.round140, 150 to R.drawable.round150)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_corners)

        binding.btnSelectImage.setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                getPermission(Manifest.permission.READ_MEDIA_IMAGES)
            }else{
                getPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        binding.roundingCornersButton.setOnClickListener {
            val dialog = CornersDialog()
            val applyListener = object : CornersDialog.OnApplyListener {
                override fun onApply(value: Int) {
                    var radius = value * 10
                    radius=rounds[radius]!!
                    viewModel.cornerRadius.value = radius
                    updateWidgetWithCorners(radius)
                }
            }
            dialog.setOnApplyListener(applyListener)
            dialog.show(supportFragmentManager, "customDialogTag")
        }

        binding.photoDateButton.setOnClickListener {
            viewModel.isDateVisible.value = !(viewModel.isDateVisible.value ?: false)
            updateWidgetWithDateText(viewModel.dateText.value, viewModel.isDateVisible.value!!)
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    // Grant permission to read the URI
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    cropImage(uri)
                    viewModel.imageUri.value = uri
                    viewModel.dateText.value = getFormattedPhotoDate(uri)
                    viewModel.isDateVisible.value = false
                    updateWidgetWithImage(uri)
                }
            }
        }
    }

    override fun onPause() {
        updateWidgetWithImage(viewModel.imageUri.value!!)
        updateWidgetWithCorners(viewModel.cornerRadius.value!!)
        updateWidgetWithDateText(viewModel.dateText.value, viewModel.isDateVisible.value!!)
        super.onPause()
        viewModel.saveToPrefs() // Сохранение данных при паузе Activity
    }

    override fun onDestroy() {
        updateWidgetWithImage(viewModel.imageUri.value!!)
        updateWidgetWithCorners(viewModel.cornerRadius.value!!)
        updateWidgetWithDateText(viewModel.dateText.value, viewModel.isDateVisible.value!!)
        super.onDestroy()
        viewModel.saveToPrefs()
    }

    private fun getPermission(permissions: String) {
        Dexter.withContext(applicationContext)
            .withPermission(permissions)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Intent(MediaStore.ACTION_PICK_IMAGES)
                    } else {
                        Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    pickImageLauncher.launch(intent)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(binding.root, "Permission Denied!", Snackbar.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                    p1?.continuePermissionRequest()
                }
            }).check()
    }


    private fun cropImage(uri: Uri){
        val intent = Intent(
            this@PhotoWidgetMainActivity,
            CropImageActivity::class.java
        )
        intent.putExtra(CROP_IMAGE_INTENT_KEY,uri.toString())
        startActivityForResult(intent,12)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 12 && resultCode == RESULT_OK){
            if (data != null){
                val imageUriString = data.getStringExtra(CROP_IMAGE_URI_KEY)
                val imageUri = Uri.parse(imageUriString)
                //val imageBytes = data.getByteArrayExtra(CROP_IMAGE_B_ARRAY_KEY)
                //val arrayInputStream = ByteArrayInputStream(imageBytes)
                //val bitmap = BitmapFactory.decodeStream(arrayInputStream)
                //binding.cropIv.setImageBitmap(bitmap)
                binding.cropIv.setImageURI(imageUri)
                viewModel.imageUri.value=imageUri
                updateWidgetWithImage(imageUri)
            }
        }
    }
    private fun getFormattedPhotoDate(uri: Uri): String? {
        val date = getPhotoDate(uri) ?: return null
        val originalFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
        val targetFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return try {
            val dateParsed = originalFormat.parse(date)
            targetFormat.format(dateParsed!!)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getPhotoDate(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.use {
                val exif = ExifInterface(it)
                exif.getAttribute(ExifInterface.TAG_DATETIME)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateWidgetWithImage(uri: Uri) {
        val widgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(this, NewAppWidget::class.java))

        for (widgetId in widgetIds) {
            val remoteViews = RemoteViews(packageName, R.layout.new_app_widget)

            // Открытие потока изображения
            val inputStream = contentResolver.openInputStream(uri)
            // Декодирование изображения в Bitmap с меньшим разрешением
            val options = BitmapFactory.Options()
            options.inSampleSize = 4 // Уменьшаем разрешение в 4 раза
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)

            // Установка Bitmap в ImageView
            remoteViews.setImageViewBitmap(R.id.widget_image_view, bitmap)

            widgetManager.updateAppWidget(widgetId, remoteViews)
        }
        saveImageUriInPrefs(uri)
    }

    private fun updateWidgetWithCorners(draw: Int){
        val widgetManager=AppWidgetManager.getInstance(this)
        val widgetIds=widgetManager.getAppWidgetIds(ComponentName(this, NewAppWidget::class.java))

        for (widgetId in widgetIds){
            val remoteViews=RemoteViews(packageName, R.layout.new_app_widget)

            remoteViews.setInt(R.id.widget_frame, "setBackgroundResource", draw)

            widgetManager.updateAppWidget(widgetId,remoteViews)
        }
        saveCornerRadiusInPrefs(draw)
    }

    private fun updateWidgetWithDateText(dateText: String?, isVisible: Boolean) {
        val widgetManager = AppWidgetManager.getInstance(this)
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(this, NewAppWidget::class.java))

        for (widgetId in widgetIds) {
            val remoteViews = RemoteViews(packageName, R.layout.new_app_widget)
            remoteViews.setTextViewText(R.id.widgetDateText, dateText ?: "")
            remoteViews.setViewVisibility(R.id.widgetDateText, if (isVisible) VISIBLE else INVISIBLE)
            widgetManager.updateAppWidget(widgetId, remoteViews)
        }

        saveDateVisibilityInPrefs(isVisible)
        saveDateTextInPrefs(dateText ?: "")
    }



    private fun saveCornerRadiusInPrefs(cornerRadius: Int) {
        val sharedPreferences = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("corner_draw_key", cornerRadius)
        editor.apply()
    }

    private fun saveImageUriInPrefs(uri: Uri) {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("image_uri", uri.toString())
        editor.apply()
    }

    private fun saveDateVisibilityInPrefs(isVisible: Boolean) {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("date_visibility", isVisible)
        editor.apply()
    }

    private fun saveDateTextInPrefs(dateText: String) {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("date_text", dateText)
        editor.apply()
    }

    private  fun setDrawableResRadius(relLay: RelativeLayout,draw: Int){
        relLay.setBackgroundResource(0)
        relLay.setBackgroundResource(draw)
    }

    private fun updateWidgetWithImageDebounced(uri: Uri){
        CoroutineScope(Dispatchers.IO).launch {
            delay(500)
            updateWidgetWithImage(uri)
        }
    }

    private fun updateWidgetWithCornersDebounced(cornerRadius: Int){
        CoroutineScope(Dispatchers.IO).launch {
            delay(500)
            updateWidgetWithCorners(cornerRadius)
        }
    }

    private fun updateWidgetWithDateTextDebounced(dateText: String?, isVisible: Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            delay(500)
            updateWidgetWithDateText(dateText,isVisible)
        }
    }
}