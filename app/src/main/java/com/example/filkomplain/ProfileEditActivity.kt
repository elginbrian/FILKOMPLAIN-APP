package com.example.filkomplain

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var editFotoProfil: ImageView
    private lateinit var btnEditPfp: Button

    private lateinit var editNamaProfil: EditText
    private lateinit var editNIMProfil: EditText
    private lateinit var editProdiProfil: EditText
    private lateinit var editEmailProfil: EditText
    private lateinit var editPhoneProfil: EditText

    private lateinit var btnSelesaiEditProfil: Button

    private val PICK_IMAGE_REQUEST = 100
    private var selectedImageUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // private var uploadedImageUrl: String? = null

    private var isFromGoogleLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        editFotoProfil = findViewById(R.id.editFotoProfil)
        btnEditPfp = findViewById(R.id.btnEditPfp)

        editNamaProfil = findViewById(R.id.editNamaProfil)
        editNIMProfil = findViewById(R.id.editNIMProfil)
        editProdiProfil = findViewById(R.id.editProdiProfil)
        editEmailProfil = findViewById(R.id.editEmailProfil)
        editPhoneProfil = findViewById(R.id.editPhoneProfil)

        btnSelesaiEditProfil = findViewById(R.id.btnSelesaiEditProfil)

        // Tangkap intent extra untuk cek apakah dari login Google
        isFromGoogleLogin = intent.getBooleanExtra("fromGoogleLogin", false)

        loadUserProfile()

        btnSelesaiEditProfil.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnSelesaiEditProfil.isEnabled = areAllFieldsFilled()
            }
        }

        editNamaProfil.addTextChangedListener(textWatcher)
        editNIMProfil.addTextChangedListener(textWatcher)
        editPhoneProfil.addTextChangedListener(textWatcher)

        // Disable Profile Photo Button
        btnEditPfp.isEnabled = false

        btnSelesaiEditProfil.setOnClickListener {
            updateProfile {
                if (isFromGoogleLogin) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                } else {
                    val intent = Intent(this, ProfileShowActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
        }
    }

    private fun areAllFieldsFilled(): Boolean {
        return editNamaProfil.text.toString().trim().isNotEmpty() &&
                editNIMProfil.text.toString().trim().isNotEmpty() &&
                editPhoneProfil.text.toString().trim().isNotEmpty()
    }

    private var oldNama: String? = null
    private var oldNIM: String? = null
    private var oldTelepon: String? = null
    // private var oldPhotoUrl: String? = null

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        oldNama = document.getString("nama")
                        oldNIM = document.getString("nim")
                        oldTelepon = document.getString("telepon")
                        // oldPhotoUrl = document.getString("photoUrl")

                        editNamaProfil.setText(oldNama)
                        editPhoneProfil.setText(oldTelepon)

                        val nim = document.getString("nim")
                        val prodi = document.getString("prodi")

                        editNIMProfil.setText(nim)
                        editEmailProfil.setText(currentUser.email)

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
                            editProdiProfil.setText(namaProdi)
                        }

                        val photoUrl = document.getString("photoUrl")
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.profile_photo)
                                .into(editFotoProfil)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil data profil", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Pilih Foto Profil"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            editFotoProfil.setImageURI(selectedImageUri)
        }
    }

    private fun updateProfile(onComplete: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak ditemukan, silakan login kembali", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val nama = editNamaProfil.text.toString().trim()
        val nim = editNIMProfil.text.toString().trim()
        val telepon = editPhoneProfil.text.toString().trim()

        saveUserData(userId, nama, nim, telepon, onComplete)
    }

    private fun saveUserData(
        userId: String,
        newNama: String,
        newNIM: String,
        newTelepon: String,
        onComplete: () -> Unit
    ) {
        val updates = mutableMapOf<String, Any>()

        if (newNama != oldNama) updates["nama"] = newNama
        if (newNIM != oldNIM) updates["nim"] = newNIM
        if (newTelepon != oldTelepon) updates["telepon"] = newTelepon
        // if (newPhotoUrl != null && newPhotoUrl != oldPhotoUrl) updates["photoUrl"] = newPhotoUrl

        val updateFirestore = {
            firestore.collection("users").document(userId)
                .set(updates, SetOptions.merge()) // set() + merge
                .addOnSuccessListener {
                    Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    oldNama = newNama
                    oldNIM = newNIM
                    oldTelepon = newTelepon
                    onComplete()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menyimpan profil", Toast.LENGTH_SHORT).show()
                }
        }

        if (updates.isNotEmpty()) {
            updateFirestore()
        } else {
            Toast.makeText(this, "Tidak ada perubahan yang disimpan", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        val intent = Intent(this, ProfileShowActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }
}
