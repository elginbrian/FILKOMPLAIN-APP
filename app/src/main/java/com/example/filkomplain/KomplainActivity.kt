package com.example.filkomplain

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.filkomplain.api.ApiService
import com.example.filkomplain.api.RetrofitClient
import com.example.filkomplain.model.KomplainModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Date
import java.util.Locale

class KomplainActivity : AppCompatActivity() {

    private lateinit var btnUpFotoKomplain: Button
    private lateinit var textUpFoto: TextView
    private lateinit var textDescUpFoto: TextView

    private lateinit var editJudul: EditText
    private lateinit var editDescKomplain: EditText
    private lateinit var editLokasi: EditText
    private lateinit var editKontak: EditText

    private lateinit var btnBuatKomplain: Button

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    private var uploadedImageUrl: String? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_komplain)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Bind views
        btnUpFotoKomplain = findViewById(R.id.btnUpFotoKomplain)
        btnBuatKomplain = findViewById(R.id.btnBuatKomplain)
        textUpFoto = findViewById(R.id.textUpFoto)
        textDescUpFoto = findViewById(R.id.textDescUpFoto)
        editJudul = findViewById(R.id.editJudul)
        editDescKomplain = findViewById(R.id.editDescKomplain)
        editLokasi = findViewById(R.id.editLokasi)
        editKontak = findViewById(R.id.editKontak)

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                uploadImageToServer(it)
            }
        }

        tampilanSebelumUpload()

        btnUpFotoKomplain.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnBuatKomplain.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnBuatKomplain.isEnabled = areAllFieldsFilled() && (uploadedImageUrl != null)
            }
        }

        editJudul.addTextChangedListener(textWatcher)
        editDescKomplain.addTextChangedListener(textWatcher)
        editLokasi.addTextChangedListener(textWatcher)
        editKontak.addTextChangedListener(textWatcher)

        btnBuatKomplain.setOnClickListener {
            saveData()
        }
    }

    private fun areAllFieldsFilled(): Boolean {
        return editJudul.text.toString().trim().isNotEmpty() &&
                editDescKomplain.text.toString().trim().isNotEmpty() &&
                editLokasi.text.toString().trim().isNotEmpty() &&
                editKontak.text.toString().trim().isNotEmpty()
    }

    private fun saveData() {
        val judul = editJudul.text.toString()
        val deskripsi = editDescKomplain.text.toString()
        val lokasi = editLokasi.text.toString()
        val kontak = editKontak.text.toString()

        val imageUrl = uploadedImageUrl
        if (imageUrl == null) {
            return
        }

        val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val timestampNow = System.currentTimeMillis()

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val komplainData = KomplainModel(judul, deskripsi, lokasi, kontak, currentDate, imageUrl, timestampNow, userId ?: "")

        saveKomplainToFirestore(komplainData) { success ->
            if (success) {
                Toast.makeText(this, "Laporanmu berhasil terkirim!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan data komplain", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveKomplainToFirestore(komplainData: KomplainModel, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val komplainRef = db.collection("komplain").document() // auto-ID

        komplainRef.set(komplainData)
            .addOnSuccessListener {
                callback(true) // Success
            }
            .addOnFailureListener { exception ->
                Log.w("Komplain2Activity", "Error saving document", exception)
                callback(false) // Fail
            }
    }

    private fun tampilanSebelumUpload() {
        btnUpFotoKomplain.apply {
            background = ContextCompat.getDrawable(this@KomplainActivity, R.drawable.bg_btn_up_photo_ripple)
            isClickable = true
        }
        textUpFoto.text = "Unggah foto"
        textDescUpFoto.text = "Kamu dapat mengunggah\nfoto laporanmu di sini"
    }

    private fun tampilanSetelahUploadBerhasil() {
        btnUpFotoKomplain.apply {
            background = ContextCompat.getDrawable(this@KomplainActivity, R.drawable.bg_btn_uploaded_photo)
            isClickable = false
        }
        textUpFoto.text = "Foto berhasil diunggah"
        textDescUpFoto.text = "Pastikan kamu mengunggah \nfoto yang tepat"
    }

    private fun uploadImageToServer(uri: Uri) {
        val file = uriToFile(uri, this)
        if (file == null) {
            Toast.makeText(this, "Gagal mendapatkan file dari gambar", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val description = "Foto Komplain".toRequestBody("text/plain".toMediaTypeOrNull())

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.uploadImage(body, description).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val raw = response.body()?.string()
                    Log.d("UploadDebug", "Raw response: $raw")
                    Toast.makeText(this@KomplainActivity, "Upload berhasil", Toast.LENGTH_SHORT).show()

                    val prefix = "File berhasil di-upload: "
                    if (raw != null && raw.startsWith(prefix)) {
                        val fileName = raw.substringAfter(prefix).trim()
                        val serverUrl = RetrofitClient.getUploadsUrl()
                        uploadedImageUrl = "$serverUrl$fileName"
                        Log.d("UploadDebug", "uploadedImageUrl = $uploadedImageUrl")

                        runOnUiThread {
                            tampilanSetelahUploadBerhasil()
                            btnBuatKomplain.isEnabled = areAllFieldsFilled() && uploadedImageUrl != null
                        }
                    } else {
                        Log.e("UploadDebug", "Respons tidak sesuai format")
                        Toast.makeText(this@KomplainActivity, "Format respon tidak dikenali", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    val error = response.errorBody()?.string()
                    Log.e("UploadDebug", "Error response: $error")
                    Toast.makeText(this@KomplainActivity, "Upload gagal: $error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("UploadDebug", "Failure: ${t.message}")
                Toast.makeText(this@KomplainActivity, "Gagal upload: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uriToFile(uri: Uri, context: Context): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("temp_image", ".jpg", context.cacheDir)
        tempFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        return tempFile
    }


    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
        finish()
    }
}