package com.kyagamy.step.common

import android.content.Context
import android.content.SharedPreferences

class SettingsGameGetter internal constructor(context: Context) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences("stepmix", Context.MODE_PRIVATE)

    fun saveSetting(name: String, value: Any) {
        try {
            val setting = allSettings.firstOrNull { x -> x.Name == name }
            when {
                setting!!.dataType == "Float" -> {
                    preferences.edit().putFloat(name, value as Float).apply()
                }
                setting.dataType == "Boolean" -> {
                    preferences.edit().putBoolean(name, value as Boolean).apply()
                }
                setting.dataType == "int" -> {
                    preferences.edit().putInt(name, value as Int).apply()
                }
                setting.dataType == "String" -> {
                    preferences.edit().putString(name, value as String).apply()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    fun getValueInt(name: String): Int {
        return preferences.getInt(name, 1)
    }

    fun getValueFloat(name: String): Float {
        return preferences.getFloat(name, 1f)
    }

    fun getValueString(name: String): String {
        return preferences.getString(name, "DEFAULT") ?: ""
    }

    fun getValueBool(name: String): Boolean {
        return preferences.getBoolean(name, false)
    }


    private class PreferenceObject(var dataType: String, var Name: String)

    companion object {
        private val allSettings = arrayOf(
            PreferenceObject("Float", "SPEED"),
            PreferenceObject("Boolean", "EW"),
            PreferenceObject("Boolean", "RV"),
            PreferenceObject("Boolean", "AC"),
            PreferenceObject("Boolean", "DC"),
            PreferenceObject("int", "AV"),
            PreferenceObject("Boolean", "V"),
            PreferenceObject("Boolean", "AP"),
            PreferenceObject("Boolean", "NS"),
            PreferenceObject("Boolean", "FD"),
            PreferenceObject("Boolean", "FL"),
            PreferenceObject("Boolean", "RS"),
            PreferenceObject("Boolean", "BGA_OFF"),
            PreferenceObject("Boolean", "BGA_DARK"),
            PreferenceObject("String", "NOTE_SKIN_NAME"),
            PreferenceObject("int", "RUSH")
        )

        //Speed modifiers
        const val SPEED = "SPEED"
        const val EW = "EW" //eartworm
        const val RV = "RV" //random velocity
        const val AC = "AC" //Aceleration
        const val DC = "DC" //Deceleration

        //AV modifier
        const val AV = "AV"

        //Display Modifiers
        const val V = "V" //Vanish
        const val AP = "AP"
        const val NS = "NS" //non step
        const val FD = "FD" //freedom
        const val FL = "FL" //flash
        const val RS = "RS" //random skin
        const val BGA_OFF = "BGA_OFF"
        const val BGA_DARK = "BGA_DARK"

        //Others
        const val NOTE_SKIN_NAME = "NOTE_SKIN_NAME"
        const val RUSH = "RUSH"
        const val AV_UNABLE = -1
    }

}