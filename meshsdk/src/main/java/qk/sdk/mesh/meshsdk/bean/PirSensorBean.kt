package qk.sdk.mesh.meshsdk.bean

/**
 * @des:
 * @author: chensc@mxchip.com
 * @date: 2020/8/1 11:30 AM
 */
data class PirSensorBean(var bioSenser:String? =null,
                         var remainingElectricity:String? =null,
                         var event:String? =null) :
    DeviceBaseProperty()