package qk.sdk.mesh.mxnrfmesh

import android.app.Application
import qk.sdk.mesh.meshsdk.MeshHelper

class MeshApplicaiton : Application() {
    override fun onCreate() {
        super.onCreate()

        MeshHelper.initMesh(this)
        initHttp()
    }

    private fun initHttp(){

    }
}