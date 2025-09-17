# EnPix – Image Steganography Android App

EnPix is an Android application that implements **image steganography** and **encryption** to provide a secure way of hiding sensitive information inside images.

## 🔐 Features

* **Login & Registration** system for user authentication.
* **Image Steganography**: Hide secret messages inside PNG images.
* **Encryption & Decryption** with password protection.
* **User-friendly Interface** with Material Design components.
* **Secure Message Extraction**: Only original PNG stego images can be decrypted.

## ⚡ Tech Stack

* **Language**: Kotlin
* **Framework**: Android SDK
* **Database**: SQLite (for user info)
* **Libraries**:

  * Material Components
  * Glide (for image handling)

## 🚨 Known Issues

* Images sent via apps that **compress or convert PNGs (WhatsApp, Email inline attachments, etc.)** will not decrypt properly.
* To preserve steganography data, images must be shared as **files/documents**.

## 📱 Usage

1. Register/Login to the app.
2. Select an image and embed your secret message with a password.
3. Share the generated **.png** file.
4. The receiver selects the file in the app and enters the correct password to decrypt the hidden message.

## 📌 Future Enhancements

* Add cloud-based storage for encrypted images.
* Implement AES encryption before embedding.
* Improve file integrity checks (hash verification).

---

Made with ❤️ by **Suyash Parab (suyash9664)** for **B.Sc CS Final Year Project**.
