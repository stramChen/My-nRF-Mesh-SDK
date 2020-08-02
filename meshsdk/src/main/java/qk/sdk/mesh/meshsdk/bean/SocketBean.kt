package qk.sdk.mesh.meshsdk.bean

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/8/1 11:30 AM
 */
data class SocketBean(var switch:String? =null,
                      var switchSecond:String? =null,
                      var switchThird:String? =null,
                      var event:String? =null) :
    DeviceBaseProperty(null,null)