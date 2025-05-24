package com.example.filkomplain

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileShowActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var namaEditText: EditText
    private lateinit var nimEditText: EditText
    private lateinit var prodiEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText

    private lateinit var btnEditProfil: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_show)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        namaEditText = findViewById(R.id.showNamaProfil)
        nimEditText = findViewById(R.id.showNIMProfil)
        prodiEditText = findViewById(R.id.showProdiProfil)
        emailEditText = findViewById(R.id.showEmailProfil)
        phoneEditText = findViewById(R.id.showPhoneProfil)
        btnEditProfil = findViewById(R.id.btnEditProfil)
        btnLogout = findViewById(R.id.btnLogout)

        loadUserProfile()

        btnEditProfil.setOnClickListener {
            startActivity(Intent(this, ProfileEditActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        btnLogout.setOnClickListener {
            auth.signOut() // logout dari Firebase
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nama = document.getString("nama")
                        val nim = document.getString("nim")
                        val prodi = document.getString("prodi")

                        if (prodi.isNullOrEmpty() && !nim.isNullOrEmpty()) {
                            val kodeProdi = nim.substring(6, 8)

                            val namaProdi = when (kodeProdi) {
                                "20" -> "Teknik Informatika"
                                "30" -> "Teknik Komputer"
                                "40" -> "Sistem Informasi"
                                "60" -> "Pendidikan Teknologi Informasi"
                                "70" -> "Teknologi Informasi"
                                else -> "Program Studi Tidak Dikenal"
                            }
                            prodiEditText.setText(namaProdi)
                        } else {
                            prodiEditText.setText(prodi)
                        }

                        namaEditText.setText(nama ?: "")
                        nimEditText.setText(nim ?: "")
                        emailEditText.setText(currentUser.email ?: "")
                        phoneEditText.setText(document.getString("telepon") ?: "")
                    }
                }
                .addOnFailureListener {
                    // Toast jika perlu
                }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}