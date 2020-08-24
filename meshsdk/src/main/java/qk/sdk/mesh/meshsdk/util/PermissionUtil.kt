package qk.sdk.mesh.meshsdk.util

import android.Manifest
import android.app.Activity
import android.content.Context
import com.joker.api.Permissions4M
import com.joker.api.wrapper.ListenerWrapper
import me.weyye.hipermission.HiPermission
import me.weyye.hipermission.PermissionCallback
import me.weyye.hipermission.PermissionItem
import qk.sdk.mesh.meshsdk.R

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

    fun checkMeshPermission(
        context: Context, callback: PermissionCallback,
        prompt: String = context.getString(R.string.app_name)
    ) {
        var permissions = ArrayList<PermissionItem>()
        permissions.add(PermissionItem(Manifest.permission.BLUETOOTH))
        permissions.add(PermissionItem(Manifest.permission.BLUETOOTH_ADMIN))
        permissions.add(PermissionItem(Manifest.permission.ACCESS_FINE_LOCATION))
        HiPermission.create(context).msg(prompt).permissions(permissions)
            .style(R.style.PermissionDefaultBlueStyle)
            .animStyle(R.style.PermissionAnimScale)
                .title(context.getString(R.string.grant_permission_ble_title))
                .msg(context.getString(R.string.grant_permission_ble_content))
            .checkMutiPermission(callback)

    }
}