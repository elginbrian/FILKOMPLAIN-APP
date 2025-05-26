package com.example.filkomplain

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.example.filkomplain.api.CreateReportRequest
import com.example.filkomplain.api.CreateReportResponse
import com.example.filkomplain.api.FetchProfileResponse
import com.example.filkomplain.api.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

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

    private var selectedImageUri: Uri? = null
    private var selectedFileName: String? = null

    private val TAG = "KomplainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_komplain)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        btnUpFotoKomplain = findViewById(R.id.btnUpFotoKomplain)
        btnBuatKomplain = findViewById(R.id.btnBuatKomplain)
        textUpFoto = findViewById(R.id.textUpFoto)
        textDescUpFoto = findViewById(R.id.textDescUpFoto)
        editJudul = findViewById(R.id.editJudul)
        editDescKomplain = findViewById(R.id.editDescKomplain)
        editLokasi = findViewById(R.id.editLokasi)
        editKontak = findViewById(R.id.editKontak)

        loadProfileAndPreFillContact()

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                handleImageSelection(it)
            }
        }

        updateUploadButtonUI(false)

        btnUpFotoKomplain.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnBuatKomplain.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnBuatKomplain.isEnabled = areAllFieldsFilled()
            }
        }

        editJudul.addTextChangedListener(textWatcher)
        editDescKomplain.addTextChangedListener(textWatcher)
        editLokasi.addTextChangedListener(textWatcher)
        editKontak.addTextChangedListener(textWatcher)

        btnBuatKomplain.setOnClickListener {
            submitReport()
        }
    }

    private fun loadProfileAndPreFillContact() {
        val authToken = SessionManager.getAuthToken(this)
        if (authToken == null) {
            Log.w(TAG, "Auth token is null, cannot fetch profile for contact.")
            return
        }

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getUserProfile("Bearer $authToken").enqueue(object: Callback<FetchProfileResponse> {
            override fun onResponse(call: Call<FetchProfileResponse>, response: Response<FetchProfileResponse>) {
                if (response.isSuccessful) {
                    val profileResponse = response.body()
                    if (profileResponse != null && profileResponse.success) {
                        val phoneNumberFromProfile = profileResponse.data?.phone_number
                        if (!phoneNumberFromProfile.isNullOrEmpty()) {
                            editKontak.setText(phoneNumberFromProfile)
                            Log.d(TAG, "Contact pre-filled from profile API: $phoneNumberFromProfile")
                        } else {
                            Log.d(TAG, "Phone number is empty in profile API response.")
                        }
                    } else {
                        Log.e(TAG, "Failed to get profile data: ${profileResponse?.message}")
                    }
                } else {
                    Log.e(TAG, "Error fetching profile for contact: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<FetchProfileResponse>, t: Throwable) {
                Log.e(TAG, "Network failure when fetching profile for contact: ${t.message}", t)
            }
        })
    }

    private fun areAllFieldsFilled(): Boolean {
        return editDescKomplain.text.toString().trim().isNotEmpty() &&
                editLokasi.text.toString().trim().isNotEmpty() &&
                editKontak.text.toString().trim().isNotEmpty()
    }

    private fun handleImageSelection(uri: Uri) {
        selectedImageUri = uri
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    selectedFileName = it.getString(nameIndex)
                }
            }
        }
        updateUploadButtonUI(true, selectedFileName)
        btnBuatKomplain.isEnabled = areAllFieldsFilled()
    }

    private fun submitReport() {
        val authToken = SessionManager.getAuthToken(this)
        val usernameFromSession = SessionManager.getUsername(this)

        if (authToken == null || usernameFromSession == null) {
            Toast.makeText(this, "Sesi tidak valid. Silakan login kembali.", Toast.LENGTH_LONG).show()
            goToLogin()
            return
        }

        val judulLaporan = editJudul.text.toString().trim()
        val deskripsiLaporan = editDescKomplain.text.toString().trim()
        val content = if (judulLaporan.isNotEmpty()) "$judulLaporan\n\n$deskripsiLaporan" else deskripsiLaporan

        val place = editLokasi.text.toString().trim()
        val phoneNumber = editKontak.text.toString().trim()
        val status = "hold"

        btnBuatKomplain.isEnabled = false
        btnBuatKomplain.text = "Mengirim..."

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val call: Call<CreateReportResponse>

        if (selectedImageUri != null) {
            val file = uriToFile(selectedImageUri!!, this)
            if (file == null) {
                Toast.makeText(this, "Gagal memproses file gambar.", Toast.LENGTH_SHORT).show()
                btnBuatKomplain.isEnabled = true
                btnBuatKomplain.text = "Buat Laporan"
                return
            }

            val userNameRB = usernameFromSession.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentRB = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val placeRB = place.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneNumberRB = phoneNumber.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusRB = status.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val attachmentPart = MultipartBody.Part.createFormData("attachment", file.name, requestFile)

            call = apiService.createReportWithAttachment(
                token = "Bearer $authToken",
                userName = userNameRB,
                content = contentRB,
                place = placeRB,
                phoneNumber = phoneNumberRB,
                status = statusRB,
                attachment = attachmentPart
            )
        } else {
            val createReportRequest = CreateReportRequest(
                user_name = usernameFromSession,
                content = content,
                place = place,
                phone_number = phoneNumber,
                status = status,
                attachment = ""
            )
            call = apiService.createReport("Bearer $authToken", createReportRequest)
        }

        call.enqueue(object: Callback<CreateReportResponse> {
            override fun onResponse(call: Call<CreateReportResponse>, response: Response<CreateReportResponse>) {
                btnBuatKomplain.isEnabled = true
                btnBuatKomplain.text = "Buat Laporan"

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        Toast.makeText(this@KomplainActivity, apiResponse.message ?: "Laporan berhasil dibuat!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        val errorMessage = apiResponse?.message ?: "Gagal membuat laporan."
                        Toast.makeText(this@KomplainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "API Logic Error: $errorMessage, Raw Error Body: ${response.errorBody()?.string()}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var errorMessage = "Gagal membuat laporan: Error ${response.code()}"
                    try {
                        if (errorBody != null) {
                            val errorResponse = RetrofitClient.instance.responseBodyConverter<CreateReportResponse>(
                                CreateReportResponse::class.java, CreateReportResponse::class.java.annotations
                            ).convert(response.errorBody()!!)
                            if (errorResponse != null && errorResponse.message?.isNotBlank() == true) {
                                errorMessage = errorResponse.message!!
                            } else if (errorBody.isNotBlank()){
                                errorMessage = "$errorMessage - $errorBody"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing error body: $e")
                    }
                    Toast.makeText(this@KomplainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "HTTP Error: ${response.code()}, Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<CreateReportResponse>, t: Throwable) {
                btnBuatKomplain.isEnabled = true
                btnBuatKomplain.text = "Buat Laporan"
                Log.e(TAG, "Network Failure: ${t.message}", t)
                Toast.makeText(this@KomplainActivity, "Gagal membuat laporan: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun navigateToMain() {
        val intent = Intent(this@KomplainActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
        finish()
    }

    private fun updateUploadButtonUI(imageSelected: Boolean, fileName: String? = null) {
        if (imageSelected) {
            btnUpFotoKomplain.apply {
                background = ContextCompat.getDrawable(this@KomplainActivity, R.drawable.bg_btn_uploaded_photo)
            }
            textUpFoto.text = fileName ?: "Foto dipilih"
            textDescUpFoto.text = "Klik untuk mengganti atau menghapus foto."
        } else {
            btnUpFotoKomplain.apply {
                background = ContextCompat.getDrawable(this@KomplainActivity, R.drawable.bg_btn_up_photo_ripple)
                isClickable = true
            }
            textUpFoto.text = "Unggah foto"
            textDescUpFoto.text = "Kamu dapat mengunggah\nfoto laporanmu di sini (opsional)"
            selectedImageUri = null
            selectedFileName = null
        }
        btnBuatKomplain.isEnabled = areAllFieldsFilled()
    }

    private fun uriToFile(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var localFileName = "temp_image_${System.currentTimeMillis()}"
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        localFileName = it.getString(nameIndex)
                    }
                }
            }
            val extension = localFileName.substringAfterLast('.', "")
            val finalFileName = "upload_${System.currentTimeMillis()}" + if (extension.isNotEmpty()) ".$extension" else ".jpg"

            val tempFile = File(context.cacheDir, finalFileName)
            tempFile.outputStream().use { fileOut ->
                inputStream.copyTo(fileOut)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to File", e)
            null
        }
    }

    private fun goToLogin() {
        SessionManager.clearSession(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom)
        finish()
    }
}