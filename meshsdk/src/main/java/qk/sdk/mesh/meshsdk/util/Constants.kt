package qk.sdk.mesh.meshsdk.util

object Constants {
    /**
     * 错误码说明：int 类型
     * SDK全局错误码：100起，如未初始化SDK的错误码为101,未释放蓝牙资源133
     * PROVISION错误码：200起
     *
     */

    //------未初始化SDK
    val SDK_NOT_INIT_MSG = "SDK_NOT_INIT_MSG"
    val SDK_NOT_INIT_CODE = 101

    //------蓝牙相关权限
    val PERMISSION_GRANTED = "GRANTED"
    val PERMISSION_DENIED = "DENIED"
    val PERMISSION_CLOSED = "CLOSED"

    //------扫描设备
    val SCAN_PROVISIONED = "provisioned"
    val SCAN_UNPROVISIONED = "unProvisioned"


//    val PROVISION_SUCCESS_CODE = 200//provision 成功
//    val PROVISION_WRONG_PARAM_CODE = 322 //provision传参错误，找不到mac地址对应的设备
//    val PROVISION_WRONG_PARAM = "找不到mac地址对应的设备" //provision传参错误，找不到mac地址对应的设备

    enum class ConnectState(var msg: String, var code: Int = 1000) {
        SDK_NOT_INIT("SDK_NOT_INIT_MSG", 101),
        CANNOT_FIND_DEVICE_BY_MAC("找不到mac地址对应的设备", 102),//provision传参错误，找不到mac地址对应的设备
        CONNECT_FAILED("未释放蓝牙资源", 133),

        PROVISION_SUCCESS("provisioned", 200),//provision 成功

        CONNECTING("连接中", 201),
        DISCOVERING_SERVICE("检查服务中", 202),
        INITIALIZING("初始化蓝牙连接", 203),
        DEVICE_READY("连接初始化完成", 204),
        RECONNETCING("正在重连", 205),
        DISCONNECTING("正在断开连接", 206),
        DISCONNECTED("连接已断开", 207),
    }
}