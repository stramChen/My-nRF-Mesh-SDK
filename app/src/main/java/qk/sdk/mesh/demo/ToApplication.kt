package qk.sdk.mesh.demo

import android.app.Application
import qk.sdk.mesh.meshsdk.MeshSDK

class ToApplication : Application() {

    override fun onCreate() {
        super.onCreate()
//        MeshHelper.initMesh(applicationContext)
        MeshSDK.init(this)
//        DfuHelper.getInstance(context)
    }
}