package qk.sdk.mesh.meshsdk.callback

/**
 * @des:Mesh组播下行线监听回调
 * @author: chensc@mxchip.com
 * @date: 2020/7/24 4:59 PM
 */
open interface IDownstreamListener {
    fun onCommand(message: String?)
//    fun shouldHandle(var1: String?): Boolean4
}