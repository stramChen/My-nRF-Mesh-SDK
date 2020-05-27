package no.nordicsemi.android.meshprovisioner.utils

import android.util.Log

object CommonUtil {
    fun printLog(tag: String, msg: String) {
//        if (BuildConfig.DEBUG) {
        Log.e(tag, msg)
//        }
    }
}