package com.example.filkomplain.model

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class KomplainModel(
    val judul: String = "",
    val deskripsi: String = "",
    val lokasi: String = "",
    val kontak: String = "",
    val tanggal: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val uid: String = ""
)

fun getKomplainData(callback: (KomplainModel?) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    val komplainRef = db.collection("komplain").document("idKomplain")

    komplainRef.get()
        .addOnSuccessListener { document ->
            val komplainData = if (document != null && document.exists()) {
                document.toObject(KomplainModel::class.java)
            } else {
                null
            }
            callback(komplainData)
        }
        .addOnFailureListener { exception ->
            Log.w("KomplainActivity", "Error getting document", exception)
            callback(null)
        }
}
