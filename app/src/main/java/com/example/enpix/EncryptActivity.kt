package com.example.enpix

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EncryptActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var imagePreview: ImageView
    private lateinit var editMessage: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var btnEncrypt: MaterialButton
    private lateinit var btnSelect: MaterialButton
    private lateinit var btnShare: MaterialButton

    private var lastSavedUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imagePreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypt)

        // Initialize views
        imagePreview = findViewById(R.id.imagePreview)
        editMessage = findViewById(R.id.editMessage)
        editPassword = findViewById(R.id.editPassword)
        btnEncrypt = findViewById(R.id.btnEncryptNow)
        btnSelect = findViewById(R.id.btnSelectImage)
        btnShare = findViewById(R.id.btnShareImage)

        // Select image
        btnSelect.setOnClickListener { pickImage.launch("image/*") }

        // Encrypt and save
        btnEncrypt.setOnClickListener {
            val uri = selectedImageUri
            val msg = editMessage.text?.toString().orEmpty()
            val pwd = editPassword.text?.toString().orEmpty()

            if (uri == null) {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (msg.isBlank() || pwd.isBlank()) {
                Toast.makeText(this, "Message and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    btnEncrypt.isEnabled = false

                    val (savedUri, size) = withContext(Dispatchers.IO) {
                        val input = contentResolver.openInputStream(uri)
                            ?: error("Cannot open image input stream")

                        val bm = BitmapFactory.decodeStream(input)
                            ?: error("Cannot decode bitmap from image")
                        input.close()

                        val src = bm.copy(Bitmap.Config.ARGB_8888, true)
                        val outBm = StegoUtils.embedMessageInBitmap(src, msg, pwd)

                        savePngToPictures(outBm)
                    }

                    lastSavedUri = savedUri
                    imagePreview.setImageURI(savedUri)

                    Toast.makeText(
                        this@EncryptActivity,
                        "✅ Stego image saved (${size} bytes)",
                        Toast.LENGTH_LONG
                    ).show()

                    // Auto-share saved PNG
                    shareImage(savedUri)

                } catch (e: Exception) {
                    Toast.makeText(
                        this@EncryptActivity,
                        "❌ Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnEncrypt.isEnabled = true
                }
            }
        }

        // Share manually
        btnShare.setOnClickListener {
            lastSavedUri?.let { shareImage(it) }
                ?: Toast.makeText(this, "No image saved yet!", Toast.LENGTH_SHORT).show()
        }
    }

    /** Save PNG to Pictures/EnPix and return Uri & size */
    private suspend fun savePngToPictures(bitmap: Bitmap): Pair<Uri, Long> =
        withContext(Dispatchers.IO) {
            val pngBytes = StegoUtils.bitmapToPngBytes(bitmap)
            val filename = "enpix_stego_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EnPix")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: error("Failed to create MediaStore entry")

                contentResolver.openOutputStream(uri)?.use { it.write(pngBytes) }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)

                Pair(uri, pngBytes.size.toLong())
            } else {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                ) ?: error("Failed to create entry")

                contentResolver.openOutputStream(uri)?.use { it.write(pngBytes) }
                Pair(uri, pngBytes.size.toLong())
            }
        }

    /** Share saved PNG */
    private fun shareImage(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Stego Image"))
    }
}
