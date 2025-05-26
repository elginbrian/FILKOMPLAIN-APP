package com.example.filkomplain

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "FilkomplainAppPrefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_USERNAME = "username"
    private const val KEY_USER_ID = "user_id"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveAuthToken(context: Context, token: String) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.apply()
    }

    fun getAuthToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_AUTH_TOKEN, null)
    }

    fun saveUsername(context: Context, username: String?) {
        val editor = getSharedPreferences(context).edit()
        if (username != null) {
            editor.putString(KEY_USERNAME, username)
        } else {
            editor.remove(KEY_USERNAME)
        }
        editor.apply()
    }

    fun getUsername(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USERNAME, null)
    }

    fun saveUserId(context: Context, userId: Int?) {
        val editor = getSharedPreferences(context).edit()
        if (userId != null) {
            editor.putInt(KEY_USER_ID, userId)
        } else {
            editor.remove(KEY_USER_ID)
        }
        editor.apply()
    }

    fun getUserId(context: Context): Int? {
        val id = getSharedPreferences(context).getInt(KEY_USER_ID, -1)
        return if (id != -1) id else null
    }

    fun clearSession(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.remove(KEY_AUTH_TOKEN)
        editor.remove(KEY_USERNAME)
        editor.remove(KEY_USER_ID)
        editor.apply()
    }
}