package com.example.filkomplain

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.example.filkomplain.api.ApiService
import com.example.filkomplain.api.FetchProfileResponse
import com.example.filkomplain.api.GeneralApiResponse
import com.example.filkomplain.api.RetrofitClient
import com.example.filkomplain.api.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var editFotoProfil: ImageView
    private lateinit var btnEditPfp: Button

    private lateinit var editNamaProfil: EditText
    private lateinit var editNIMProfil: EditText
    private lateinit var editProdiProfil: EditText
    private lateinit var editEmailProfil: EditText
    private lateinit var editPhoneProfil: EditText

    private lateinit var btnSelesaiEditProfil: Button

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private var selectedImageUri: Uri? = null

    private var isFromGoogleLogin = false
    private val TAG = "ProfileEditActivity"

    private var oldNama: String? = null
    private var oldNIM: String? = null
    private var oldTelepon: String? = null
    private var oldProdi: String? = null
    private var oldEmail: String? = null
    private var oldPhotoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        initializeViews()
        setupListeners()

        isFromGoogleLogin = intent.getBooleanExtra("fromGoogleLogin", false)
        loadUserProfileFromApi()

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                Glide.with(this@ProfileEditActivity)
                    .load(it)
                    .circleCrop()
                    .placeholder(R.drawable.profile_photo)
                    .error(R.drawable.profile_photo)
                    .into(editFotoProfil)
                btnSelesaiEditProfil.isEnabled = hasProfileDataChanged()
            }
        }
    }

    private fun initializeViews() {
        editFotoProfil = findViewById(R.id.editFotoProfil)
        btnEditPfp = findViewById(R.id.btnEditPfp)
        editNamaProfil = findViewById(R.id.editNamaProfil)
        editNIMProfil = findViewById(R.id.editNIMProfil)
        editProdiProfil = findViewById(R.id.editProdiProfil)
        editEmailProfil = findViewById(R.id.editEmailProfil)
        editPhoneProfil = findViewById(R.id.editPhoneProfil)
        btnSelesaiEditProfil = findViewById(R.id.btnSelesaiEditProfil)
    }

    private fun setupListeners() {
        btnSelesaiEditProfil.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnSelesaiEditProfil.isEnabled = hasProfileDataChanged()
            }
        }
        editNamaProfil.addTextChangedListener(textWatcher)
        editNIMProfil.addTextChangedListener(textWatcher)
        editProdiProfil.addTextChangedListener(textWatcher)
        editPhoneProfil.addTextChangedListener(textWatcher)

        btnEditPfp.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnSelesaiEditProfil.setOnClickListener {
            updateProfileViaApi { success ->
                if (success) {
                    navigateToNextScreen()
                }
            }
        }
    }

    private fun hasProfileDataChanged(): Boolean {
        val currentUINama = editNamaProfil.text.toString().trim()
        val newNIM = editNIMProfil.text.toString().trim()
        val newProdi = editProdiProfil.text.toString().trim()
        val newTelepon = editPhoneProfil.text.toString().trim()

        val namaChanged = currentUINama != oldNama && (currentUINama.isNotEmpty() || oldNama?.isNotEmpty() == true)
        val nimChanged = newNIM != oldNIM && (newNIM.isNotEmpty() || oldNIM?.isNotEmpty() == true)
        val prodiChanged = newProdi != oldProdi && (newProdi.isNotEmpty() || oldProdi?.isNotEmpty() == true)
        val teleponChanged = newTelepon != oldTelepon && (newTelepon.isNotEmpty() || oldTelepon?.isNotEmpty() == true)
        val imageChanged = selectedImageUri != null

        return namaChanged || nimChanged || prodiChanged || teleponChanged || imageChanged
    }

    private fun loadUserProfileFromApi() {
        val authToken = SessionManager.getAuthToken(this)
        if (authToken == null) {
            Toast.makeText(this, "Sesi tidak valid. Silakan login kembali.", Toast.LENGTH_LONG).show()
            goToLogin()
            return
        }

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getUserProfile("Bearer $authToken").enqueue(object : Callback<FetchProfileResponse> {
            override fun onResponse(call: Call<FetchProfileResponse>, response: Response<FetchProfileResponse>) {
                if (response.isSuccessful) {
                    val profileResponse = response.body()
                    if (profileResponse != null && profileResponse.success) {
                        val userData = profileResponse.data

                        oldEmail = userData?.email
                        oldNama = userData?.username
                        oldNIM = userData?.nim
                        oldTelepon = userData?.phone_number
                        oldProdi = userData?.program_studi
                        oldPhotoUrl = userData?.profile_image_url

                        editEmailProfil.setText(oldEmail ?: "")
                        editEmailProfil.isEnabled = false
                        editNamaProfil.setText(oldNama ?: "")
                        editNamaProfil.isEnabled = true

                        editNIMProfil.setText(oldNIM ?: "")
                        editPhoneProfil.setText(oldTelepon ?: "")
                        editProdiProfil.setText(oldProdi ?: "")
                        editProdiProfil.isEnabled = true

                        if (!oldPhotoUrl.isNullOrEmpty()) {
                            Glide.with(this@ProfileEditActivity)
                                .load(oldPhotoUrl)
                                .placeholder(R.drawable.profile_photo)
                                .error(R.drawable.profile_photo)
                                .circleCrop()
                                .into(editFotoProfil)
                        } else {
                            Glide.with(this@ProfileEditActivity)
                                .load(R.drawable.profile_photo)
                                .circleCrop()
                                .into(editFotoProfil)
                        }
                        btnEditPfp.isEnabled = true
                        btnSelesaiEditProfil.isEnabled = false
                        Log.d(TAG, "User profile loaded for editing. Prodi: $oldProdi")
                    } else {
                        Toast.makeText(this@ProfileEditActivity, profileResponse?.message ?: "Gagal mengambil data profil.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ProfileEditActivity, "Gagal mengambil data profil: Error ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<FetchProfileResponse>, t: Throwable) {
                Toast.makeText(this@ProfileEditActivity, "Gagal mengambil data profil: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun goToLogin() {
        SessionManager.clearSession(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateProfileViaApi(onComplete: (Boolean) -> Unit) {
        val authToken = SessionManager.getAuthToken(this)
        if (authToken == null) {
            Toast.makeText(this, "Sesi tidak valid, silakan login kembali", Toast.LENGTH_SHORT).show()
            goToLogin()
            onComplete(false)
            return
        }

        if (!hasProfileDataChanged()) {
            Toast.makeText(this, "Tidak ada perubahan data untuk disimpan.", Toast.LENGTH_SHORT).show()
            onComplete(true)
            return
        }

        btnSelesaiEditProfil.isEnabled = false
        btnSelesaiEditProfil.text = "Menyimpan..."

        val currentUINama = editNamaProfil.text.toString().trim()
        val newNIM = editNIMProfil.text.toString().trim()
        val newProdi = editProdiProfil.text.toString().trim()
        val newTelepon = editPhoneProfil.text.toString().trim()

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val call: Call<GeneralApiResponse>

        if (selectedImageUri != null) {
            val file = uriToFile(selectedImageUri!!, this)
            if (file == null) {
                Toast.makeText(this, "Gagal memproses file gambar.", Toast.LENGTH_SHORT).show()
                btnSelesaiEditProfil.isEnabled = true
                btnSelesaiEditProfil.text = "Selesai"
                onComplete(false)
                return
            }

            val usernameRB = currentUINama.toRequestBody("text/plain".toMediaTypeOrNull())
            val nimRB = newNIM.toRequestBody("text/plain".toMediaTypeOrNull())
            val prodiRB = newProdi.toRequestBody("text/plain".toMediaTypeOrNull())
            val teleponRB = newTelepon.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val profileImagePart = MultipartBody.Part.createFormData("profile_image", file.name, requestFile)

            Log.d(TAG, "Updating profile with ATTACHMENT (multipart)")
            call = apiService.updateUserProfileWithAttachment(
                "Bearer $authToken",
                username = usernameRB.takeIf { currentUINama.isNotEmpty() && currentUINama != oldNama },
                nim = nimRB.takeIf { newNIM.isNotEmpty() && newNIM != oldNIM },
                programStudi = prodiRB.takeIf { newProdi.isNotEmpty() && newProdi != oldProdi },
                phoneNumber = teleponRB.takeIf { newTelepon.isNotEmpty() && newTelepon != oldTelepon },
                profileImageFile = profileImagePart
            )

        } else {
            val updateRequest = UpdateProfileRequest(
                username = if (currentUINama != oldNama && currentUINama.isNotEmpty()) currentUINama else null,
                nim = if (newNIM != oldNIM && newNIM.isNotEmpty()) newNIM else null,
                program_studi = if (newProdi != oldProdi && newProdi.isNotEmpty()) newProdi else null,
                phone_number = if (newTelepon != oldTelepon && newTelepon.isNotEmpty()) newTelepon else null,
            )
            Log.d(TAG, "Updating profile WITHOUT attachment (JSON): $updateRequest")
            call = apiService.updateUserProfile("Bearer $authToken", updateRequest)
        }

        call.enqueue(object: Callback<GeneralApiResponse> {
            override fun onResponse(call: Call<GeneralApiResponse>, response: Response<GeneralApiResponse>) {
                btnSelesaiEditProfil.isEnabled = true
                btnSelesaiEditProfil.text = "Selesai"

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        Toast.makeText(this@ProfileEditActivity, apiResponse.message ?: "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()

                        val usernameChanged = currentUINama != oldNama && currentUINama.isNotEmpty()
                        if (usernameChanged) {
                            SessionManager.saveUsername(this@ProfileEditActivity, currentUINama)
                            oldNama = currentUINama
                        }
                        if (newNIM != oldNIM && newNIM.isNotEmpty()) oldNIM = newNIM
                        if (newProdi != oldProdi && newProdi.isNotEmpty()) oldProdi = newProdi
                        if (newTelepon != oldTelepon && newTelepon.isNotEmpty()) oldTelepon = newTelepon

                        if (selectedImageUri != null) {
                            selectedImageUri = null
                        }
                        onComplete(true)
                    } else {
                        Toast.makeText(this@ProfileEditActivity, apiResponse?.message ?: "Gagal menyimpan profil.", Toast.LENGTH_LONG).show()
                        onComplete(false)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var errorMessage = "Gagal menyimpan profil: Error ${response.code()}"
                    try {
                        if (errorBody != null) {
                            val errorResponse = RetrofitClient.instance.responseBodyConverter<GeneralApiResponse>(
                                GeneralApiResponse::class.java,
                                GeneralApiResponse::class.java.annotations
                            ).convert(response.errorBody()!!)
                            if (errorResponse != null && errorResponse.message.isNotBlank()) {
                                errorMessage = errorResponse.message
                            } else if (errorBody.isNotBlank()){
                                errorMessage = "$errorMessage - $errorBody"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing error body: $e")
                    }
                    Toast.makeText(this@ProfileEditActivity, errorMessage, Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }

            override fun onFailure(call: Call<GeneralApiResponse>, t: Throwable) {
                btnSelesaiEditProfil.isEnabled = true
                btnSelesaiEditProfil.text = "Selesai"
                Toast.makeText(this@ProfileEditActivity, "Gagal menyimpan profil: ${t.message}", Toast.LENGTH_LONG).show()
                onComplete(false)
            }
        })
    }

    private fun navigateToNextScreen() {
        if (isFromGoogleLogin) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        } else {
            val intent = Intent(this, ProfileShowActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var fileName = "temp_image_${System.currentTimeMillis()}"
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
            val finalFileName = "profile_upload_${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"

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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, ProfileShowActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}