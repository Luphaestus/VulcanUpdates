package com.vulcanizer.updates.utils

import android.content.Context
import android.util.Log
import com.vulcanizer.updates.utils.ConfigHandler

object CheckAppStartUseCase {
    fun get(context: Context): AppStart {
        val appinfo = ConfigHandler(context, "appinfo")

        if (appinfo.getBoolean("tos", true))
        {
            return AppStart.TOS
        }
        else if (appinfo.getBoolean("first_time_version", false))
        {
            return AppStart.FIRST_TIME_VERSION
        }

        return AppStart.NORMAL
    }

    fun setTOS(context: Context)
    {
        val appinfo = ConfigHandler(context, "appinfo")
        appinfo.saveBoolean("tos", false)
    }

    fun setFirstTimeVersion(context: Context, state : Boolean)
    {
        val appinfo = ConfigHandler(context, "appinfo")
        appinfo.saveBoolean("first_time", state)
    }
}


enum class AppStart {
    TOS, FIRST_TIME_VERSION, NORMAL
}
