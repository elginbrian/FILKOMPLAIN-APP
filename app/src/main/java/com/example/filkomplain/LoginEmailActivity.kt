package com.example.filkomplain

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.filkomplain.api.ApiService
import com.example.filkomplain.api.LoginRequest
import com.example.filkomplain.api.LoginResponse
import com.example.filkomplain.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginEmailActivity : AppCompatActivity() {

    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnMasuk: Button

    private val TAG = "LoginEmailActivity"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_email)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

         ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
             val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
             v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
             insets
         }

        editEmail = findViewById(R.id.editEmail)
        editPassword = findViewById(R.id.editPassword)
        btnMasuk = findViewById(R.id.btnMasuk)

        btnMasuk.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnMasuk.isEnabled = areAllFieldsFilled()
            }
        }

        editEmail.addTextChangedListener(textWatcher)
        editPassword.addTextChangedListener(textWatcher)

        btnMasuk.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginRequest = LoginRequest(email, password)
            val apiService = RetrofitClient.instance.create(ApiService::class.java)

            apiService.loginUser(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null && loginResponse.success) {
                            val token = loginResponse.data?.token
                            if (token != null) {
                                SessionManager.saveAuthToken(this@LoginEmailActivity, token)
                                val usernameFromToken = JwtUtils.getUsernameFromToken(token)
                                val userIdFromToken = JwtUtils.getUserIdFromToken(token)
                                SessionManager.saveUsername(this@LoginEmailActivity, usernameFromToken)
                                SessionManager.saveUserId(this@LoginEmailActivity, userIdFromToken)

                                Log.d(TAG, "Logged in. Token: $token, Username: $usernameFromToken, UserID: $userIdFromToken")

                                Toast.makeText(this@LoginEmailActivity, "Login berhasil!", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this@LoginEmailActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            val errorMessage = loginResponse?.message ?: "Email atau password salah."
                            Toast.makeText(this@LoginEmailActivity, errorMessage, Toast.LENGTH_LONG).show()
                            Log.e(TAG, "API Logic Error: $errorMessage, Raw Error Body: ${response.errorBody()?.string()}")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        var errorMessage = "Login gagal: Error ${response.code()}"
                        try {
                            if (errorBody != null) {
                                val errorResponse = RetrofitClient.instance.responseBodyConverter<LoginResponse>(
                                    LoginResponse::class.java,
                                    LoginResponse::class.java.annotations
                                ).convert(response.errorBody()!!)

                                if (errorResponse != null && errorResponse.message?.isNotBlank() == true) {
                                    errorMessage = errorResponse.message!!
                                } else if (errorBody.isNotBlank()){
                                    errorMessage = "$errorMessage - $errorBody"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing error body: $e, Raw Error: $errorBody")
                        }
                        Toast.makeText(this@LoginEmailActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "HTTP Error: ${response.code()}, Body: $errorBody")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e(TAG, "Network Failure: ${t.message}", t)
                    Toast.makeText(this@LoginEmailActivity, "Login gagal: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun areAllFieldsFilled(): Boolean {
        return editEmail.text.toString().trim().isNotEmpty() &&
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