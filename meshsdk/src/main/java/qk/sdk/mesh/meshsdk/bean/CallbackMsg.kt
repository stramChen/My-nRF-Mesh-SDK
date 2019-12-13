package qk.sdk.mesh.meshsdk.bean

data class ErrorMsg(var code: Int, var msg: String)

enum class CommonErrorMsg(var code: Int, var msg: String) {
    /*******扫描*********/
    NO_BLUTOOH_PERMISSION(2010300, "请先开启蓝牙权限"),
    NO_LOCATION_PERMISSION(2010301, "请先开启定位权限"),
    NO_STORAGE_PERMISSION(2010302, "请先开启存储权限"),

    /*******连接**********/

    /*******provision**********/
    PROVISION_UNICAST_UNABLED(2010303, "正在匹配中，请稍后")
}

val ERROR_MSG_UNICAST_UNABLED = "Unicast address is already in use."