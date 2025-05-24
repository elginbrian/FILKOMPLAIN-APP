package com.example.filkomplain

import SpaceItemDecoration
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filkomplain.adapter.KomplainAdapter
import com.example.filkomplain.model.KomplainModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var textNama: TextView
    private lateinit var btnShowProfile: ImageButton
    private lateinit var btnAddKomplain: Button

    private lateinit var bgMainEmpty: ImageView
    private lateinit var textKosong1: TextView
    private lateinit var textKosong2: TextView
    private lateinit var rvKomplain: RecyclerView

    private lateinit var komplainAdapter: KomplainAdapter
    private var listKomplain = mutableListOf<KomplainModel>()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        textNama = findViewById(R.id.textNama)
        btnShowProfile = findViewById(R.id.btnShowProfile)
        btnAddKomplain = findViewById(R.id.btnAddKomplain)

        bgMainEmpty = findViewById(R.id.bgMainEmpty)
        textKosong1 = findViewById(R.id.textKosong1)
        textKosong2 = findViewById(R.id.textKosong2)
        rvKomplain = findViewById(R.id.rvKomplain)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val nama = document.getString("nama")
                    val namaDepan = nama?.split(" ")?.firstOrNull() ?: "User"
                    textNama.text = "Halo, $namaDepan!"

                    val textTanya = findViewById<TextView>(R.id.textTanya)
                    textTanya.text = "Mau ngabarin apa\nhari ini, $namaDepan? 😉"

                    val nim = document.getString("nim")

                    val textProdi = findViewById<TextView>(R.id.textProdi)

                    if (!nim.isNullOrEmpty()) {
                        val kodeProdi = nim.substring(6, 8)
                        val namaProdi = when (kodeProdi) {
                            "20" -> "Teknik Informatika"
                            "30" -> "Teknik Komputer"
                            "40" -> "Sistem Informasi"
                            "60" -> "Pendidikan Teknologi Informasi"
                            "70" -> "Teknologi Informasi"
                            else -> null
                        }
                        textProdi.text = namaProdi ?: ""
                    } else {
                        textProdi.text = ""
                    }
                }
                .addOnFailureListener {
                    textNama.text = "Gagal memuat data user"
                }

            // Setup RecyclerView
            rvKomplain.layoutManager = LinearLayoutManager(this)
            komplainAdapter = KomplainAdapter(listKomplain)
            rvKomplain.adapter = komplainAdapter

            // Load data
            loadKomplainData(uid)

        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val spacingInDp = 16
        rvKomplain.addItemDecoration(SpaceItemDecoration(spacingInDp))


        btnShowProfile.setOnClickListener {
            startActivity(Intent(this, ProfileShowActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        btnAddKomplain.setOnClickListener {
            startActivity(Intent(this, KomplainActivity::class.java))
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
            finish()
        }
    }

    private fun loadKomplainData(uid: String) {
        db.collection("komplain")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                listKomplain.clear()
                Log.d("MainActivity", "Jumlah komplain ditemukan: ${result.size()}")
                for (document in result) {
                    val komplain = document.toObject(KomplainModel::class.java)
                    listKomplain.add(komplain)
                }
                komplainAdapter.notifyDataSetChanged()

                if (listKomplain.isEmpty()) {
                    rvKomplain.visibility = View.GONE
                    bgMainEmpty.visibility = View.VISIBLE
                    textKosong1.visibility = View.VISIBLE
                    textKosong2.visibility = View.VISIBLE
                } else {
                    rvKomplain.visibility = View.VISIBLE
                    bgMainEmpty.visibility = View.GONE
                    textKosong1.visibility = View.GONE
                    textKosong2.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Gagal mengambil data komplain", exception)
                Toast.makeText(this, "Gagal memuat data komplain", Toast.LENGTH_SHORT).show()
            }
    }

}