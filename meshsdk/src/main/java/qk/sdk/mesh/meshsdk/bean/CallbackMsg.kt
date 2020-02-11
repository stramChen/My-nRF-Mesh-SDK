package qk.sdk.mesh.meshsdk.bean

data class CallbackMsg(var code: Int, var msg: String)

enum class CommonErrorMsg(var code: Int, var msg: String) {
//    val SDK_NOT_INIT_MSG = "SDK_NOT_INIT_MSG"
//    val SDK_NOT_INIT_CODE = 301
//
//    val PERMISSION_GRANTED = "GRANTED"
//    val PERMISSION_DENIED = "DENIED"
//    val PERMISSION_CLOSED = "CLOSED"
//
//    val SCAN_PROVISIONED = "provisioned"
//    val SCAN_UNPROVISIONED = "unProvisioned"
//
//    val PROVISION_SUCCESS_CODE = 200//provision 成功
//    val PROVISION_WRONG_PARAM_CODE = 302 //provision传参错误，找不到mac地址对应的设备
//    val PROVISION_WRONG_PARAM_CODE = 302 //provision传参错误，找不到mac地址对应的设备


    /*******扫描*********/
    NO_BLUTOOH_PERMISSION(2010300, "请先开启蓝牙权限"),
    NO_LOCATION_PERMISSION(2010301, "请先开启定位权限"),
    NO_STORAGE_PERMISSION(2010302, "请先开启存储权限"),

    /*******连接**********/
    CONNECT_PROVISIONED_NODE_UPDATE(2010400, "匹配节点更新"),
    DISCONNECTED(-200, "连接断开"),


    /*******provision**********/
    PROVISION_UNICAST_UNABLED(2010500, "节点已被provision，请先删除本地存储")

}

val ERROR_MSG_UNICAST_UNABLED = "Unicast address is already in use."