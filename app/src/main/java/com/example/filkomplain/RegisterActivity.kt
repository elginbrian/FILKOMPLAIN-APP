package com.example.filkomplain

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import android.os.Bundle
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
// Hapus import Firebase Auth jika tidak digunakan lagi di activity ini untuk registrasi
// import com.google.firebase.auth.FirebaseAuth
// Hapus import Firestore jika tidak digunakan lagi di activity ini untuk registrasi
// import com.google.firebase.firestore.FirebaseFirestore
import com.example.filkomplain.api.ApiService
import com.example.filkomplain.api.GeneralApiResponse
import com.example.filkomplain.api.RegisterRequest
import com.example.filkomplain.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var editNama: EditText
    private lateinit var editNIM: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnDaftar: Button
    private lateinit var textMasuk: TextView

    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        editNama = findViewById(R.id.editNama)
        editEmail = findViewById(R.id.editEmail)
        editPassword = findViewById(R.id.editPassword)
        btnDaftar = findViewById(R.id.btnDaftar)
        textMasuk = findViewById(R.id.textMasuk)

        btnDaftar.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnDaftar.isEnabled = areAllFieldsFilled()
            }
        }

        editNama.addTextChangedListener(textWatcher)
        editEmail.addTextChangedListener(textWatcher)
        editPassword.addTextChangedListener(textWatcher)

        btnDaftar.setOnClickListener {
            val username = editNama.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Nama (username), email, dan password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 8) {
                Toast.makeText(this, "Password minimal 8 karakter.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val registerRequest = RegisterRequest(email, username, password)
            val apiService = RetrofitClient.instance.create(ApiService::class.java)

            apiService.registerUser(registerRequest).enqueue(object : Callback<GeneralApiResponse> {
                override fun onResponse(call: Call<GeneralApiResponse>, response: Response<GeneralApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            Toast.makeText(this@RegisterActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMessage = apiResponse?.message ?: "Registrasi gagal. Coba lagi."
                            Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                            Log.e(TAG, "API Error: $errorMessage, Raw: ${response.errorBody()?.string()}")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        var errorMessage = "Registrasi gagal: ${response.code()}"
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
                            Log.e(TAG, "Error parsing error body: $e, Raw Error: $errorBody")
                        }
                        Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "HTTP Error: ${response.code()}, Body: $errorBody")
                    }
                }

                override fun onFailure(call: Call<GeneralApiResponse>, t: Throwable) {
                    Log.e(TAG, "Network Failure: ${t.message}", t)
                    Toast.makeText(this@RegisterActivity, "Registrasi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }

        textMasuk.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
    }

    private fun areAllFieldsFilled(): Boolean {
        return editNama.text.toString().trim().isNotEmpty() &&
                editEmail.text.toString().trim().isNotEmpty() &&
                editPassword.text.toString().trim().isNotEmpty()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}