package qk.sdk.mesh.demo

import android.app.Application
import qk.sdk.mesh.meshsdk.MeshHelper

class ToApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MeshHelper.initMesh(applicationContext)
    }
}