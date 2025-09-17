package com.example.enpix

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class DecryptActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var imagePreview: ImageView
    private lateinit var editPassword: TextInputEditText
    private lateinit var txtMessage: TextView
    private lateinit var btnDecrypt: MaterialButton
    private lateinit var btnSelect: MaterialButton
    private lateinit var btnCopy: MaterialButton
    private lateinit var btnShare: MaterialButton

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imagePreview.setImageURI(it)
            txtMessage.text = "" // reset message on new selection
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)

        imagePreview = findViewById(R.id.imagePreview)
        editPassword = findViewById(R.id.editPassword)
        txtMessage = findViewById(R.id.txtMessage)
        btnDecrypt = findViewById(R.id.btnDecryptNow)
        btnSelect = findViewById(R.id.btnSelectImage)
        btnCopy = findViewById(R.id.btnCopyMessage)
        btnShare = findViewById(R.id.btnShareMessage)

        // Select image
        btnSelect.setOnClickListener { pickImage.launch("image/*") }

        // Decrypt message
        btnDecrypt.setOnClickListener {
            val uri = selectedImageUri
            val pwd = editPassword.text?.toString().orEmpty()

            if (uri == null) {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pwd.isBlank()) {
                Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val input = contentResolver.openInputStream(uri)
                    ?: error("Cannot open image")
                val bm = BitmapFactory.decodeStream(input)
                    ?: error("Cannot decode image")
                input.close()

                val message = StegoUtils.extractMessageFromBitmap(bm, pwd)

                // ✅ Show message inside this activity
                txtMessage.text = message

                Toast.makeText(this, "Message extracted successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                txtMessage.text = ""
                Toast.makeText(
                    this,
                    "❌ Wrong password or corrupted image",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Copy message
        btnCopy.setOnClickListener {
            val msg = txtMessage.text.toString()
            if (msg.isNotBlank()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Decrypted Message", msg))
                Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No message to copy", Toast.LENGTH_SHORT).show()
            }
        }

        // Share message
        btnShare.setOnClickListener {
            val msg = txtMessage.text.toString()
            if (msg.isNotBlank()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, msg)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Decrypted Message"))
            } else {
                Toast.makeText(this, "No message to share", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
