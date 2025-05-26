package com.example.filkomplain

import android.util.Base64
import android.util.Log
import org.json.JSONObject

object JwtUtils {
    fun getUsernameFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
                val jsonObject = JSONObject(payload)
                jsonObject.optString("username", null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("JwtUtils", "Error parsing username from token", e)
            null
        }
    }

    fun getUserIdFromToken(token: String): Int? {
        return try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
                val jsonObject = JSONObject(payload)
                if (jsonObject.has("user_id")) {
                    jsonObject.optInt("user_id", -1).takeIf { it != -1 }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("JwtUtils", "Error parsing user_id from token", e)
            null
        }
    }
}