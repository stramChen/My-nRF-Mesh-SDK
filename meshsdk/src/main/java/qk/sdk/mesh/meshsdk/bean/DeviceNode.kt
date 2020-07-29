package qk.sdk.mesh.meshsdk.bean

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/7/25 12:45 PM
 */

data class DeviceNode<T>(
    var status:String? = null,
    var deviceId:String?  = null,
    var properties :T? = null){
    companion object{
        public const val STATUS_ONLINE   = "Online";
        public const val STATUS_OFFLINE   = "Offline";
    }
}