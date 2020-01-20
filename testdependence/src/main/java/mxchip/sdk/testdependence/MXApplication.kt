package mxchip.sdk.testdependence

import android.app.Application

class MXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//        MultiDex.install(this)
    }
}