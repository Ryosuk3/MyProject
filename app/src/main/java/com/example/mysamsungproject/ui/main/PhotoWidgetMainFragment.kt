package com.example.mysamsungproject.ui.main

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mysamsungproject.R
import com.example.mysamsungproject.Settings
import com.example.mysamsungproject.databinding.PhotoWidgetMainFragmentBinding
import com.example.mysamsungproject.photoWidget.NewAppWidget
import com.example.mysamsungproject.photoWidget.utils.CornersDialog
import com.example.mysamsungproject.photoWidget.utils.CropImageActivity
import com.example.mysamsungproject.photoWidget.utils.CropImageActivity.Companion.CROP_IMAGE_INTENT_KEY
import com.example.mysamsungproject.photoWidget.utils.CropImageActivity.Companion.CROP_IMAGE_URI_KEY
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoWidgetMainFragment : Fragment() {

    private lateinit var binding: PhotoWidgetMainFragmentBinding
    private lateinit var viewModel: PhotoWidgetMainViewModel
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PhotoWidgetMainFragmentBinding.inflate(inflater,container,false)
        Log.d("PhotoWidgetMainFragment", "Binding initialized: $binding")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(PhotoWidgetMainViewModel::class.java)
        Log.d("PhotoWidgetMainFragment", "ViewModel initialized: $viewModel")

        viewModel.imageUri.observe(viewLifecycleOwner) { uri ->
            uri?.let {
                binding.cropIv.setImageURI(it)
                try {
                    requireActivity().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val inputStream = requireActivity().contentResolver.openInputStream(it)
                    val drawable = Drawable.createFromStream(inputStream, it.toString())
                    binding.cropIv.setImageDrawable(drawable)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                saveImageUriInPrefs(it)
            }
        }

        viewModel.cornerRadius.observe(viewLifecycleOwner) { radius ->
            radius?.let {
                setDrawableResRadius(binding.relLay, it)
                saveCornerRadiusInPrefs(it)
            }
        }

        viewModel.isDateVisible.observe(viewLifecycleOwner) { isVisible ->
            isVisible?.let {
                binding.dateText.visibility = if (it) View.VISIBLE else View.INVISIBLE
                saveDateVisibilityInPrefs(it)
            }
        }

        viewModel.dateText.observe(viewLifecycleOwner) { text ->
            text?.let {
                binding.dateText.text = it
                saveDateTextInPrefs(it)
            }
        }

        val rounds = mapOf(0 to R.drawable.round0, 10 to R.drawable.round10, 20 to R.drawable.round20,
            30 to R.drawable.round30, 40 to R.drawable.round40, 50 to R.drawable.round50, 60 to R.drawable.round60,
            70 to R.drawable.round70, 80 to R.drawable.round80, 90 to R.drawable.round90, 100 to R.drawable.round100,
            110 to R.drawable.round110, 120 to R.drawable.round120, 130 to R.drawable.round130,
            140 to R.drawable.round140, 150 to R.drawable.round150)
        val dialog = Dialog(requireContext())
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
            dialog.show(parentFragmentManager, "customDialogTag")
        }

        binding.imageBack.setOnClickListener{

            findNavController().navigate(R.id.action_photoWidgetMainFragment3_to_mainFragment)
        }


        binding.photoDateButton.setOnClickListener {
            viewModel.isDateVisible.value = !(viewModel.isDateVisible.value ?: false)
            updateWidgetWithDateText(viewModel.dateText.value, viewModel.isDateVisible.value!!)
        }

        binding.imageSave.setOnClickListener {
            viewModel.saveSettingsToFirebase()
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.data
                if (uri != null) {
                    requireActivity().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
        super.onPause()
        viewModel.saveToPrefs()
        updateWidget(viewModel.imageUri.value, viewModel.dateText.value, viewModel.isDateVisible.value ?: false, viewModel.cornerRadius.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.saveToPrefs()
        updateWidget(viewModel.imageUri.value, viewModel.dateText.value, viewModel.isDateVisible.value ?: false, viewModel.cornerRadius.value)
    }

    private fun getPermission(permissions: String) {
        Dexter.withContext(requireContext())
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
            requireContext(),
            CropImageActivity::class.java
        )
        intent.putExtra(CROP_IMAGE_INTENT_KEY,uri.toString())
        startActivityForResult(intent,12)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 12 && resultCode == Activity.RESULT_OK){
            if (data != null){
                val imageUris = mutableListOf<Uri>()
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    for (i in 0 until count) {
                        val imageUri = data.clipData!!.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else if (data.data != null) {
                    val imageUri = data.data
                    imageUris.add(imageUri!!)
                }

                imageUris.forEach { uri ->
                    requireActivity().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    cropImage(uri)
                    viewModel.imageUri.value = uri
                    viewModel.dateText.value = getFormattedPhotoDate(uri)
                    viewModel.isDateVisible.value = false
                    updateWidgetWithImage(uri)
                }
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
            val inputStream: InputStream? = requireActivity().contentResolver.openInputStream(uri)
            inputStream?.use {
                val exif = ExifInterface(it)
                exif.getAttribute(ExifInterface.TAG_DATETIME)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateWidget(uri: Uri?, dateText: String?, isDateVisible: Boolean, cornerRadius: Int?) {
        val widgetManager = AppWidgetManager.getInstance(requireContext())
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(requireContext(), NewAppWidget::class.java))

        for (widgetId in widgetIds) {
            val remoteViews = RemoteViews(requireContext().packageName, R.layout.new_app_widget)


            uri?.let {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                remoteViews.setImageViewBitmap(R.id.widget_image_view, bitmap)
            }


            remoteViews.setTextViewText(R.id.widgetDateText, dateText ?: "")
            remoteViews.setViewVisibility(R.id.widgetDateText, if (isDateVisible) View.VISIBLE else View.INVISIBLE)


            cornerRadius?.let {
                remoteViews.setInt(R.id.widget_frame, "setBackgroundResource", it)
            }

            widgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }

    private fun updateWidgetWithCorners(draw: Int) {
        val widgetManager=AppWidgetManager.getInstance(requireContext())
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(requireContext(), NewAppWidget::class.java))

        for (widgetId in widgetIds){
            val remoteViews = RemoteViews(requireContext().packageName, R.layout.new_app_widget)
             remoteViews.setInt(R.id.widget_frame, "setBackgroundResource", draw)
            widgetManager.updateAppWidget(widgetId, remoteViews)
        }
        saveCornerRadiusInPrefs(draw)
    }



    private fun updateWidgetWithDateText(dateText: String?, isVisible: Boolean) {
        val widgetManager = AppWidgetManager.getInstance(requireContext())
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(requireContext(), NewAppWidget::class.java))

        for (widgetId in widgetIds){
            val remoteViews = RemoteViews(requireContext().packageName, R.layout.new_app_widget)
            remoteViews.setTextViewText(R.id.widgetDateText, dateText ?: "")
            remoteViews.setViewVisibility(R.id.widgetDateText, if (isVisible) VISIBLE else INVISIBLE)
            widgetManager.updateAppWidget(widgetId, remoteViews)
        }
        saveDateVisibilityInPrefs(isVisible)
        saveDateTextInPrefs(dateText ?: "")
    }



    private fun updateWidgetWithImage(uri: Uri) {



        val widgetManager = AppWidgetManager.getInstance(requireContext())
        val widgetIds = widgetManager.getAppWidgetIds(ComponentName(requireContext(), NewAppWidget::class.java))

        for (widgetID in widgetIds){
            val remoteViews = RemoteViews(requireContext().packageName, R.layout.new_app_widget)

            val inputStream = requireContext().contentResolver.openInputStream(uri)

            val options = BitmapFactory.Options()
            options.inSampleSize=4
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)

            remoteViews.setImageViewBitmap(R.id.widget_image_view, bitmap)
            widgetManager.updateAppWidget(widgetID, remoteViews)
        }
        saveImageUriInPrefs(uri)
    }



    private  fun setDrawableResRadius(relLay: RelativeLayout,draw: Int){
        relLay.setBackgroundResource(0)
        relLay.setBackgroundResource(draw)
    }
    private fun saveCornerRadiusInPrefs(radius: Int) {
        val sharedPreferences = requireActivity().getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("corner_draw_key", radius)
        editor.apply()
    }

    private fun saveImageUriInPrefs(uri: Uri) {
        val prefs = requireActivity().getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("image_uri", uri.toString())
        editor.apply()
    }

    private fun saveDateVisibilityInPrefs(isVisible: Boolean) {
        val prefs = requireActivity().getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("date_visibility", isVisible)
        editor.apply()
    }

    private fun saveDateTextInPrefs(dateText: String) {
        val prefs = requireActivity().getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("date_text", dateText)
        editor.apply()
    }



}