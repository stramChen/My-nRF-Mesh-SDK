package qk.sdk.mesh.meshsdk.util

import android.content.Context

object LocalPreferences : AbsPreference() {
    private val CURRENT_NETKEY = "currentNetKey"
    private val SP_MESH = "mesh_sp"

    @JvmStatic
    fun init(context: Context) {
        init(SP_MESH, context)
    }

    fun setCurrentNetKey(key: String) {
        setObject(CURRENT_NETKEY, key)
    }

    fun getCurrentNetKey(): String = getObject(CURRENT_NETKEY, "") as String
}