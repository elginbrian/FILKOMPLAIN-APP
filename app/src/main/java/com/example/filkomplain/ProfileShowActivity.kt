package com.example.filkomplain

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.example.filkomplain.api.ApiService
import com.example.filkomplain.api.FetchProfileResponse
import com.example.filkomplain.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileShowActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var namaEditText: EditText
    private lateinit var nimEditText: EditText
    private lateinit var prodiEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText

    private lateinit var btnEditProfil: Button
    private lateinit var btnLogout: Button

    private val TAG = "ProfileShowActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_show)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        profileImageView = findViewById(R.id.editFotoProfil)
        namaEditText = findViewById(R.id.showNamaProfil)
        nimEditText = findViewById(R.id.showNIMProfil)
        prodiEditText = findViewById(R.id.showProdiProfil)
        emailEditText = findViewById(R.id.showEmailProfil)
        phoneEditText = findViewById(R.id.showPhoneProfil)
        btnEditProfil = findViewById(R.id.btnEditProfil)
        btnLogout = findViewById(R.id.btnLogout)

        loadUserProfileFromApi()

        btnEditProfil.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        btnLogout.setOnClickListener {
            SessionManager.clearSession(this)
            val intent = Intent(this, SplashActivity::class.java) // DIUBAH KE SplashActivity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
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
                        namaEditText.setText(userData?.username ?: "")
                        nimEditText.setText(userData?.nim ?: "")
                        prodiEditText.setText(userData?.program_studi ?: "")
                        emailEditText.setText(userData?.email ?: "")
                        phoneEditText.setText(userData?.phone_number ?: "")

                        val photoUrl = userData?.profile_image_url
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this@ProfileShowActivity)
                                .load(photoUrl)
                                .placeholder(R.drawable.profile_photo)
                                .error(R.drawable.profile_photo)
                                .circleCrop()
                                .into(profileImageView)
                        } else {
                            Glide.with(this@ProfileShowActivity)
                                .load(R.drawable.profile_photo)
                                .circleCrop()
                                .into(profileImageView)
                        }
                        Log.d(TAG, "User profile loaded successfully. Photo URL: $photoUrl")
                    } else {
                        val errorMessage = profileResponse?.message ?: "Gagal memuat data profil."
                        Toast.makeText(this@ProfileShowActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "API Logic Error (Profile): $errorMessage")
                        Glide.with(this@ProfileShowActivity).load(R.drawable.profile_photo).circleCrop().into(profileImageView)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@ProfileShowActivity, "Gagal memuat profil: Error ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "API HTTP Error (Profile) ${response.code()}: $errorBody")
                    Glide.with(this@ProfileShowActivity).load(R.drawable.profile_photo).circleCrop().into(profileImageView)
                }
            }

            override fun onFailure(call: Call<FetchProfileResponse>, t: Throwable) {
                Toast.makeText(this@ProfileShowActivity, "Gagal memuat profil: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Network failure (Profile): ", t)
                Glide.with(this@ProfileShowActivity).load(R.drawable.profile_photo).circleCrop().into(profileImageView)
            }
        })
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}