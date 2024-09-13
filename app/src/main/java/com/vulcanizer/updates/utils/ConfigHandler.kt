package com.vulcanizer.updates.utils
import android.content.Context
import android.content.SharedPreferences

public class ConfigHandler (context: Context, preferencesName: String) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun saveString(key: String, value: String) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(key: String, defaultValue: String): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun saveInt(key: String, value: Int) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun saveBoolean(key: String, value: Boolean) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun saveFloat(key: String, value: Float) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putFloat(key, value)
        editor.apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun saveLong(key: String, value: Long) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }
}