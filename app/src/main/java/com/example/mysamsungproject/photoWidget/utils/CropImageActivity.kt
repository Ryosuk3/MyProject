package com.example.mysamsungproject.photoWidget.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.mysamsungproject.databinding.ActivityCropImageBinding
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class CropImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropImageBinding

    companion object{
        const val CROP_IMAGE_INTENT_KEY = "image-uri"
        const val CROP_IMAGE_B_ARRAY_KEY = "image-bitmap"
        const val CROP_IMAGE_URI_KEY="crop_image_uri"
    }

    private var imageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        imageUri = intent.getStringExtra(CROP_IMAGE_INTENT_KEY)


        binding.cropLayout.setImageUriAsync(Uri.parse(imageUri))
        binding.cropLayout.cropShape = CropImageView.CropShape.RECTANGLE
        binding.cropLayout.setFixedAspectRatio(true)
        binding.cropLayout.isAutoZoomEnabled = true
        binding.cropLayout.setMultiTouchEnabled(true)


        binding.ivSave.setOnClickListener {
            saveImage()
        }
    }

    private fun saveImage() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            val outputStream = FileOutputStream(imageFile)
            val bitmap = binding.cropLayout.croppedImage
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()


            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, imageFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)


            contentResolver.openOutputStream(imageUri!!).use { outputStream ->
                if (outputStream!= null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }

            val resultIntent = Intent()
            resultIntent.putExtra(CROP_IMAGE_URI_KEY, imageUri.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this@CropImageActivity, e.message, Toast.LENGTH_SHORT).show()
        }
    }
}