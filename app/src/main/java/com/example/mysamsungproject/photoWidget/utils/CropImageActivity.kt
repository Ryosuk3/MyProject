package com.example.mysamsungproject.photoWidget.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
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
            val bitmap = binding.cropLayout.croppedImage
            val imageUri = saveImageToFile(bitmap)
            val resultIntent = Intent()
            resultIntent.putExtra(CROP_IMAGE_URI_KEY, imageUri.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this@CropImageActivity, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageToFile(bitmap: Bitmap): Uri {
        val directory = getDir("images", Context.MODE_PRIVATE)
        val files = directory.listFiles()

        files?.forEach { file ->
            val fileName = file.name
            if (fileName.matches(Regex("^photo.*\\.jpg$"))) {
                file.delete()
            }
        }
        val file = File(directory, "photo_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()
        return Uri.fromFile(file)
    }

}