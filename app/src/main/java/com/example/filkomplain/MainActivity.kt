package com.example.filkomplain

import SpaceItemDecoration
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.material.imageview.ShapeableImageView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.filkomplain.adapter.KomplainAdapter
import com.example.filkomplain.api.ApiService
import com.example.filkomplain.api.ApiReportItem
import com.example.filkomplain.api.FetchProfileResponse
import com.example.filkomplain.api.FetchReportsResponse
import com.example.filkomplain.api.RetrofitClient
import com.example.filkomplain.model.KomplainModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var textNama: TextView
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var btnAddKomplain: Button

    private lateinit var bgMainEmpty: ImageView
    private lateinit var textKosong1: TextView
    private lateinit var textKosong2: TextView
    private lateinit var rvKomplain: RecyclerView

    private lateinit var komplainAdapter: KomplainAdapter
    private var listKomplain = mutableListOf<KomplainModel>()

    private lateinit var textTanya: TextView
    private lateinit var textProdi: TextView

    private val TAG = "MainActivity"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authToken = SessionManager.getAuthToken(this)
        if (authToken == null) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_main)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        initializeUI()
        loadUserProfileFromApi(authToken)
        loadKomplainDataFromBackend(authToken)

        if (::rvKomplain.isInitialized) {
            val spacingInDp = 16
            rvKomplain.addItemDecoration(SpaceItemDecoration(spacingInDp))
        }

        if (::profileImageView.isInitialized) {
            profileImageView.setOnClickListener {
                startActivity(Intent(this, ProfileShowActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        if (::btnAddKomplain.isInitialized) {
            btnAddKomplain.setOnClickListener {
                startActivity(Intent(this, KomplainActivity::class.java))
                overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top)
            }
        }
    }

    private fun initializeUI() {
        textNama = findViewById(R.id.textNama)
        profileImageView = findViewById(R.id.btnShowProfile)
        btnAddKomplain = findViewById(R.id.btnAddKomplain)
        bgMainEmpty = findViewById(R.id.bgMainEmpty)
        textKosong1 = findViewById(R.id.textKosong1)
        textKosong2 = findViewById(R.id.textKosong2)
        rvKomplain = findViewById(R.id.rvKomplain)
        textTanya = findViewById(R.id.textTanya)
        textProdi = findViewById(R.id.textProdi)

        rvKomplain.layoutManager = LinearLayoutManager(this)
        komplainAdapter = KomplainAdapter(listKomplain)
        rvKomplain.adapter = komplainAdapter
    }

    private fun goToLogin() {
        SessionManager.clearSession(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserProfileFromApi(token: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getUserProfile("Bearer $token").enqueue(object : Callback<FetchProfileResponse> {
            override fun onResponse(call: Call<FetchProfileResponse>, response: Response<FetchProfileResponse>) {
                if (response.isSuccessful) {
                    val profileResponse = response.body()
                    if (profileResponse != null && profileResponse.success) {
                        val userData = profileResponse.data
                        val displayUsername = userData?.username ?: "Pengguna"
                        val programStudi = userData?.program_studi ?: ""
                        val photoUrl = userData?.profile_image_url

                        textNama.text = "Halo, $displayUsername!"
                        textTanya.text = "Mau ngabarin apa\nhari ini, $displayUsername? 😉"
                        textProdi.text = if (programStudi.isNotEmpty()) programStudi else ""

                        SessionManager.saveUsername(this@MainActivity, userData?.username)

                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this@MainActivity)
                                .load(photoUrl)
                                .placeholder(R.drawable.profile_photo)
                                .error(R.drawable.profile_photo)
                                .circleCrop()
                                .into(profileImageView)
                        } else {
                            Glide.with(this@MainActivity)
                                .load(R.drawable.profile_photo)
                                .circleCrop()
                                .into(profileImageView)
                        }
                        Log.d(TAG, "User profile loaded from API: $displayUsername, Prodi: $programStudi, Photo: $photoUrl")
                    } else {
                        setDefaultUserDisplayOnError(profileResponse?.message ?: "Gagal memuat data profil.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var errorMessage = "Gagal memuat profil: Error ${response.code()}"
                    try {
                        if(errorBody != null) {
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing error body for profile fetch: $e")
                    }
                    setDefaultUserDisplayOnError(errorMessage)
                    Log.e(TAG, "API HTTP Error (Profile) ${response.code()}: $errorBody")
                }
            }

            override fun onFailure(call: Call<FetchProfileResponse>, t: Throwable) {
                setDefaultUserDisplayOnError("Gagal memuat profil: ${t.message}")
                Log.e(TAG, "Network Failure (Profile): ", t)
            }
        })
    }

    private fun setDefaultUserDisplayOnError(errorMessage: String) {
        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
        val usernameFromSession = SessionManager.getUsername(this) ?: "Pengguna"
        textNama.text = "Halo, $usernameFromSession!"
        textTanya.text = "Mau ngabarin apa\nhari ini, $usernameFromSession? 😉"
        textProdi.text = "-"
        if(::profileImageView.isInitialized) {
            Glide.with(this@MainActivity)
                .load(R.drawable.profile_photo)
                .circleCrop()
                .into(profileImageView)
        }
    }

    private fun loadKomplainDataFromBackend(token: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        Log.d(TAG, "Fetching reports with token: Bearer $token")
        apiService.getReports("Bearer $token").enqueue(object : Callback<FetchReportsResponse> {
            override fun onResponse(call: Call<FetchReportsResponse>, response: Response<FetchReportsResponse>) {
                if (response.isSuccessful) {
                    val fetchResponse = response.body()
                    if (fetchResponse != null && fetchResponse.success) {
                        listKomplain.clear()
                        val apiReports = fetchResponse.data?.reports
                        if (apiReports != null && apiReports.isNotEmpty()) {
                            Log.d(TAG, "Jumlah komplain API: ${apiReports.size}")
                            for (apiReport in apiReports) {
                                val komplainModel = mapApiReportToKomplainModel(apiReport)
                                listKomplain.add(komplainModel)
                            }
                            rvKomplain.visibility = View.VISIBLE
                            bgMainEmpty.visibility = View.GONE
                            textKosong1.visibility = View.GONE
                            textKosong2.visibility = View.GONE
                        } else {
                            Log.d(TAG, "Tidak ada komplain dari API atau list kosong.")
                            showEmptyState()
                        }
                        if(::komplainAdapter.isInitialized) {
                            komplainAdapter.notifyDataSetChanged()
                        }
                    } else {
                        val errorMessage = fetchResponse?.message ?: "Gagal mengambil data laporan (API success false)."
                        Log.e(TAG, "API Logic Error: $errorMessage")
                        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        showEmptyState()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    var errorMessage = "Gagal mengambil data laporan: Error ${response.code()}"
                    try {
                        if(errorBody != null) {
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing error body for reports fetch: $e")
                    }
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "API HTTP Error: ${response.code()} - $errorBody")
                    showEmptyState()
                }
            }
            override fun onFailure(call: Call<FetchReportsResponse>, t: Throwable) {
                Log.e(TAG, "Network Failure: ${t.message}", t)
                Toast.makeText(this@MainActivity, "Gagal mengambil data laporan: ${t.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            }
        })
    }

    private fun showEmptyState() {
        if(::komplainAdapter.isInitialized) komplainAdapter.notifyDataSetChanged()
        if(::rvKomplain.isInitialized) rvKomplain.visibility = View.GONE
        if(::bgMainEmpty.isInitialized) bgMainEmpty.visibility = View.VISIBLE
        if(::textKosong1.isInitialized) textKosong1.visibility = View.VISIBLE
        if(::textKosong2.isInitialized) textKosong2.visibility = View.VISIBLE
    }

    private fun mapApiReportToKomplainModel(apiReport: ApiReportItem): KomplainModel {
        val formattedDate = formatApiDateTimeString(apiReport.created_at)
        val timestamp = parseApiDateTimeStringToTimestamp(apiReport.created_at)

        return KomplainModel(
            judul = apiReport.content ?: "Tidak ada judul",
            deskripsi = apiReport.content ?: "",
            lokasi = apiReport.place ?: "Lokasi tidak diketahui",
            kontak = apiReport.phone_number ?: "-",
            tanggal = formattedDate,
            imageUrl = apiReport.attachment ?: "",
            timestamp = timestamp,
            uid = apiReport.user_name ?: ""
        )
    }

    private fun formatApiDateTimeString(apiDateTime: String?): String {
        if (apiDateTime.isNullOrEmpty()) return "Tanggal tidak diketahui"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date = parser.parse(apiDateTime) ?: return "Format tanggal salah"

            val formatter = SimpleDateFormat("dd MMM yy", Locale.getDefault())
            formatter.format(date)
        } catch (e: ParseException) {
            Log.e(TAG, "Error parsing date: $apiDateTime", e)
            "Format tanggal salah"
        }
    }

    private fun parseApiDateTimeStringToTimestamp(apiDateTime: String?): Long {
        if (apiDateTime.isNullOrEmpty()) return 0L
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date = parser.parse(apiDateTime) ?: return 0L
            date.time
        } catch (e: ParseException) {
            Log.e(TAG, "Error parsing date to timestamp: $apiDateTime", e)
            0L
        }
    }
}