package qk.sdk.mesh.meshsdk.util

import android.Manifest
import android.app.Activity
import com.joker.api.Permissions4M
import com.joker.api.wrapper.ListenerWrapper

object PermissionUtil {

    fun checkCommonPermission() {

    }

    val PERMISSION_BLUETOOTH_REQUEST_CODE = 1000
    val PERMISSION_BLUETOOTH_ADMIN_REQUEST_CODE = 1001
    val PERMISSION_ACCESS_FINE_LOCATION_REQUEST_CODE = 1002
    fun checkMeshPermission(
        context: Activity,
        listener: ListenerWrapper.PermissionRequestListener
    ) {
        Permissions4M.get(context).requestPermissions(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).requestListener(listener).requestCodes(
            PERMISSION_BLUETOOTH_REQUEST_CODE, PERMISSION_BLUETOOTH_ADMIN_REQUEST_CODE,
            PERMISSION_ACCESS_FINE_LOCATION_REQUEST_CODE
        ).request()
    }
}