package qk.sdk.mesh.meshsdk.util

import android.os.Environment
import java.io.File

object Constants {
    /**
     * 错误码说明：int 类型
     * SDK全局错误码：100起，如未初始化SDK的错误码为101,未释放蓝牙资源133
     * PROVISION错误码：200起
     * 导入导出错误码：500起
     * dfu错误码：600起
     * group错误码：700起
     */

    //------未初始化SDK
    const val SDK_NOT_INIT_MSG = "SDK_NOT_INIT_MSG"
    const val SDK_NOT_INIT_CODE = 101

    //------蓝牙相关权限
    const val PERMISSION_GRANTED = "GRANTED"
    const val PERMISSION_DENIED = "DENIED"
    const val PERMISSION_CLOSED = "CLOSED"

    //------扫描设备
    const val SCAN_PROVISIONED = "provisioned"
    const val SCAN_UNPROVISIONED = "unProvisioned"

    //------dfu
    const val ERROR_TYPE_PARAM_ERROR = 2
    const val ERROR_TYPE_FILE_ERROR = 3
    const val DFU_WORK_MODE_SILENCE = 16


    //    val PROVISION_SUCCESS_CODE = 200//provision 成功
//    val PROVISION_WRONG_PARAM_CODE = 322 //provision传参错误，找不到mac地址对应的设备
//    val PROVISION_WRONG_PARAM = "找不到mac地址对应的设备" //provision传参错误，找不到mac地址对应的设备
    const val KEY_CODE = "code"
    const val KEY_MESSAGE = "message"

    enum class ConnectState(var msg: String, var code: Int = 1000) {
        SDK_NOT_INIT("SDK_NOT_INIT_MSG", 101),
        CANNOT_FIND_DEVICE_BY_MAC("找不到uuid对应的设备", 102),//传参错误，找不到mac地址对应的设备
        CONNECT_NOT_EXIST("请先建立蓝牙连接", 103),//连接不存在：当要发送消息时，发现没有已建立的连接时，报错
        CONNECT_BLE_RESOURCE_FAILED("Error on connection state change", 133),
        BLE_NOT_AVAILABLE("蓝牙未开启", 104),//未开启蓝牙
        NET_KEY_IS_NULL("networkKey is null", 105),//没有setCurrentNetworkKey

        COMMON_SUCCESS("success", 200),// 全局通用，操作成功

        BIND_APP_KEY_FOR_NODE_FAILED("", 208),//绑定appkey失败
        CONNECTING("连接中", 201),
        DISCOVERING_SERVICE("检查服务中", 202),
        INITIALIZING("初始化蓝牙连接", 203),
        RECONNETCING("正在重连", 205),
        DISCONNECTING("正在断开连接", 206),
        DISCONNECTED("连接已断开", 207),
        PROVISION_SUCCESS("provisioned", 208),//provision 成功
        PROVISION_FAILED("provision failed", -208),//provision 失败

        NET_KEY_DELETE_FAILED("netKey正在使用中，需先删除netKey对应的设备", 401),//netKey正在使用中，需先删除netKey对应的设备
        NET_KEY_NOT_EXIST("netKey不存在", 403),//netKey正在使用中，需先删除netKey对应的设备
        APP_KEY_DELETE_FAILED("appKey正在使用中，需先删除appKey对应的设备", 402),//appKey正在使用中，需先删除appKey对应的设备

        IMPORT_MESH_JSON_EMPTY_ERR("mesh json is null", 501),

        DFU_FILE_NOT_EXIST("更新包不存在", 601),
        DFU_PARAM_ERROR("传参错误", 601),

        GROUP_NOT_EXIST("group群组",701),
        NODE_NOT_EXIST("节点不存在",702)
    }

    /**
     * 本地存储目录
     */
    val BASE_PATH = Environment
        .getExternalStorageDirectory().absolutePath + File.separator
    val FOLDER_MXCHIP = BASE_PATH + "mxchip" + File.separator

    /**
     * 本地存储目录————二级目录
     */
    val FOLDER_CACHE = FOLDER_MXCHIP + "cache" + File.separator
    val FOLDER_DOWNLOAD = FOLDER_MXCHIP + "download" + File.separator

    val MESH_LOG_FILE_NAME = "mesh_log"
}