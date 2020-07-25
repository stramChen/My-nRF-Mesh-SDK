package qk.sdk.mesh.meshsdk.bean

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/7/25 12:45 PM
 */
const val STATUS_ONLINE   = "Online";
const val STATUS_OFFLINE   = "Offline";
data class DeviceNode<T>(
    var status:String? = null,
    var deviceId:String?  = null,
    var properties :T? = null)