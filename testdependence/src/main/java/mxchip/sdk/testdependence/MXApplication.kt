package mxchip.sdk.testdependence

import android.app.Application
import androidx.multidex.MultiDex

class MXApplication : Application() {
    override fun onCreate() {
        super.onCreate()
//        MultiDex.install(this)
    }
}